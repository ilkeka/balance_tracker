package me.ilker.sync.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import me.ilker.balance_tracker.sync.SyncSnapshot

internal sealed class SyncProtocolException(message: String) : Exception(message) {
    /**
     * The payload did not carry a valid signature for the stored pairing secret, so the peer does not
     * hold it. Raised before any record reaches the database.
     */
    data object Unauthenticated : SyncProtocolException("payload failed authentication") {
        private fun readResolve(): Any = Unauthenticated
    }

    data object MalformedChunk : SyncProtocolException("chunk header is malformed") {
        private fun readResolve(): Any = MalformedChunk
    }

    data object MessageTooLarge : SyncProtocolException("message exceeded the size limit") {
        private fun readResolve(): Any = MessageTooLarge
    }

    data object MissingChunk : SyncProtocolException("a chunk of the message was never received") {
        private fun readResolve(): Any = MissingChunk
    }

    data object IncompatibleVersion : SyncProtocolException("peer speaks a different protocol version") {
        private fun readResolve(): Any = IncompatibleVersion
    }

    /** The peer presented our own descriptor, which means it found a stale advertisement of ours. */
    data object SameDevice : SyncProtocolException("the peer is this device") {
        private fun readResolve(): Any = SameDevice
    }
}

/**
 * A snapshot plus the signature proving the sender holds the pairing secret.
 *
 * Authenticating the payload rather than running a challenge-response handshake means both sides
 * simply write their sealed snapshot and read the peer's, so there is no message ordering to get
 * wrong and the same bytes work over Bluetooth or as a file.
 *
 * Replaying an older sealed payload is harmless rather than a vulnerability: every record carries a
 * version and the merge only accepts records that supersede the local copy, so a stale payload
 * contributes nothing. That property is why no shared cursor is needed.
 */
@Serializable
internal data class SealedSyncPayload(
    val protocolVersion: Int,
    val deviceId: String,
    val signature: String,
    val transactions: List<SyncedTransactionDto>
)

private val TransactionListSerializer = ListSerializer(SyncedTransactionDto.serializer())

internal object SyncPayloadCodec {
    const val Version: Int = 1

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun seal(secret: ByteArray, snapshot: SyncSnapshot): String = json.encodeToString(
        SealedSyncPayload.serializer(),
        SealedSyncPayload(
            protocolVersion = Version,
            deviceId = snapshot.deviceId,
            signature = sign(
                secret = secret,
                deviceId = snapshot.deviceId,
                transactions = snapshot.transactions.map { it.toDto() }
            ),
            transactions = snapshot.transactions.map { it.toDto() }
        )
    )

    /** Verifies the signature, then returns the snapshot. Nothing is merged unless this succeeds. */
    suspend fun open(secret: ByteArray, sealed: String): SyncSnapshot {
        val payload = json.decodeFromString(SealedSyncPayload.serializer(), sealed)
        if (payload.protocolVersion != Version) throw SyncProtocolException.IncompatibleVersion

        val expected = sign(
            secret = secret,
            deviceId = payload.deviceId,
            transactions = payload.transactions
        )
        if (!constantTimeEquals(expected.encodeToByteArray(), payload.signature.encodeToByteArray())) {
            throw SyncProtocolException.Unauthenticated
        }

        return SyncSnapshot(
            deviceId = payload.deviceId,
            transactions = payload.transactions.map { it.toSynced() }
        )
    }

    /**
     * Signs the canonical serialisation of the records rather than the sealed envelope, so reordering
     * or whitespace in the envelope cannot invalidate an otherwise valid signature.
     */
    private suspend fun sign(secret: ByteArray, deviceId: String, transactions: List<SyncedTransactionDto>): String =
        hmacSha256(
            key = secret,
            message = byteArrayOf(Version.toByte(), 0) +
                deviceId.encodeToByteArray() +
                byteArrayOf(0) +
                json.encodeToString(TransactionListSerializer, transactions).encodeToByteArray()
        ).toHex()
}