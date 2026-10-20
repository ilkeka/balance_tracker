package me.ilker.sync.protocol

import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun pairingHello(
    deviceId: String,
    alias: String = "Pixel",
    nonce: String = "11".repeat(16)
) = PairingMessage.Hello(
    device = DeviceDescriptor(
        deviceId = deviceId,
        alias = alias,
        model = null,
        type = DeviceType.Mobile,
        fingerprint = "000000"
    ),
    nonce = nonce
)

/** Same asymmetry as the sync tests: one side writes larger chunks than the other. */
private fun linkedTransports(): Pair<LinkedTransport, LinkedTransport> {
    val toReceiver = Channel<ByteArray>(Channel.UNLIMITED)
    val toInitiator = Channel<ByteArray>(Channel.UNLIMITED)
    return LinkedTransport(64, toInitiator, toReceiver) to
        LinkedTransport(20, toReceiver, toInitiator)
}

class PairingSessionTest {

    @Test
    fun `both sides compare the same code and store the same secret`() = runTest {
        val (initiator, receiver) = linkedTransports()
        val initiatorHello = pairingHello(deviceId = "aaaa", alias = "Pixel")
        val receiverHello = pairingHello(
            deviceId = "bbbb",
            alias = "Tablet",
            nonce = "22".repeat(16)
        )

        var codeSeenByInitiator: String? = null
        var codeSeenByReceiver: String? = null
        var secretOnInitiator: ByteArray? = null
        var secretOnReceiver: ByteArray? = null

        val server = backgroundScope.async {
            // Mirrors serveIncoming: the receiver reads the first message to know this is a pairing
            // before handing the already-read hello to the session.
            val first = receiver.readMessageText()
            val peerHello = assertIs<PairingMessage.Hello>(PairingMessageCodec.read(first))
            runPairingSession(
                transport = receiver,
                local = receiverHello,
                peerHello = peerHello,
                onPeer = { _, code ->
                    codeSeenByReceiver = code
                    true
                },
                onPaired = { _, secret -> secretOnReceiver = secret }
            )
        }

        val result = runPairingSession(
            transport = initiator,
            local = initiatorHello,
            peerHello = null,
            onPeer = { _, code ->
                codeSeenByInitiator = code
                true
            },
            onPaired = { _, secret -> secretOnInitiator = secret }
        )

        assertEquals(codeSeenByInitiator, codeSeenByReceiver)
        assertEquals(6, codeSeenByInitiator?.length)
        assertIs<PairingResult.Paired>(result)
        assertIs<PairingResult.Paired>(server.await())
        assertContentEquals(secretOnInitiator, secretOnReceiver)
        assertEquals(32, secretOnInitiator?.size)
        assertTrue(initiator.isClosed && receiver.isClosed, "the pairing link must be closed afterwards")
    }

    @Test
    fun `a decline on either side stops the handshake before a secret exists`() = runTest {
        val (initiator, receiver) = linkedTransports()
        var secretOnInitiator: ByteArray? = null
        var secretOnReceiver: ByteArray? = null

        val server = backgroundScope.async {
            val first = receiver.readMessageText()
            val peerHello = assertIs<PairingMessage.Hello>(PairingMessageCodec.read(first))
            runPairingSession(
                transport = receiver,
                local = pairingHello(deviceId = "bbbb"),
                peerHello = peerHello,
                onPeer = { _, _ -> false },
                onPaired = { _, secret -> secretOnReceiver = secret }
            )
        }

        val result = runPairingSession(
            transport = initiator,
            local = pairingHello(deviceId = "aaaa"),
            peerHello = null,
            onPeer = { _, _ -> true },
            onPaired = { _, secret -> secretOnInitiator = secret }
        )

        assertEquals(PairingResult.Declined, result)
        assertEquals(PairingResult.Declined, server.await())
        assertNull(secretOnInitiator)
        assertNull(secretOnReceiver)
    }

    @Test
    fun `a device does not pair with itself`() = runTest {
        val (initiator, receiver) = linkedTransports()
        val same = pairingHello(deviceId = "aaaa")
        var asked = false

        val server = backgroundScope.async {
            val first = receiver.readMessageText()
            val peerHello = assertIs<PairingMessage.Hello>(PairingMessageCodec.read(first))
            runPairingSession(
                transport = receiver,
                local = same,
                peerHello = peerHello,
                onPeer = { _, _ -> asked = true; true },
                onPaired = { _, _ -> }
            )
        }

        val result = runPairingSession(
            transport = initiator,
            local = same,
            peerHello = null,
            onPeer = { _, _ -> asked = true; true },
            onPaired = { _, _ -> }
        )

        assertEquals(PairingResult.Declined, result)
        assertEquals(PairingResult.Declined, server.await())
        assertFalse(asked, "no user should be asked to compare a device with itself")
    }

    @Test
    fun `the session code is symmetric and changes with the nonce`() = runTest {
        val first = pairingHello(deviceId = "aaaa", nonce = "11".repeat(16))
        val second = pairingHello(deviceId = "bbbb", nonce = "22".repeat(16))

        val code = pairingCode(first, second)
        assertEquals(code, pairingCode(second, first))
        assertEquals(6, code.length)
        assertTrue(code.all { it in '0'..'9' || it in 'A'..'F' })

        val refreshed = second.copy(nonce = "33".repeat(16))
        assertNotEquals(code, pairingCode(first, refreshed))
    }

    @Test
    fun `messages survive a round trip`() {
        val messages = listOf(
            pairingHello(deviceId = "aaaa"),
            PairingMessage.Accept(),
            PairingMessage.Reject(),
            PairingMessage.Secret(value = "c2VjcmV0", proof = "abc123"),
            PairingMessage.Paired(proof = "abc123")
        )

        messages.forEach { message ->
            assertEquals(message, PairingMessageCodec.read(PairingMessageCodec.write(message)))
        }
    }

    @Test
    fun `a body that is not one of ours is reported as unauthenticated`() {
        assertIs<SyncProtocolException.Unauthenticated>(
            runCatching { PairingMessageCodec.read("""{"kind":"NotOurMessage"}""") }.exceptionOrNull()
        )
    }

    @Test
    fun `a peer on another protocol version is reported as incompatible`() {
        val encoded = PairingMessageCodec.write(PairingMessage.Accept()).replace(
            """"protocolVersion":${SyncPayloadCodec.Version}""",
            """"protocolVersion":${SyncPayloadCodec.Version + 1}"""
        )

        assertIs<SyncProtocolException.IncompatibleVersion>(
            runCatching { PairingMessageCodec.read(encoded) }.exceptionOrNull()
        )
    }
}
