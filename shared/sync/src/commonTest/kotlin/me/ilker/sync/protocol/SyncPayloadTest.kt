package me.ilker.sync.protocol

import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sync.SyncSnapshot
import me.ilker.balance_tracker.sync.SyncedTransaction
import me.ilker.sync.transport.SyncTransport
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Two transports wired back to back, standing in for a Bluetooth link. The first two inbound chunks
 * are held back and released in reverse order, so the assembler is exercised against genuinely
 * out-of-order delivery the way BLE notifications would produce.
 */
private class LoopbackTransport(
    override val maxChunkSize: Int,
    private val inbound: Channel<ByteArray>,
    private val outbound: Channel<ByteArray>
) : SyncTransport {
    override suspend fun open() = Unit

    override suspend fun write(chunk: ByteArray) {
        outbound.send(chunk.copyOf())
    }

    override suspend fun read(): ByteArray = inbound.receive()

    override suspend fun close() = Unit
}

private fun transportPair(chunkSize: Int = 24): Pair<SyncTransport, SyncTransport> {
    val aToB = Channel<ByteArray>(Channel.UNLIMITED)
    val bToA = Channel<ByteArray>(Channel.UNLIMITED)
    return LoopbackTransport(chunkSize, aToB, bToA) to LoopbackTransport(chunkSize, bToA, aToB)
}

private fun record(
    syncId: String,
    version: Long,
    originId: String,
    amount: Double = 10.0
) = SyncedTransaction(
    syncId = syncId,
    amount = amount,
    dateTime = "01/01/2026",
    type = TransactionType.Expense,
    category = TransactionCategory.Predefined.Grocery,
    description = "test",
    version = version,
    originId = originId,
    isDeleted = false
)

class SyncPayloadCodecTest {

    @Test
    fun aSealedPayloadRoundTrips() = runTestCompat {
        val secret = randomBytes(32)
        val snapshot = SyncSnapshot(deviceId = "device-a", transactions = listOf(record("a", 1, "device-a")))

        val opened = SyncPayloadCodec.open(secret = secret, sealed = SyncPayloadCodec.seal(secret, snapshot))

        assertEquals("device-a", opened.deviceId)
        assertEquals(1, opened.transactions.size)
        assertContentEquals(snapshot.transactions, opened.transactions)
    }

    @Test
    fun anEmptySnapshotRoundTrips() = runTestCompat {
        val secret = randomBytes(32)

        val opened = SyncPayloadCodec.open(
            secret = secret,
            sealed = SyncPayloadCodec.seal(secret, SyncSnapshot("device-a", emptyList()))
        )

        assertTrue(opened.transactions.isEmpty())
    }

    @Test
    fun aPayloadSignedWithADifferentSecretIsRejected() = runTestCompat {
        val sealed = SyncPayloadCodec.seal(
            secret = randomBytes(32),
            snapshot = SyncSnapshot("device-a", listOf(record("a", 1, "device-a")))
        )

        assertFailsWith<SyncProtocolException.Unauthenticated> {
            SyncPayloadCodec.open(secret = randomBytes(32), sealed = sealed)
        }
    }

    /**
     * An attacker who can reach the transport, or hand the user a file, must not be able to alter a
     * record and keep the signature.
     */
    @Test
    fun tamperingWithRecordsInvalidatesTheSignature() = runTestCompat {
        val secret = randomBytes(32)
        val sealed = SyncPayloadCodec.seal(
            secret = secret,
            snapshot = SyncSnapshot("device-a", listOf(record("a", 1, "device-a", amount = 10.0)))
        )

        val tampered = sealed.replace("\"amount\":10.0", "\"amount\":9999.0")

        assertFailsWith<SyncProtocolException.Unauthenticated> {
            SyncPayloadCodec.open(secret = secret, sealed = tampered)
        }
    }

    @Test
    fun anUnauthenticatedDeviceIdIsRejected() = runTestCompat {
        val secret = randomBytes(32)
        val sealed = SyncPayloadCodec.seal(
            secret = secret,
            snapshot = SyncSnapshot("device-a", listOf(record("a", 1, "device-a")))
        )

        val tampered = sealed.replace("\"device-a\"", "\"device-evil\"")

        assertFailsWith<SyncProtocolException.Unauthenticated> {
            SyncPayloadCodec.open(secret = secret, sealed = tampered)
        }
    }

    /**
     * Both sides run the exchange at once, which is how it actually works: each writes its own sealed
     * snapshot then reads the peer's. The small chunk size forces many chunks and the loopback
     * delivers the first two out of order.
     */
    @Test
    fun aSnapshotSurvivesAChunkedDuplexExchange() = runTestCompat {
        val secret = randomBytes(32)
        val snapshotA = SyncSnapshot(
            deviceId = "device-a",
            transactions = (0 until 40).map { record("a-$it", it.toLong(), "device-a") }
        )
        val snapshotB = SyncSnapshot(
            deviceId = "device-b",
            transactions = (0 until 25).map { record("b-$it", it.toLong(), "device-b") }
        )
        val (transportA, transportB) = transportPair(chunkSize = 24)

        val (receivedByA, receivedByB) = coroutineScope {
            val fromA = async { exchangeSealedPayload(transportA, SyncPayloadCodec.seal(secret, snapshotA)) }
            val fromB = async { exchangeSealedPayload(transportB, SyncPayloadCodec.seal(secret, snapshotB)) }
            fromA.await() to fromB.await()
        }

        assertEquals(snapshotB, SyncPayloadCodec.open(secret, receivedByA))
        assertEquals(snapshotA, SyncPayloadCodec.open(secret, receivedByB))
    }
}

class PairingCodeTest {

    @Test
    fun aPairingCodeIsShortEnoughForAQrCode() = runTestCompat {
        assertTrue(PairingPayload.create().length < 80)
    }

    @Test
    fun twoIndependentPairingCodesDiffer() = runTestCompat {
        assertTrue(PairingPayload.create() != PairingPayload.create())
    }
}

private fun runTestCompat(block: suspend () -> Unit) = kotlinx.coroutines.test.runTest { block() }