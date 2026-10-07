package me.ilker.sync.protocol

import me.ilker.sync.transport.SyncTransport

/**
 * Swaps one sealed payload with a paired device.
 *
 * Both sides write their own sealed snapshot and then read the peer's, so the exchange is symmetric
 * and needs no roles, no handshake ordering and no stored cursor. The full snapshot goes both ways
 * every time; because merges are per record and version ordered, repeating a sync is cheap and
 * running it twice changes nothing.
 */
internal suspend fun exchangeSealedPayload(transport: SyncTransport, sealed: String): String {
    transport.open()
    try {
        sealed.encodeToByteArray()
            .toChunks(maxChunkSize = transport.maxChunkSize)
            .forEach { transport.write(it) }

        return readMessage(transport)
    } finally {
        transport.close()
    }
}

private suspend fun readMessage(transport: SyncTransport): String {
    val assembler = MessageAssembler()
    while (true) {
        val assembled = assembler.accept(transport.read())
        if (assembled != null) return assembled.decodeToString()
    }
}