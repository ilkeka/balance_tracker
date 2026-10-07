package me.ilker.sync.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.ilker.balance_tracker.sync.SyncSnapshot
import me.ilker.sync.transport.SyncTransport

/**
 * The messages a session can carry, in the order they are exchanged.
 *
 * LocalSend separates announcing, discovering, preparing and transferring; the same shape is needed
 * here, because the receiver has to be able to refuse before any ledger data crosses the link. The
 * stages are therefore explicit rather than folded into one symmetric swap:
 *
 * 1. both sides write [Hello] immediately, so neither has to win a race to speak first
 * 2. the initiator writes [Request], carrying the metadata the receiver needs to decide
 * 3. the receiver writes [Accept] or [Reject]
 * 4. both sides write their sealed snapshot
 *
 * A reject ends the session at step 3, before a single transaction has moved.
 */
@Serializable
internal sealed interface SyncMessage {
    val protocolVersion: Int

    /** Identity and proof of holding the shared secret, sent by both sides unprompted. */
    @Serializable
    data class Hello(
        override val protocolVersion: Int = SyncPayloadCodec.Version,
        val device: DeviceDescriptor,
        /** HMAC over the sender's device id, proving this side holds the pairing secret. */
        val proof: String
    ) : SyncMessage

    /**
     * Sent by whoever pressed sync. The receiver shows this to the user, so it carries the peer's
     * advertised name rather than anything read out of the database.
     */
    @Serializable
    data class Request(
        override val protocolVersion: Int = SyncPayloadCodec.Version,
        val device: DeviceDescriptor,
        val transactionCount: Int
    ) : SyncMessage

    @Serializable
    data class Accept(
        override val protocolVersion: Int = SyncPayloadCodec.Version
    ) : SyncMessage

    /** Carries no reason: the receiver declined, which is all the initiator needs to know. */
    @Serializable
    data class Reject(
        override val protocolVersion: Int = SyncPayloadCodec.Version
    ) : SyncMessage
}

internal object SyncMessageCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "kind"
    }

    fun write(message: SyncMessage): String = json.encodeToString(SyncMessage.serializer(), message)

    /**
     * A body that will not parse is treated as unauthenticated rather than as a version mismatch:
     * it means the peer is not this app, which the caller reports the same way it reports a device
     * that never paired.
     */
    fun read(payload: String): SyncMessage {
        val message = runCatching { json.decodeFromString(SyncMessage.serializer(), payload) }
            .getOrElse { throw SyncProtocolException.Unauthenticated }
        if (message.protocolVersion != SyncPayloadCodec.Version) throw SyncProtocolException.IncompatibleVersion
        return message
    }
}

/**
 * A request waiting for the user, held by the manager while they decide.
 *
 * The link stays open while this is on screen, so the session has a deadline: if the user never
 * answers, the transport is torn down rather than left connected.
 */
internal data class IncomingSyncRequest(
    val device: DeviceDescriptor,
    val transactionCount: Int
) {
    /**
     * The public half of the pairing secret, which both users compare before accepting.
     *
     * Two devices that paired out of band show the same six characters, which is what makes the
     * approval meaningful rather than a blind accept.
     */
    val verifyCode: String get() = device.fingerprint
}

internal sealed interface SyncSessionResult {
    /**
     * The snapshot swap finished.
     *
     * [peer] is the identity the other device proved by holding the secret, so it is the only trustworthy
     * source for what to remember: the advertisement carries a truncated id precisely so it cannot be
     * used for this.
     *
     * [peerSnapshot] is what the other device sent, already authenticated, or null when it sent nothing
     * usable: the receiver treats an unreadable payload the same way it treats a tampered one, but the
     * sync itself did complete.
     */
    data class Completed(val peer: DeviceDescriptor, val peerSnapshot: SyncSnapshot?) : SyncSessionResult

    /** The peer declined, or the two turned out to be the same device. */
    data object Declined : SyncSessionResult
}

/**
 * Runs a full session over [transport]: hello exchange, the receiver's accept or reject, then the
 * snapshot swap in both directions.
 *
 * @param request set on the initiating side, null on the receiving side.
 * @param onRequest called on the receiving side with the initiator's metadata. Suspending, because
 *   the user has to see the request and answer it before the session can continue.
 * @return [SyncSessionResult.Completed] carrying the peer's snapshot, or
 *   [SyncSessionResult.Declined] if the peer rejected or turned out to be this device.
 */
internal suspend fun runSyncSession(
    transport: SyncTransport,
    local: DeviceDescriptor,
    secret: ByteArray,
    snapshot: SyncSnapshot,
    request: SyncMessage.Request?,
    onRequest: suspend (IncomingSyncRequest) -> Boolean
): SyncSessionResult {
    transport.open()
    try {
        val link = FramedLink(transport)

        link.send(SyncMessage.Hello(device = local, proof = proof(secret, local.deviceId)))

        val peerHello = link.read() as? SyncMessage.Hello ?: throw SyncProtocolException.Unauthenticated
        if (peerHello.device.deviceId == local.deviceId) return SyncSessionResult.Declined
        if (!verify(secret, peerHello)) throw SyncProtocolException.Unauthenticated
        // Taken from the hello rather than the request: the hello is what the peer proved, and both
        // sides describe themselves before either asks for anything.
        val peer = peerHello.device

        if (request != null) {
            link.send(request)
            return when (link.read()) {
                is SyncMessage.Accept -> {
                    link.sendRaw(SyncPayloadCodec.seal(secret, snapshot))
                    SyncSessionResult.Completed(peer, link.openSnapshot(secret))
                }

                is SyncMessage.Reject -> SyncSessionResult.Declined
                else -> throw SyncProtocolException.Unauthenticated
            }
        }

        val peerRequest = link.read() as? SyncMessage.Request ?: throw SyncProtocolException.Unauthenticated
        val accepted = onRequest(
            IncomingSyncRequest(device = peerRequest.device, transactionCount = peerRequest.transactionCount)
        )
        link.send(if (accepted) SyncMessage.Accept() else SyncMessage.Reject())
        if (!accepted) return SyncSessionResult.Declined

        link.sendRaw(SyncPayloadCodec.seal(secret, snapshot))
        return SyncSessionResult.Completed(peer, link.openSnapshot(secret))
    } finally {
        transport.close()
    }
}

/**
 * Message-oriented view of a [SyncTransport].
 *
 * The transport carries chunks of one message at a time and the assembler is stateful, so wrapping
 * both together keeps the sequencing in one place. One assembler serves the whole session because it
 * resets after each complete message.
 */
private class FramedLink(private val transport: SyncTransport) {
    private val assembler = MessageAssembler()

    suspend fun send(message: SyncMessage) = sendRaw(SyncMessageCodec.write(message))

    suspend fun sendRaw(text: String) {
        text.encodeToByteArray()
            .toChunks(maxChunkSize = transport.maxChunkSize)
            .forEach { transport.write(it) }
    }

    suspend fun read(): SyncMessage = SyncMessageCodec.read(readRaw())

    suspend fun readRaw(): String {
        while (true) {
            assembler.accept(transport.read())?.let { return it.decodeToString() }
        }
    }

    /**
     * Reads the peer's snapshot and opens it.
     *
     * A payload that does not authenticate is reported as null rather than thrown: the signed exchange
     * itself already succeeded, so the difference between "the peer sent nothing usable" and "the peer
     * sent something I cannot read" does not change what the user is told.
     */
    suspend fun openSnapshot(secret: ByteArray): SyncSnapshot? =
        runCatching { SyncPayloadCodec.open(secret, readRaw()) }.getOrNull()
}

/**
 * Proof that the sender holds the pairing secret, without revealing it.
 *
 * Binds the device id so a captured hello cannot be replayed as a different device, and is compared
 * in constant time like every other tag in the protocol.
 */
internal suspend fun proof(secret: ByteArray, deviceId: String): String =
    hmacSha256(key = secret, message = "hello:$deviceId".encodeToByteArray()).toHex()

internal suspend fun verify(secret: ByteArray, hello: SyncMessage.Hello): Boolean = constantTimeEquals(
    proof(secret, hello.device.deviceId).encodeToByteArray(),
    hello.proof.encodeToByteArray()
)
