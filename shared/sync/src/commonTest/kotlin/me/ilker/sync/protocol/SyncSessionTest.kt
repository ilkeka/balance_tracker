package me.ilker.sync.protocol

import me.ilker.balance_tracker.sync.SyncSnapshot
import me.ilker.balance_tracker.sync.SyncedTransaction
import me.ilker.sync.transport.SyncTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Two transports wired back to back, standing in for a Bluetooth link. Each side gets a different chunk
 * size, so a session is exercised against the asymmetry a real link has: the side that negotiated a
 * larger MTU writes more per chunk than the side that did not.
 */
/**
 * Runs a session and records what happened instead of letting a failure escape into the test scope,
 * which is what lets the "both sides reject each other" cases assert on the outcome at all.
 */
internal suspend fun runSessionCatching(
    transport: SyncTransport,
    local: DeviceDescriptor,
    secret: ByteArray,
    snapshot: SyncSnapshot,
    request: SyncMessage.Request?,
    onRequest: suspend (IncomingSyncRequest) -> Boolean
): Result<SyncSessionResult> = runCatching {
    runSyncSession(
        transport = transport,
        local = local,
        secret = secret,
        snapshot = snapshot,
        request = request,
        onRequest = onRequest
    )
}

internal class LinkedTransport(
    override val maxChunkSize: Int,
    private val inbound: Channel<ByteArray>,
    private val outbound: Channel<ByteArray>
) : SyncTransport {
    var isClosed = false

    override suspend fun open() = Unit

    override suspend fun write(chunk: ByteArray) {
        outbound.send(chunk.copyOf())
    }

    override suspend fun read(): ByteArray = inbound.receive()

    override suspend fun close() {
        isClosed = true
    }
}

private fun transports(
    initiatorChunkSize: Int = 64,
    receiverChunkSize: Int = 20
): Pair<LinkedTransport, LinkedTransport> {
    val toReceiver = Channel<ByteArray>(Channel.UNLIMITED)
    val toInitiator = Channel<ByteArray>(Channel.UNLIMITED)
    return LinkedTransport(initiatorChunkSize, toInitiator, toReceiver) to
        LinkedTransport(receiverChunkSize, toReceiver, toInitiator)
}

private fun descriptor(
    deviceId: String = "00112233445566778899aabbccddeeff",
    alias: String = "Pixel",
    model: String = "Pixel 9",
    type: DeviceType = DeviceType.Mobile
) = DeviceDescriptor(
    deviceId = deviceId,
    alias = alias,
    model = model,
    type = type,
    fingerprint = "A1B2C3"
)

private fun snapshot(deviceId: String, count: Int = 2) = SyncSnapshot(
    deviceId = deviceId,
    transactions = List(count) { index ->
        SyncedTransaction(
            syncId = "sync-$index",
            amount = 10.0 + index,
            dateTime = "01/01/2026",
            type = me.ilker.balance_tracker.sdk.TransactionType.Expense,
            category = me.ilker.balance_tracker.sdk.TransactionCategory.Predefined.Grocery,
            description = "test",
            version = index + 1L,
            originId = deviceId,
            isDeleted = false
        )
    }
)

private val Secret = ByteArray(16) { it.toByte() }

class SyncSessionTest {
    @Test
    fun `initiator receives the snapshot it accepted`() = runTest {
        val (initiator, receiver) = transports()
        val mine = snapshot("aaaa")
        val theirs = snapshot("bbbb")

        val server = backgroundScope.async {
            runSyncSession(
                transport = receiver,
                local = descriptor(deviceId = "bbbb", alias = "Tablet"),
                secret = Secret,
                snapshot = theirs,
                request = null,
                onRequest = { true }
            )
        }

        val result = runSyncSession(
            transport = initiator,
            local = descriptor(),
            secret = Secret,
            snapshot = mine,
            request = SyncMessage.Request(device = descriptor(), transactionCount = mine.transactions.size),
            onRequest = { false }
        )

        assertEquals(mine, assertIs<SyncSessionResult.Completed>(server.await()).peerSnapshot)
        assertEquals(theirs, assertIs<SyncSessionResult.Completed>(result).peerSnapshot)
    }

    @Test
    fun `both sides learn who the peer proved itself to be`() = runTest {
        val (initiator, receiver) = transports()
        val initiatorLocal = descriptor(alias = "Pixel")
        val receiverLocal = descriptor(deviceId = "bbbb", alias = "Tablet", type = DeviceType.Desktop)

        val server = backgroundScope.async {
            runSyncSession(
                transport = receiver,
                local = receiverLocal,
                secret = Secret,
                snapshot = snapshot("bbbb"),
                request = null,
                onRequest = { true }
            )
        }

        val result = assertIs<SyncSessionResult.Completed>(
            runSyncSession(
                transport = initiator,
                local = initiatorLocal,
                secret = Secret,
                snapshot = snapshot("aaaa"),
                request = SyncMessage.Request(device = initiatorLocal, transactionCount = 0),
                onRequest = { false }
            )
        )

        // The identity has to come from the authenticated hello, since the advertisement only carries a
        // truncated id and the screen persists whatever this reports.
        assertEquals(receiverLocal, result.peer)
        assertEquals(initiatorLocal, assertIs<SyncSessionResult.Completed>(server.await()).peer)
    }

    @Test
    fun `declining the request moves nothing in either direction`() = runTest {
        val (initiator, receiver) = transports()

        val server = backgroundScope.async {
            runSyncSession(
                transport = receiver,
                local = descriptor(deviceId = "bbbb"),
                secret = Secret,
                snapshot = snapshot("bbbb"),
                request = null,
                onRequest = { false }
            )
        }

        val result = runSyncSession(
            transport = initiator,
            local = descriptor(),
            secret = Secret,
            snapshot = snapshot("aaaa"),
            request = SyncMessage.Request(device = descriptor(), transactionCount = 0),
            onRequest = { false }
        )

        assertEquals(SyncSessionResult.Declined, server.await())
        assertEquals(SyncSessionResult.Declined, result)
    }

    @Test
    fun `a peer holding a different secret cannot complete a session`() = runTest {
        val (initiator, receiver) = transports()
        val otherSecret = ByteArray(16) { (it + 1).toByte() }

        val server = backgroundScope.async {
            runSessionCatching(
                transport = receiver,
                local = descriptor(deviceId = "bbbb"),
                secret = otherSecret,
                snapshot = snapshot("bbbb"),
                request = null,
                onRequest = { true }
            )
        }

        val error = assertFailsWith<SyncProtocolException.Unauthenticated> {
            runSyncSession(
                transport = initiator,
                local = descriptor(),
                secret = Secret,
                snapshot = snapshot("aaaa"),
                request = SyncMessage.Request(device = descriptor(), transactionCount = 0),
                onRequest = { false }
            )
        }

        assertEquals(SyncProtocolException.Unauthenticated, error)
        assertEquals(
            SyncProtocolException.Unauthenticated,
            server.await().exceptionOrNull()
        )
    }

    @Test
    fun `a device syncing with itself reports no merge instead of doubling up`() = runTest {
        val (initiator, receiver) = transports()
        val same = descriptor()

        val server = backgroundScope.async {
            runSyncSession(
                transport = receiver,
                local = same,
                secret = Secret,
                snapshot = snapshot("aaaa"),
                request = null,
                onRequest = { true }
            )
        }

        val result = runSyncSession(
            transport = initiator,
            local = same,
            secret = Secret,
            snapshot = snapshot("aaaa"),
            request = SyncMessage.Request(device = same, transactionCount = 0),
            onRequest = { false }
        )

        assertEquals(SyncSessionResult.Declined, server.await())
        assertEquals(SyncSessionResult.Declined, result)
    }

    @Test
    fun `the transport is closed even when the session fails`() = runTest {
        val (initiator, receiver) = transports()

        val server = backgroundScope.async {
            runSessionCatching(
                transport = receiver,
                local = descriptor(deviceId = "bbbb"),
                secret = ByteArray(16) { 7 },
                snapshot = snapshot("bbbb"),
                request = null,
                onRequest = { true }
            )
        }

        assertFailsWith<SyncProtocolException.Unauthenticated> {
            runSyncSession(
                transport = initiator,
                local = descriptor(),
                secret = Secret,
                snapshot = snapshot("aaaa"),
                request = SyncMessage.Request(device = descriptor(), transactionCount = 0),
                onRequest = { false }
            )
        }

        assertTrue(initiator.isClosed, "a failed session must not leave the link open")
        server.await().isFailure
    }

    @Test
    fun `a sealed payload that has been altered cannot be opened`() = runTest {
        val sealed = SyncPayloadCodec.seal(Secret, snapshot("aaaa"))

        // The signature is hex, so swapping one character for a different hex digit keeps the JSON
        // parseable. That isolates the failure to the tag rather than to the decoder, which is the
        // property that matters: the digest is what detects tampering, not well-formed JSON.
        val tagStart = sealed.indexOf("signature\":\"") + "signature\":\"".length
        val altered = sealed.toCharArray().also {
            it[tagStart + 4] = if (it[tagStart + 4] == 'a') 'b' else 'a'
        }.concatToString()

        assertNotEquals(sealed, altered, "the payload should have been altered")
        assertFailsWith<SyncProtocolException.Unauthenticated> { SyncPayloadCodec.open(Secret, altered) }
    }

    @Test
    fun `a transaction changed after signing is rejected`() = runTest {
        val sealed = SyncPayloadCodec.seal(Secret, snapshot("aaaa"))

        // Same JSON shape, different transaction: the signature still parses but no longer matches.
        val altered = sealed.replace("{\"syncId\":\"sync-0\",\"amount\":10.0", "{\"syncId\":\"sync-0\",\"amount\":99.0")
        assertNotEquals(sealed, altered, "the amount should have been altered")

        assertFailsWith<SyncProtocolException.Unauthenticated> { SyncPayloadCodec.open(Secret, altered) }
    }

    @Test
    fun `a payload sealed with another secret cannot be opened`() = runTest {
        val sealed = SyncPayloadCodec.seal(Secret, snapshot("aaaa"))

        assertFailsWith<SyncProtocolException.Unauthenticated> {
            SyncPayloadCodec.open(ByteArray(16) { 3 }, sealed)
        }
    }

    @Test
    fun `the request carries the initiator's count so the receiver can decide`() = runTest {
        val (initiator, receiver) = transports()
        val seen = CompletableDeferred<Int>()

        val server = backgroundScope.async {
            runSyncSession(
                transport = receiver,
                local = descriptor(deviceId = "bbbb"),
                secret = Secret,
                snapshot = snapshot("bbbb"),
                request = null,
                onRequest = { request ->
                    seen.complete(request.transactionCount)
                    true
                }
            )
        }

        val mine = snapshot("aaaa", count = 5)
        runSyncSession(
            transport = initiator,
            local = descriptor(),
            secret = Secret,
            snapshot = mine,
            request = SyncMessage.Request(device = descriptor(), transactionCount = mine.transactions.size),
            onRequest = { false }
        )

        assertEquals(5, seen.await())
        server.await()
    }

    @Test
    fun `the approval prompt identifies the peer by its advertised name and fingerprint`() = runTest {
        val (initiator, receiver) = transports()
        val seen = CompletableDeferred<IncomingSyncRequest>()

        val server = backgroundScope.async {
            runSyncSession(
                transport = receiver,
                local = descriptor(deviceId = "bbbb"),
                secret = Secret,
                snapshot = snapshot("bbbb"),
                request = null,
                onRequest = { request ->
                    seen.complete(request)
                    true
                }
            )
        }

        val local = descriptor(alias = "Pixel", type = DeviceType.Mobile)
        runSyncSession(
            transport = initiator,
            local = local,
            secret = Secret,
            snapshot = snapshot("aaaa"),
            request = SyncMessage.Request(device = local, transactionCount = 1),
            onRequest = { false }
        )

        val request = seen.await()
        assertEquals("Pixel", request.device.alias)
        assertEquals("A1B2C3", request.verifyCode)
        server.await()
    }
}

class SyncMessageCodecTest {
    @Test
    fun `every message survives a round trip`() {
        val messages = listOf(
            SyncMessage.Hello(device = descriptor(), proof = "abc123"),
            SyncMessage.Request(device = descriptor(), transactionCount = 7),
            SyncMessage.Accept(),
            SyncMessage.Reject()
        )

        messages.forEach { message ->
            assertEquals(message, SyncMessageCodec.read(SyncMessageCodec.write(message)))
        }
    }

    @Test
    fun `a body that is not one of ours is reported as unauthenticated rather than incompatible`() {
        assertFailsWith<SyncProtocolException.Unauthenticated> {
            SyncMessageCodec.read("""{"kind":"NotOurMessage"}""")
        }
    }

    @Test
    fun `a peer on another protocol version is reported as incompatible`() {
        val encoded = SyncMessageCodec.write(SyncMessage.Accept()).replace(
            """"protocolVersion":${SyncPayloadCodec.Version}""",
            """"protocolVersion":${SyncPayloadCodec.Version + 1}"""
        )

        assertFailsWith<SyncProtocolException.IncompatibleVersion> { SyncMessageCodec.read(encoded) }
    }
}

class DescriptorCodecTest {
    @Test
    fun `a descriptor survives the fixed-width advertisement encoding`() {
        val decoded = DescriptorCodec.decode(DescriptorCodec.encode(descriptor()))

        assertEquals(
            DeviceDescriptor(
                deviceId = "00112233",
                alias = "Pixel",
                model = "Pixel",
                type = DeviceType.Mobile,
                fingerprint = "A1B2C3"
            ),
            decoded
        )
    }

    @Test
    fun `an uppercase fingerprint does not break encoding`() {
        // Fingerprints are published uppercase for the approval prompt, so this is the shape every real
        // advertisement has; the hex decoder is lowercase-only.
        val real = DeviceDescriptor(
            deviceId = "00112233445566778899aabbccddeeff",
            alias = "Pixel",
            model = "Pixel 9",
            type = DeviceType.Mobile,
            fingerprint = deviceFingerprint(ByteArray(16) { 0xAB.toByte() })
        )

        assertEquals(real.fingerprint, DescriptorCodec.decode(DescriptorCodec.encode(real))?.fingerprint)
    }

    @Test
    fun `the payload stays inside the size older controllers accept`() {
        val longest = descriptor(alias = "abcdefghijklmnop", model = "Pixel 9 Pro Max")

        val payload = DescriptorCodec.encode(longest)

        assertEquals(DescriptorCodec.MaxSize, payload.size)
        assertEquals("abcdefgh", DescriptorCodec.decode(payload)?.alias)
        assertEquals("Pixel", DescriptorCodec.decode(payload)?.model)
    }

    @Test
    fun `every device type round trips`() {
        DeviceType.entries.forEach { type ->
            val original = descriptor(type = type)
            assertEquals(type, DescriptorCodec.decode(DescriptorCodec.encode(original))?.type)
        }
    }

    @Test
    fun `an advertiser that is not this app is ignored instead of crashing the scan`() {
        assertNull(DescriptorCodec.decode(ByteArray(DescriptorCodec.MaxSize)))

        val wrongVersion = DescriptorCodec.encode(descriptor()).also {
            it[0] = (SyncPayloadCodec.Version + 1).toByte()
        }
        assertNull(DescriptorCodec.decode(wrongVersion))
    }

    @Test
    fun `a truncated advertisement is rejected rather than read as garbage`() {
        val full = DescriptorCodec.encode(descriptor())

        assertNull(DescriptorCodec.decode(full.copyOf(full.size - 1)))
    }
}

class DeviceFingerprintTest {
    @Test
    fun `the fingerprint is the same on both devices and changes with the secret`() {
        assertEquals(deviceFingerprint(Secret), deviceFingerprint(Secret.copyOf()))
        assertFalse(
            deviceFingerprint(Secret) == deviceFingerprint(ByteArray(16) { (it + 1).toByte() })
        )
        assertEquals(6, deviceFingerprint(Secret).length)
    }
}

class SyncSessionFramingTest {
    @Test
    fun `a payload larger than the negotiated chunk size is split and reassembled`() = runTest {
        // 20 bytes per chunk against a multi-kilobyte snapshot: the assembler is the only thing keeping
        // the record intact, and BLE really does deliver this little at a time.
        val (initiator, receiver) = transports(initiatorChunkSize = 20, receiverChunkSize = 20)
        val large = snapshot("aaaa", count = 60)

        val server = backgroundScope.async {
            runSyncSession(
                transport = receiver,
                local = descriptor(deviceId = "bbbb"),
                secret = Secret,
                snapshot = snapshot("bbbb", count = 60),
                request = null,
                onRequest = { true }
            )
        }

        val result = runSyncSession(
            transport = initiator,
            local = descriptor(),
            secret = Secret,
            snapshot = large,
            request = SyncMessage.Request(device = descriptor(), transactionCount = 60),
            onRequest = { false }
        )

        // The initiator receives the serving device's snapshot, not its own.
        val theirs = snapshot("bbbb", count = 60)
        assertContentEquals(
            theirs.transactions,
            assertIs<SyncSessionResult.Completed>(result).peerSnapshot?.transactions
        )
        assertContentEquals(
            large.transactions,
            assertIs<SyncSessionResult.Completed>(server.await()).peerSnapshot?.transactions
        )
    }
}