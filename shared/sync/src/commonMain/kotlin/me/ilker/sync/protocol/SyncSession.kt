package me.ilker.sync.protocol

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.ilker.balance_tracker.sync.SyncSnapshot
import me.ilker.sync.transport.SyncTransport
import me.ilker.sync.transport.SyncTransportException

/**
 * The messages a session can carry, in the order they are exchanged.
 *
 * LocalSend separates announcing, discovering, preparing and transferring; the same shape is needed
 * here, because the receiver has to be able to refuse before any ledger data crosses the link. The
 * stages are therefore explicit rather than folded into one symmetric swap:
 *
 * 1. the initiator writes [Hello] first, so the receiver can tell a sync from a pairing before it
 *    answers; the receiver then writes its own [Hello]
 * 2. the initiator writes [Request], carrying the metadata the receiver needs to decide
 * 3. the receiver writes [Accept] or [Reject]
 * 4. both sides write their sealed snapshot and read the peer's
 * 5. both sides write [Done] once they have read the peer's snapshot
 *
 * A reject ends the session at step 3, before a single transaction has moved. The final [Done] keeps
 * the two devices from closing the link while the other is still consuming the last chunks.
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

    /**
     * Sent after the sender has read the peer's snapshot, to confirm the whole exchange completed.
     *
     * Notifications are queued rather than acknowledged at the application level, so without this the
     * sender can close the connection while its final chunks are still in flight. The peer then waits
     * for data that will never arrive, which showed up as one device finishing while the other stayed
     * on the progress screen. Receiving [Done] proves the peer read everything this side sent.
     */
    @Serializable
    data class Done(
        override val protocolVersion: Int = SyncPayloadCodec.Version
    ) : SyncMessage

    /**
     * Sent instead of a hello when the receiver no longer holds a pairing secret.
     *
     * Without it the receiver can only close the connection, which the initiator sees as a dropped
     * link. That distinction matters: a dropped link is retryable, while this means the stored link
     * is gone and should be cleared rather than shown as paired forever.
     */
    @Serializable
    data class Unpaired(
        override val protocolVersion: Int = SyncPayloadCodec.Version,
        val device: DeviceDescriptor
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
 * @param peerHello the initiator's already-read hello, when this side received the connection and had
 *   to inspect the first message to know it was a sync at all.
 * @return [SyncSessionResult.Completed] carrying the peer's snapshot, or
 *   [SyncSessionResult.Declined] if the peer rejected or turned out to be this device.
 */
internal suspend fun runSyncSession(
    transport: SyncTransport,
    local: DeviceDescriptor,
    secret: ByteArray,
    snapshot: SyncSnapshot,
    request: SyncMessage.Request?,
    onRequest: suspend (IncomingSyncRequest) -> Boolean,
    peerHello: SyncMessage.Hello? = null
): SyncSessionResult {
    transport.open()
    try {
        val link = FramedLink(transport)

        link.send(SyncMessage.Hello(device = local, proof = proof(secret, local.deviceId)))

        val peer = peerHello ?: when (val answer = link.read()) {
            is SyncMessage.Hello -> answer
            // A peer that removed its secret says so explicitly, so the initiator can drop a link that
            // can never authenticate again instead of treating this as a transient transport failure.
            is SyncMessage.Unpaired -> throw SyncProtocolException.PeerUnpaired
            else -> throw SyncProtocolException.Unauthenticated
        }
        if (peer.device.deviceId == local.deviceId) return SyncSessionResult.Declined
        if (!verify(secret, peer)) throw SyncProtocolException.Unauthenticated
        // Taken from the hello rather than the request: the hello is what the peer proved, and both
        // sides describe themselves before either asks for anything.
        val peerDevice = peer.device

        if (request != null) {
            link.send(request)
            // The answer waits on a person on the other device, so it gets the approval budget rather
            // than the short deadline that applies to chunks arriving during a transfer.
            return when (link.read(timeoutMillis = ApprovalTimeoutMillis)) {
                is SyncMessage.Accept -> {
                    link.sendRaw(SyncPayloadCodec.seal(secret, snapshot))
                    val peerSnapshot = link.openSnapshot(secret)
                    link.completeExchange()
                    SyncSessionResult.Completed(peerDevice, peerSnapshot)
                }

                is SyncMessage.Reject -> SyncSessionResult.Declined
                else -> throw SyncProtocolException.Unauthenticated
            }
        }

        val peerRequest = link.read() as? SyncMessage.Request ?: throw SyncProtocolException.Unauthenticated
        // A request nobody answers must not hold the link (or the initiator) open forever. Timing out
        // is the same as declining: nothing has crossed the link at this point.
        val accepted = withTimeoutOrNull(ApprovalTimeoutMillis) {
            onRequest(
                IncomingSyncRequest(
                    device = peerRequest.device,
                    transactionCount = peerRequest.transactionCount
                )
            )
        } ?: false
        link.send(if (accepted) SyncMessage.Accept() else SyncMessage.Reject())
        if (!accepted) return SyncSessionResult.Declined

        link.sendRaw(SyncPayloadCodec.seal(secret, snapshot))
        val peerSnapshot = link.openSnapshot(secret)
        link.completeExchange()
        return SyncSessionResult.Completed(peerDevice, peerSnapshot)
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
 *
 * Internal rather than private because pairing speaks the same framing with a different message type.
 */
internal class FramedLink(private val transport: SyncTransport) {
    private val assembler = MessageAssembler()

    suspend fun send(message: SyncMessage) = sendRaw(SyncMessageCodec.write(message))

    suspend fun sendRaw(text: String) {
        text.encodeToByteArray()
            .toChunks(maxFrameSize = transport.maxFrameSize)
            .forEach { transport.write(it) }
    }

    suspend fun read(timeoutMillis: Long = ChunkTimeoutMillis): SyncMessage =
        SyncMessageCodec.read(readRaw(timeoutMillis))

    /**
     * Reads one complete message, failing when the peer goes quiet for longer than [timeoutMillis].
     *
     * The timeout applies per chunk rather than to the whole message: a large snapshot is many chunks
     * and a fixed deadline on the message would reject a transfer that is still making progress. A
     * silent gap, on the other hand, means the peer dropped the link, which used to leave the screen
     * on its progress state forever.
     */
    suspend fun readRaw(timeoutMillis: Long = ChunkTimeoutMillis): String {
        while (true) {
            val chunk = withTimeoutOrNull(timeoutMillis) { transport.read() }
                ?: throw SyncTransportException.Disconnected("the peer stopped sending data")
            assembler.accept(chunk)?.let { return it.decodeToString() }
        }
    }

    /**
     * Tells the peer this side has read its snapshot, and waits for the peer to say the same.
     *
     * Best effort by design: both snapshots have already been exchanged when this runs, so a peer
     * that closes without answering (an older build) must not turn a completed sync into a failure.
     * A peer that does answer proves it consumed everything, which is what keeps a fast sender from
     * tearing the link down while its last chunks are still in flight.
     */
    suspend fun completeExchange() {
        try {
            send(SyncMessage.Done())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            return
        }

        withTimeoutOrNull(FinalAcknowledgementTimeoutMillis) {
            try {
                read()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // The peer closed after a successful swap; the data is already safely received.
            }
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
 * Reads the first complete message on a fresh connection without assuming its type.
 *
 * The receiving side uses this to tell a pairing hello from a sync hello. A new assembler is safe
 * here because not a single chunk of the first message has been consumed yet.
 */
internal suspend fun SyncTransport.readMessageText(): String {
    val assembler = MessageAssembler()
    while (true) {
        // Same guard as a session read: a peer that connects and then goes silent must release the
        // receive loop instead of leaving it parked for the life of the link.
        val chunk = withTimeoutOrNull(ChunkTimeoutMillis) { read() }
            ?: throw SyncTransportException.Disconnected("the peer stopped sending data")
        assembler.accept(chunk)?.let { return it.decodeToString() }
    }
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

/**
 * How long one chunk may take to arrive before the peer is treated as gone.
 *
 * A chunk is queued immediately, so this is not a transfer deadline: it only catches a link that has
 * died without reporting a disconnect. Without it a dropped notification leaves both devices on the
 * progress screen indefinitely.
 */
internal const val ChunkTimeoutMillis: Long = 20_000L

/** How long the receiving user has to answer a sync request before it is declined. */
internal const val ApprovalTimeoutMillis: Long = 60_000L

/** How long to wait for the peer's final acknowledgement after the snapshots have been exchanged. */
internal const val FinalAcknowledgementTimeoutMillis: Long = 5_000L
