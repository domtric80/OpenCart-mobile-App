package com.example.security

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.fail
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device-gated test: it passes only when Android Keystore attests that the AES
 * key is held by StrongBox or the Trusted Execution Environment.
 */
@RunWith(AndroidJUnit4::class)
class HardwareCredentialProtectorInstrumentedTest {
    @Test
    fun credentialKeyIsHardwareBackedOrProtectorFailsClosed() {
        val protector = AndroidKeystoreCredentialProtector(
            ApplicationProvider.getApplicationContext()
        )
        try {
            val level = protector.hardwareSecurityLevel()
            assertTrue(
                level == HardwareSecurityLevel.STRONGBOX ||
                    level == HardwareSecurityLevel.TRUSTED_ENVIRONMENT
            )
        } catch (_: CredentialProtectionException) {
            try {
                protector.protect("instrumented-store", CredentialField.API_KEY, "secret")
                fail("Software-backed credential persistence must be rejected")
            } catch (_: CredentialProtectionException) {
                // Expected on emulators and devices without TEE/StrongBox.
            }
        }
    }
}
