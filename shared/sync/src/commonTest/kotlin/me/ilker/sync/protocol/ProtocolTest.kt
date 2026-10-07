package me.ilker.sync.protocol

import kotlinx.coroutines.test.runTest
import me.ilker.balance_tracker.sync.SyncSnapshot
import me.ilker.balance_tracker.sync.SyncedTransaction
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.sync.transport.SyncTransport
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MessageFramingTest {

    @Test
    fun aMessageLargerThanOneChunkReassembles() {
        val payload = ByteArray(1000) { (it % 251).toByte() }

        val assembler = MessageAssembler()
        val completed = payload.toChunks(maxChunkSize = 100).mapNotNull { assembler.accept(it) }

        assertEquals(1, completed.size)
        assertContentEquals(payload, completed.single())
    }

    @Test
    fun aMessageSmallerThanOneChunkReassembles() {
        val payload = ByteArray(10) { 1 }

        val assembler = MessageAssembler()
        val completed = payload.toChunks(maxChunkSize = 100).mapNotNull { assembler.accept(it) }

        assertContentEquals(payload, completed.single())
    }

    /**
     * BLE notifications are not ordered, so the assembler has to key on the sequence number rather
     * than assuming arrival order.
     */
    @Test
    fun chunksMayArriveOutOfOrder() {
        val payload = ByteArray(1000) { (it % 251).toByte() }

        val assembler = MessageAssembler()
        val chunks = payload.toChunks(maxChunkSize = 100).reversed()
        val completed = chunks.mapNotNull { assembler.accept(it) }

        assertContentEquals(payload, completed.single())
    }

    @Test
    fun noMessageIsEmittedUntilEveryChunkArrives() {
        val payload = ByteArray(300) { 7 }
        val chunks = payload.toChunks(maxChunkSize = 100)

        val assembler = MessageAssembler()
        chunks.dropLast(1).forEach { assertNull(assembler.accept(it), "emitted before the final chunk") }

        assertContentEquals(payload, assembler.accept(chunks.last()))
    }

    @Test
    fun aMessageSplitOverManyChunksStillRoundTrips() {
        val payload = ByteArray(8 * 1024) { (it % 97).toByte() }

        val assembler = MessageAssembler()
        val chunks = payload.toChunks(maxChunkSize = 17)
        val completed = chunks.mapNotNull { assembler.accept(it) }

        assertEquals(1, completed.size)
        assertContentEquals(payload, completed.single())
    }

    @Test
    fun anOversizedMessageIsRejectedRatherThanBuffered() {
        val assembler = MessageAssembler(maxMessageSize = 24)

        assertFailsWith<SyncProtocolException.MessageTooLarge> {
            repeat(16) { index -> assembler.accept(frameOf(sequence = index, total = 16, size = 16)) }
        }
    }

    /**
     * A transfer cut off mid-message leaves the assembler waiting for chunks that will never come, so
     * recovery is an explicit reset. That is safe because the assembler is created per exchange, which
     * is what stops a truncated sync from poisoning the next one.
     */
    @Test
    fun resetClearsAPartiallyReceivedMessage() {
        val assembler = MessageAssembler()
        val chunks = ByteArray(300) { 7 }.toChunks(maxChunkSize = 100)
        assembler.accept(chunks.first())

        assembler.reset()

        assertContentEquals(ByteArray(4) { 3 }, assembler.accept(ByteArray(4) { 3 }.toChunks(maxChunkSize = 100).single()))
    }

    @Test
    fun aTruncatedHeaderIsRejected() {
        assertFailsWith<SyncProtocolException.MalformedChunk> {
            MessageAssembler().accept(ByteArray(2))
        }
    }

    @Test
    fun aZeroChunkCountIsRejected() {
        assertFailsWith<SyncProtocolException.MalformedChunk> {
            MessageAssembler().accept(byteArrayOf(0, 0, 0, 0, 9))
        }
    }

    private fun frameOf(sequence: Int, total: Int, size: Int): ByteArray =
        ByteArray(MessageAssembler.HeaderSize + size).also { frame ->
            frame[0] = (sequence shr 8 and 0xFF).toByte()
            frame[1] = (sequence and 0xFF).toByte()
            frame[2] = (total shr 8 and 0xFF).toByte()
            frame[3] = (total and 0xFF).toByte()
        }
}

class CryptoTest {

    @Test
    fun sha256MatchesKnownVector() = runTest {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sha256(ByteArray(0)).toHex()
        )
    }

    @Test
    fun sha256MatchesAbcVector() = runTest {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256("abc".encodeToByteArray()).toHex()
        )
    }

    /**
     * RFC 4231 test case 1. A non-empty key and a tag that must match exactly, otherwise pairing
     * would either reject valid devices or accept forged snapshots.
     */
    @Test
    fun hmacSha256MatchesRfc4231Case1() = runTest {
        val key = ByteArray(20) { 0x0b }
        val message = "Hi There".encodeToByteArray()

        assertEquals(
            "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
            hmacSha256(key = key, message = message).toHex()
        )
    }

    @Test
    fun hmacSha256MatchesRfc4231Case2() = runTest {
        assertEquals(
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
            hmacSha256(
                key = "Jefe".encodeToByteArray(),
                message = "what do ya want for nothing?".encodeToByteArray()
            ).toHex()
        )
    }

    @Test
    fun aKeyLongerThanTheBlockSizeIsHashedFirst() = runTest {
        val key = ByteArray(131) { 0xaa.toByte() }

        assertEquals(
            "60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54",
            hmacSha256(key = key, message = "Test Using Larger Than Block-Size Key - Hash Key First".encodeToByteArray()).toHex()
        )
    }

    @Test
    fun constantTimeComparisonDistinguishesDifferentTags() {
        assertEquals(false, constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 4)))
        assertEquals(false, constantTimeEquals(byteArrayOf(1, 2), byteArrayOf(1, 2, 3)))
        assertEquals(true, constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 3)))
    }
}

class PairingPayloadTest {

    @Test
    fun aPairingCodeRoundTrips() = runTest {
        val encoded = PairingPayload.create()

        val secret = PairingPayload.decode(encoded)

        assertEquals(32, secret.size)
        assertEquals(encoded, PairingPayload.encode(secret))
    }

    /**
     * A camera or a mistyped character can yield a code of the right shape but the wrong secret. The
     * embedded digest catches that here rather than as an opaque authentication failure during a sync.
     */
    @Test
    fun aMisreadCodeIsRejected() = runTest {
        val parts = PairingPayload.create().split(":")
        val misread = listOf(parts[0], parts[1], parts[2], randomBytes(8).toBase64()).joinToString(":")

        assertFailsWith<IllegalArgumentException> { PairingPayload.decode(misread) }
    }

    @Test
    fun aCorruptedSecretIsRejected() = runTest {
        val parts = PairingPayload.create().split(":").toMutableList()
        parts[2] = randomBytes(32).toBase64()

        assertFailsWith<IllegalArgumentException> { PairingPayload.decode(parts.joinToString(":")) }
    }

    @Test
    fun anArbitraryStringIsRejected() = runTest {
        assertFailsWith<IllegalArgumentException> { PairingPayload.decode("not a pairing code") }
    }

    @Test
    fun fingerprintIsShortAndStable() = runTest {
        val secret = PairingPayload.decode(PairingPayload.create())

        assertEquals(6, PairingPayload.fingerprint(secret).length)
        assertEquals(PairingPayload.fingerprint(secret), PairingPayload.fingerprint(secret))
    }
}

class Base64Test {

    @Test
    fun valuesRoundTrip() {
        for (size in 0..40) {
            val original = ByteArray(size) { (it * 7 + 3).toByte() }
            assertContentEquals(original, original.toBase64().fromBase64(), "size=$size")
        }
    }

    @Test
    fun knownVectorEncodes() {
        assertEquals("aGVsbG8=", "hello".encodeToByteArray().toBase64())
    }

    @Test
    fun paddingIsCorrect() {
        assertEquals("YQ==", byteArrayOf(0x61).toBase64())
        assertEquals("YWI=", byteArrayOf(0x61, 0x62).toBase64())
        assertEquals("YWJj", "abc".encodeToByteArray().toBase64())
    }
}

class HexTest {

    @Test
    fun valuesRoundTrip() {
        val original = ByteArray(64) { (it * 31 % 256).toByte() }
        assertContentEquals(original, original.toHex().hexToByteArray())
    }

    @Test
    fun knownVectorEncodes() {
        assertEquals("00ff10", byteArrayOf(0, -1, 16).toHex())
    }
}