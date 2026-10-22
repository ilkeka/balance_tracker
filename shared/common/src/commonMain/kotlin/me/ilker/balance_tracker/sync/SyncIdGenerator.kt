package me.ilker.balance_tracker.sync

import kotlin.random.Random

private const val HexDigits = "0123456789abcdef"

/** Hex encode, for persisting a binary secret in the `Pairing` table. */
internal fun ByteArray.hexToString(): String = buildString(size * 2) {
    for (byte in this@hexToString) {
        val value = byte.toInt() and 0xFF
        append(HexDigits[value shr 4])
        append(HexDigits[value and 0x0F])
    }
}

internal fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "hex string must have an even length" }
    return ByteArray(length / 2) { index ->
        val high = HexDigits.indexOf(this[index * 2])
        val low = HexDigits.indexOf(this[index * 2 + 1])
        require(high >= 0 && low >= 0) { "invalid hex digit" }
        ((high shl 4) or low).toByte()
    }
}

/**
 * 128-bit random identifier, hex encoded.
 *
 * Used for both record ids and the per-install device id. Collisions are not a practical concern at
 * this size, and generating in common code avoids an expect/actual per platform.
 */
internal fun generateSyncId(): String {
    val bytes = Random.nextBytes(16)
    return buildString(bytes.size * 2) {
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            append(HexDigits[value shr 4])
            append(HexDigits[value and 0x0F])
        }
    }
}