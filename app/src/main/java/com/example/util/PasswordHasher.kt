package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom

object PasswordHasher {
    private val random = SecureRandom()

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(salt: String, password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$password".toByteArray(Charsets.UTF_8)
        val hashBytes = digest.digest(input)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        if (salt.isBlank() || expectedHash.length != 64) return false
        val actual = hashPassword(salt, password)
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }
}
