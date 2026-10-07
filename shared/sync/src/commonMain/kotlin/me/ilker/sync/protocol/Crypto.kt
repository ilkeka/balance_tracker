package me.ilker.sync.protocol

/**
 * HMAC-SHA256 and the constant-time comparison, built on the common [sha256].
 *
 * Everything cryptographic lives in common code, so the pairing digest and payload signature are
 * covered by the same test suite on every target rather than on whichever platforms happen to have
 * a native implementation.
 */
private const val BlockSize = 64

/**
 * HMAC-SHA256 (RFC 2104).
 *
 * Used to prove to both devices that the other side holds the secret exchanged during QR pairing,
 * and to stop a nearby BLE peer from injecting transactions into the ledger. "No server" does not
 * mean "no authentication": BLE advertising is broadcast to anything in range.
 */
internal suspend fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
    val normalizedKey = when {
        key.size > BlockSize -> sha256(key)
        else -> key
    }

    val innerPad = ByteArray(BlockSize) { 0x36 }
    val outerPad = ByteArray(BlockSize) { 0x5C }
    normalizedKey.forEachIndexed { index, byte ->
        innerPad[index] = (innerPad[index].toInt() xor byte.toInt()).toByte()
        outerPad[index] = (outerPad[index].toInt() xor byte.toInt()).toByte()
    }

    val inner = sha256(innerPad + message)
    return sha256(outerPad + inner)
}

/**
 * Compares two authentication tags without leaking their contents through timing.
 *
 * Length is compared first, which is unavoidable and not secret: the tag length is fixed by the
 * algorithm.
 */
internal fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
    if (a.size != b.size) return false
    var difference = 0
    for (index in a.indices) {
        difference = difference or (a[index].toInt() xor b[index].toInt())
    }
    return difference == 0
}