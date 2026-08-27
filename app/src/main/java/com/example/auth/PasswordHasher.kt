package com.example.auth

import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal object PasswordHasher {
    private const val ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256
    private const val PREFIX_SHA256 = "pbkdf2_sha256"
    private const val PREFIX_SHA1 = "pbkdf2_sha1"

    fun hash(password: CharArray, salt: String): String {
        val (prefix, derived) = deriveWithBestAvailableAlgorithm(password, salt, ITERATIONS)
        return listOf(prefix, ITERATIONS.toString(), derived.toHex()).joinToString("$")
    }

    fun verify(password: CharArray, salt: String, encodedHash: String): Boolean {
        val parts = encodedHash.split('$')
        if (parts.size != 3) return false
        val algorithm = when (parts[0]) {
            PREFIX_SHA256 -> "PBKDF2WithHmacSHA256"
            PREFIX_SHA1 -> "PBKDF2WithHmacSHA1"
            else -> return false
        }
        val iterations = parts[1].toIntOrNull()?.takeIf { it in 100_000..1_000_000 } ?: return false
        val expected = parts[2].hexToBytes() ?: return false
        val actual = derive(password, salt, iterations, expected.size * 8, algorithm) ?: return false
        return MessageDigest.isEqual(expected, actual)
    }

    fun isModernHash(encodedHash: String): Boolean =
        encodedHash.substringBefore('$') == PREFIX_SHA256 || encodedHash.substringBefore('$') == PREFIX_SHA1

    fun verifyLegacySha256(password: CharArray, salt: String, encodedHash: String): Boolean {
        val legacy = MessageDigest.getInstance("SHA-256")
            .digest("${String(password)}:$salt".toByteArray(Charsets.UTF_8))
            .toHex()
        return MessageDigest.isEqual(
            legacy.toByteArray(Charsets.US_ASCII),
            encodedHash.toByteArray(Charsets.US_ASCII)
        )
    }

    private fun deriveWithBestAvailableAlgorithm(
        password: CharArray,
        salt: String,
        iterations: Int
    ): Pair<String, ByteArray> {
        derive(password, salt, iterations, KEY_LENGTH_BITS, "PBKDF2WithHmacSHA256")?.let {
            return PREFIX_SHA256 to it
        }
        val fallback = derive(password, salt, iterations, KEY_LENGTH_BITS, "PBKDF2WithHmacSHA1")
            ?: throw IllegalStateException("PBKDF2 non disponibile sul dispositivo")
        return PREFIX_SHA1 to fallback
    }

    private fun derive(
        password: CharArray,
        salt: String,
        iterations: Int,
        keyLengthBits: Int,
        algorithm: String
    ): ByteArray? = try {
        val spec = PBEKeySpec(password, salt.toByteArray(Charsets.UTF_8), iterations, keyLengthBits)
        try {
            SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    } catch (_: Exception) {
        null
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray? {
        if (length % 2 != 0 || !matches(Regex("[0-9a-fA-F]+"))) return null
        return try {
            ByteArray(length / 2) { index -> substring(index * 2, index * 2 + 2).toInt(16).toByte() }
        } catch (_: NumberFormatException) {
            null
        }
    }
}
