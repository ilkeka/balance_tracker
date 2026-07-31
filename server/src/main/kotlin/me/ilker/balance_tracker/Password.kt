package me.ilker.balance_tracker

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal fun passwordHash(password: String): String {
    val salt = SecureRandom().generateSeed(16)
    val keySpec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val hash = factory.generateSecret(keySpec).encoded
    return "65536:${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(hash)}"
}

internal fun passwordVerify(password: String, stored: String): Boolean {
    val parts = stored.split(":")
    val iterations = parts[0].toInt()
    val salt = Base64.getDecoder().decode(parts[1])
    val expectedHash = Base64.getDecoder().decode(parts[2])
    val keySpec = PBEKeySpec(password.toCharArray(), salt, iterations, expectedHash.size * 8)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val actualHash = factory.generateSecret(keySpec).encoded
    return actualHash.contentEquals(expectedHash)
}
