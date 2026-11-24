package me.ilker.sync.protocol

/**
 * Reassembles a message that was split into transport-sized chunks.
 *
 * BLE characteristics move far less data than a snapshot needs (an MTU is typically 20 to 500
 * bytes), so every message is chunked. Chunks may arrive out of order or interleaved, hence the
 * sequence numbers and the [total] count.
 *
 * Header layout, big-endian: `[sequence: 2][total: 2][payload...]`.
 */
internal class MessageAssembler(
    private val maxMessageSize: Int = DefaultMaxMessageSize
) {
    private var chunks: MutableMap<Int, ByteArray>? = null
    private var expectedTotal: Int = 0

    /**
     * Feeds one chunk and returns a message once the final chunk of a sequence arrives, otherwise
     * null. A message that would exceed [maxMessageSize] is rejected rather than buffered.
     */
    fun accept(chunk: ByteArray): ByteArray? {
        if (chunk.size < HeaderSize) throw SyncProtocolException.MalformedChunk

        val sequence = ((chunk[0].toInt() and 0xFF) shl 8) or (chunk[1].toInt() and 0xFF)
        val total = ((chunk[2].toInt() and 0xFF) shl 8) or (chunk[3].toInt() and 0xFF)
        if (total == 0) throw SyncProtocolException.MalformedChunk

        val payload = chunk.copyOfRange(HeaderSize, chunk.size)

        val pending = chunks ?: mutableMapOf<Int, ByteArray>().also { started ->
            chunks = started
            expectedTotal = total
        }

        if (pending.size + payload.size > maxMessageSize) {
            reset()
            throw SyncProtocolException.MessageTooLarge
        }

        pending[sequence] = payload
        if (pending.size < expectedTotal) return null

        val assembled = (0 until expectedTotal).fold(ByteArray(0)) { acc, index ->
            acc + (pending[index] ?: throw SyncProtocolException.MissingChunk)
        }
        reset()
        return assembled
    }

    fun reset() {
        chunks = null
        expectedTotal = 0
    }

    companion object {
        const val HeaderSize: Int = 4
        const val DefaultMaxMessageSize: Int = 8 * 1024 * 1024
    }
}

/**
 * Splits a message into frames no larger than [maxFrameSize], header included.
 *
 * The budget covers the whole frame rather than just the payload: a Bluetooth link can only carry
 * `MTU - 3` bytes in one notification or write, and the four header bytes are part of that. Treating
 * the budget as payload-only would make every frame four bytes too large, which the peer rejects or
 * silently truncates, so the exchange would never finish.
 */
internal fun ByteArray.toChunks(maxFrameSize: Int): List<ByteArray> {
    val payloadSize = maxFrameSize - MessageAssembler.HeaderSize
    require(payloadSize > 0) { "frame size too small for header" }
    if (isEmpty()) return listOf(frame(sequence = 0, total = 1, payload = ByteArray(0)))

    val total = (size + payloadSize - 1) / payloadSize
    return (0 until total).map { index ->
        val start = index * payloadSize
        val end = minOf(start + payloadSize, size)
        frame(sequence = index, total = total, payload = copyOfRange(start, end))
    }
}

private fun frame(sequence: Int, total: Int, payload: ByteArray): ByteArray = ByteArray(
    MessageAssembler.HeaderSize + payload.size
).also { frame ->
    frame[0] = (sequence shr 8 and 0xFF).toByte()
    frame[1] = (sequence and 0xFF).toByte()
    frame[2] = (total shr 8 and 0xFF).toByte()
    frame[3] = (total and 0xFF).toByte()
    payload.copyInto(frame, destinationOffset = MessageAssembler.HeaderSize)
}