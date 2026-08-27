package com.example.auth

import java.security.MessageDigest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun pbkdf2HashVerifiesOnlyTheCorrectPassword() {
        val salt = "00112233445566778899aabbccddeeff"
        val hash = PasswordHasher.hash("Correct1!".toCharArray(), salt)

        assertTrue(PasswordHasher.isModernHash(hash))
        assertTrue(PasswordHasher.verify("Correct1!".toCharArray(), salt, hash))
        assertFalse(PasswordHasher.verify("Wrong1!".toCharArray(), salt, hash))
    }

    @Test
    fun legacySha256CanBeRecognizedForMigration() {
        val password = "Legacy1!"
        val salt = "legacy-salt"
        val legacyHash = MessageDigest.getInstance("SHA-256")
            .digest("$password:$salt".toByteArray())
            .joinToString("") { "%02x".format(it) }

        assertFalse(PasswordHasher.isModernHash(legacyHash))
        assertTrue(PasswordHasher.verifyLegacySha256(password.toCharArray(), salt, legacyHash))
        assertFalse(PasswordHasher.verifyLegacySha256("Wrong1!".toCharArray(), salt, legacyHash))
    }
}
