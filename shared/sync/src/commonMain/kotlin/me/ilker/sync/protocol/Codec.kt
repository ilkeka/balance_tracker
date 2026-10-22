package me.ilker.sync.protocol

private const val HexDigits = "0123456789abcdef"

private const val Base64Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

internal fun ByteArray.toHex(): String = buildString(size * 2) {
    for (byte in this@toHex) {
        val value = byte.toInt() and 0xFF
        append(HexDigits[value shr 4])
        append(HexDigits[value and 0x0F])
    }
}

internal fun hexToByteArray(hex: String): ByteArray {
    require(hex.length % 2 == 0) { "hex string must have an even length" }
    return ByteArray(hex.length / 2) { index ->
        val high = HexDigits.indexOf(hex[index * 2])
        val low = HexDigits.indexOf(hex[index * 2 + 1])
        require(high >= 0 && low >= 0) { "invalid hex digit" }
        ((high shl 4) or low).toByte()
    }
}

/** Standard base64 with padding. Pairing codes are QR payloads, so text must stay URL-safe. */
internal fun ByteArray.toBase64(): String = buildString {
    var index = 0
    while (index + 2 < size) {
        val chunk = (this@toBase64[index].toInt() and 0xFF shl 16) or
            (this@toBase64[index + 1].toInt() and 0xFF shl 8) or
            (this@toBase64[index + 2].toInt() and 0xFF)
        append(Base64Alphabet[chunk shr 18 and 0x3F])
        append(Base64Alphabet[chunk shr 12 and 0x3F])
        append(Base64Alphabet[chunk shr 6 and 0x3F])
        append(Base64Alphabet[chunk and 0x3F])
        index += 3
    }

    when (size - index) {
        1 -> {
            val chunk = this@toBase64[index].toInt() and 0xFF shl 16
            append(Base64Alphabet[chunk shr 18 and 0x3F])
            append(Base64Alphabet[chunk shr 12 and 0x3F])
            append("==")
        }

        2 -> {
            val chunk = (this@toBase64[index].toInt() and 0xFF shl 16) or
                (this@toBase64[index + 1].toInt() and 0xFF shl 8)
            append(Base64Alphabet[chunk shr 18 and 0x3F])
            append(Base64Alphabet[chunk shr 12 and 0x3F])
            append(Base64Alphabet[chunk shr 6 and 0x3F])
            append('=')
        }
    }
}

internal fun String.fromBase64(): ByteArray {
    val cleaned = filterNot { it == '=' || it == '\n' || it == '\r' }
    val output = mutableListOf<Byte>()

    var buffer = 0
    var bitsCollected = 0
    for (character in cleaned) {
        val value = Base64Alphabet.indexOf(character)
        require(value >= 0) { "invalid base64 character" }
        buffer = (buffer shl 6) or value
        bitsCollected += 6
        if (bitsCollected >= 8) {
            bitsCollected -= 8
            output += (buffer shr bitsCollected and 0xFF).toByte()
        }
    }

    return output.toByteArray()
}