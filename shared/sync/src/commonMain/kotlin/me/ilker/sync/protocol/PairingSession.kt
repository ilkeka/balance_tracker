package me.ilker.sync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.ilker.sync.transport.SyncTransport

/**
 * The handshake two devices run before they hold a shared secret.
 *
 * Pairing used to travel out of band: one device minted a secret and showed it as a QR code, which
 * the other scanned. That needed a camera and a code the user could misread. Here the secret is
 * created during the Bluetooth session itself, and a six-character code shown on both screens is
 * what makes the pairing a deliberate act rather than whichever stranger answered first.
 *
 * The code is derived from both fresh nonces and both device ids, so it changes on every attempt and
 * differs between any two pairs of devices. Both users compare it and accept on their own device.
 *
 * Threat model: without an asymmetric key exchange the code cannot bind to key material, so this is
 * trust-on-first-use with a confirmation step, not a defence against an active relay or an
 * eavesdropper that can read the radio link. What it does guarantee is that a secret only exists
 * after both people looked at the same short code, so a device that merely advertised nearby cannot
 * silently take it over.
 */
@Serializable
internal sealed interface PairingMessage {
    val protocolVersion: Int

    /** Sent by both sides: who is pairing, plus a nonce that keeps the session code fresh. */
    @Serializable
    @SerialName("pair-hello")
    data class Hello(
        override val protocolVersion: Int = SyncPayloadCodec.Version,
        val device: DeviceDescriptor,
        val nonce: String
    ) : PairingMessage

    @Serializable
    @SerialName("pair-accept")
    data class Accept(
        override val protocolVersion: Int = SyncPayloadCodec.Version
    ) : PairingMessage

    /** Carries no reason: one of the two users said no, which is all the other side needs to know. */
    @Serializable
    @SerialName("pair-reject")
    data class Reject(
        override val protocolVersion: Int = SyncPayloadCodec.Version
    ) : PairingMessage

    /** The new secret, sent by the device that opened the connection once both sides accepted. */
    @Serializable
    @SerialName("pair-secret")
    data class Secret(
        override val protocolVersion: Int = SyncPayloadCodec.Version,
        val value: String,
        /** Proves the sender holds the secret it is handing over. */
        val proof: String
    ) : PairingMessage

    /** Sent last by the receiver, after it stored the secret, so the initiator can safely move on. */
    @Serializable
    @SerialName("pair-done")
    data class Paired(
        override val protocolVersion: Int = SyncPayloadCodec.Version,
        val proof: String
    ) : PairingMessage
}

internal object PairingMessageCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "kind"
    }

    fun write(message: PairingMessage): String =
        json.encodeToString(PairingMessage.serializer(), message)

    /**
     * A body that does not parse is reported as unauthenticated, matching the sync codec: it means
     * the peer is not this app, which is exactly what a missing shared secret looks like.
     */
    fun read(payload: String): PairingMessage {
        val message = runCatching { json.decodeFromString(PairingMessage.serializer(), payload) }
            .getOrElse { throw SyncProtocolException.Unauthenticated }
        if (message.protocolVersion != SyncPayloadCodec.Version) {
            throw SyncProtocolException.IncompatibleVersion
        }
        return message
    }
}

internal sealed interface PairingResult {
    data class Paired(val peer: DeviceDescriptor, val secret: ByteArray) : PairingResult

    /** Either user said no, the peer turned out to be this device, or the handshake was abandoned. */
    data object Declined : PairingResult
}

/**
 * Runs the handshake over [transport].
 *
 * The side that opened the connection passes [peerHello] as null and speaks first; the side that
 * received the connection already read the hello while deciding what kind of session it is, so it
 * passes it in. That asymmetry is what lets the receiver identify a pairing without a separate
 * message type or a second characteristic.
 *
 * @param onPeer called on both sides with the peer and the session code. Suspends while the user
 *   compares the code on both screens; a false answer rejects the pairing before a secret exists.
 * @param onPaired called once the secret is final. On the receiving side this happens before the
 *   final acknowledgement is written, so the initiator never starts syncing against a device that
 *   has not persisted the same secret yet.
 */
internal suspend fun runPairingSession(
    transport: SyncTransport,
    local: PairingMessage.Hello,
    peerHello: PairingMessage.Hello?,
    onPeer: suspend (peer: DeviceDescriptor, code: String) -> Boolean,
    onPaired: suspend (peer: DeviceDescriptor, secret: ByteArray) -> Unit
): PairingResult {
    transport.open()
    try {
        val link = FramedLink(transport)

        // Both sides introduce themselves. The initiator speaks first only so the receiver could
        // already tell this apart from a sync before it answers.
        link.sendRaw(PairingMessageCodec.write(local))
        val peer = peerHello ?: PairingMessageCodec.read(link.readRaw())
        if (peer !is PairingMessage.Hello) return PairingResult.Declined

        if (peer.nonce.length != NonceHexLength) throw SyncProtocolException.Unauthenticated
        if (peer.device.deviceId == local.device.deviceId) {
            link.sendRaw(PairingMessageCodec.write(PairingMessage.Reject()))
            return PairingResult.Declined
        }

        val code = pairingCode(local, peer)
        if (!onPeer(peer.device, code)) {
            link.sendRaw(PairingMessageCodec.write(PairingMessage.Reject()))
            return PairingResult.Declined
        }

        // Both sides write before reading: whichever user answers first must not have to wait for the
        // other to finish reading their screen.
        link.sendRaw(PairingMessageCodec.write(PairingMessage.Accept()))
        when (PairingMessageCodec.read(link.readRaw())) {
            is PairingMessage.Accept -> Unit
            is PairingMessage.Reject -> return PairingResult.Declined
            else -> throw SyncProtocolException.Unauthenticated
        }

        if (peerHello == null) {
            // The initiator mints the secret. It is the only new state in the exchange, so letting one
            // side own it keeps the two devices from racing two different secrets.
            val secret = randomBytes(SecretSizeBytes)
            val proof = pairingProof(secret, local, peer)
            link.sendRaw(
                PairingMessageCodec.write(
                    PairingMessage.Secret(value = secret.toBase64(), proof = proof)
                )
            )

            val done = PairingMessageCodec.read(link.readRaw())
            if (done !is PairingMessage.Paired) return PairingResult.Declined
            if (!constantTimeEquals(done.proof.encodeToByteArray(), proof.encodeToByteArray())) {
                throw SyncProtocolException.Unauthenticated
            }

            onPaired(peer.device, secret)
            return PairingResult.Paired(peer.device, secret)
        }

        val handedOver = PairingMessageCodec.read(link.readRaw())
        if (handedOver !is PairingMessage.Secret) return PairingResult.Declined

        val secret = runCatching { handedOver.value.fromBase64() }
            .getOrElse { throw SyncProtocolException.Unauthenticated }
        if (secret.size != SecretSizeBytes) throw SyncProtocolException.Unauthenticated

        val expected = pairingProof(secret, local, peer)
        if (!constantTimeEquals(expected.encodeToByteArray(), handedOver.proof.encodeToByteArray())) {
            throw SyncProtocolException.Unauthenticated
        }

        onPaired(peer.device, secret)
        link.sendRaw(
            PairingMessageCodec.write(
                PairingMessage.Paired(proof = pairingProof(secret, local, peer))
            )
        )
        return PairingResult.Paired(peer.device, secret)
    } finally {
        transport.close()
    }
}

/**
 * The six characters both users compare before accepting.
 *
 * Sorting both inputs makes each side compute the same value regardless of who opened the connection.
 * Hashing the transcript keeps the code from leaking the nonces it is built from.
 */
internal suspend fun pairingCode(
    local: PairingMessage.Hello,
    peer: PairingMessage.Hello
): String {
    val deviceIds = listOf(local.device.deviceId, peer.device.deviceId).sorted()
    val nonces = listOf(local.nonce, peer.nonce).sorted()
    val transcript = "pair|${deviceIds[0]}|${deviceIds[1]}|${nonces[0]}|${nonces[1]}"
    return sha256(transcript.encodeToByteArray()).copyOf(FingerprintSizeBytes).toHex().uppercase()
}

private suspend fun pairingProof(
    secret: ByteArray,
    local: PairingMessage.Hello,
    peer: PairingMessage.Hello
): String {
    val nonces = listOf(local.nonce, peer.nonce).sorted()
    val transcript = "pair-proof|${nonces[0]}|${nonces[1]}"
    return hmacSha256(secret, transcript.encodeToByteArray()).toHex()
}

/** Same length as [deviceFingerprint], so both codes read the same way on screen. */
private const val FingerprintSizeBytes = 3

/** The secret is the same size as one created for the old out-of-band pairing. */
private const val SecretSizeBytes = 32
