package me.ilker.sync.protocol

import kotlin.random.Random

private const val Scheme = "btsync"

/**
 * The one-time secret shared by QR pairing, in the form that goes into the code.
 *
 * Format: `btsync:1:<base64 secret>:<base64 digest prefix>`.
 *
 * The digest prefix lets the scanning device reject a misread code immediately with a clear message
 * instead of failing later with an opaque authentication error. Both devices store the secret
 * afterwards, so the pairing only has to happen once.
 */
internal object PairingPayload {
    private const val SecretSizeBytes = 32
    private const val DigestPrefixSizeBytes = 8

    suspend fun create(): String {
        val secret = randomBytes(SecretSizeBytes)
        return encode(secret)
    }

    suspend fun encode(secret: ByteArray): String {
        val digest = sha256(secret).copyOf(DigestPrefixSizeBytes)
        return listOf(Scheme, SyncPayloadCodec.Version.toString(), secret.toBase64(), digest.toBase64())
            .joinToString(":")
    }

    /** Returns the secret, or throws if the code is malformed or was misread. */
    suspend fun decode(payload: String): ByteArray {
        val parts = payload.trim().split(":")
        require(parts.size == 4 && parts[0] == Scheme) { "not a pairing code" }
        require(parts[1].toInt() == SyncPayloadCodec.Version) { "pairing code has an unsupported version" }

        val secret = parts[2].fromBase64()
        val expectedDigest = parts[3].fromBase64()
        require(secret.size == SecretSizeBytes) { "pairing code has the wrong secret length" }

        val actualDigest = sha256(secret).copyOf(DigestPrefixSizeBytes)
        require(constantTimeEquals(actualDigest, expectedDigest)) { "pairing code was read incorrectly" }

        return secret
    }

    /** Short, human-readable prefix so two devices can be told apart without a full exchange. */
    fun fingerprint(secret: ByteArray): String = secret.copyOf(3).toHex().uppercase()
}

internal fun randomBytes(size: Int): ByteArray = Random.nextBytes(size)

internal fun randomNonce(): String = randomBytes(16).toHex()