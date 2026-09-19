package com.example.security

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeHardwareIdentitySecurityTest {
    private val source = File("src/main/java/com/example/network/BridgeDeviceIdentity.kt").readText()

    @Test
    fun bridgeIdentityUsesANonExportableHardwareEcdsaKey() {
        assertTrue(source.contains("KeyProperties.KEY_ALGORITHM_EC"))
        assertTrue(source.contains("ECGenParameterSpec(\"secp256r1\")"))
        assertTrue(source.contains("Signature.getInstance(\"SHA256withECDSA\")"))
        assertTrue(source.contains("setIsStrongBoxBacked(strongBox)"))
        assertTrue(source.contains("SECURITY_LEVEL_STRONGBOX"))
        assertTrue(source.contains("SECURITY_LEVEL_TRUSTED_ENVIRONMENT"))
        assertTrue(source.contains("keyStore.deleteEntry(KEY_ALIAS)"))
        assertFalse(source.contains("getSharedPreferences"))
        assertFalse(source.contains("SecureRandom"))
    }

    @Test
    fun bridgeIdentityIsADevicePossessionProofNotALocalAuthenticationKey() {
        assertTrue(source.contains("setUserAuthenticationRequired(false)"))
        assertTrue(source.contains("associa al token la sola chiave pubblica"))
        assertTrue(source.contains("val nonce = UUID.randomUUID().toString()"))
        assertTrue(source.contains("val timestamp = System.currentTimeMillis() / 1000L"))
    }
}
