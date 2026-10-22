package me.ilker.sync.protocol

import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals

/**
 * Known-answer tests from FIPS 180-4 and RFC 4231.
 *
 * These are the reason SHA-256 lives in common code: the same vectors now run on every target, so a
 * platform that silently computed a different digest would fail here rather than in a sync handshake.
 */
class Sha256Test {
    private fun hash(text: String): String = sha256(text.encodeToByteArray()).toHex()

    @Test
    fun anEmptyMessageHashesToTheKnownDigest() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hash("")
        )
    }

    @Test
    fun aShortMessageHashesToTheKnownDigest() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hash("abc")
        )
    }

    @Test
    fun aMultiBlockMessageHashesToTheKnownDigest() {
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            hash("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq")
        )
    }

    /**
     * The length is written into the final block, so a message that ends exactly on a block boundary
     * is the case most likely to pad wrongly.
     */
    @Test
    fun aMessageEndingOnABlockBoundaryHashesToTheKnownDigest() {
        val digest = sha256(ByteArray(64) { 0x61.toByte() }).toHex()

        assertEquals(
            "ffe054fe7ae0cb6dc65c3af9b61d5209f439851db43d0ba5997337df154668eb",
            digest
        )
    }

    @Test
    fun aLongMessageSpansManyBlocks() {
        val data = ByteArray(1000) { (it % 251).toByte() }

        assertEquals(32, sha256(data).size)
    }

    @Test
    fun hmacMatchesRfc4231CaseTwo() = runTest {
        // Key "Jefe", data "what do ya want for nothing?"
        val key = "Jefe".encodeToByteArray()
        val message = "what do ya want for nothing?".encodeToByteArray()

        assertEquals(
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
            hmacSha256(key, message).toHex()
        )
    }

    @Test
    fun hmacMatchesRfc4231CaseOne() = runTest {
        val key = ByteArray(20) { 0x0b.toByte() }
        val message = "Hi There".encodeToByteArray()

        assertEquals(
            "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
            hmacSha256(key, message).toHex()
        )
    }

    /**
     * A key longer than the block size has to be hashed first, which is easy to get wrong and would
     * only show up as an opaque authentication failure between two devices.
     */
    @Test
    fun anOverlongKeyIsHashedFirst() = runTest {
        val key = ByteArray(131) { 0xaa.toByte() }
        val message = "Test Using Larger Than Block-Size Key - Hash Key First".encodeToByteArray()

        assertEquals(
            "60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54",
            hmacSha256(key, message).toHex()
        )
    }

    @Test
    fun constantTimeComparisonDistinguishesEveryByte() {
        val value = sha256("balance".encodeToByteArray())

        assertEquals(true, constantTimeEquals(value, value.copyOf()))
        for (index in value.indices) {
            val altered = value.copyOf().also { it[index] = (it[index] + 1).toByte() }
            assertEquals(false, constantTimeEquals(value, altered))
        }
        assertEquals(false, constantTimeEquals(value, value.copyOf(value.size - 1)))
    }
}
