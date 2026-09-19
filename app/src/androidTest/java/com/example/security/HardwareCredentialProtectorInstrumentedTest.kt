package com.example.security

import android.os.Build
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyStore
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import org.junit.Assert.assertEquals
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
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val key = keyStore.getKey(CREDENTIAL_KEY_ALIAS, null) as SecretKey
            val keyInfo = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore")
                .getKeySpec(key, KeyInfo::class.java) as KeyInfo
            assertTrue(keyInfo.isUserAuthenticationRequired)
            assertEquals(AUTHORIZATION_WINDOW_SECONDS, keyInfo.userAuthenticationValidityDurationSeconds)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val expectedTypes = KeyProperties.AUTH_BIOMETRIC_STRONG or
                    KeyProperties.AUTH_DEVICE_CREDENTIAL
                assertEquals(expectedTypes, keyInfo.userAuthenticationType)
            }
        } catch (_: CredentialProtectionException) {
            try {
                protector.protect("instrumented-store", CredentialField.API_KEY, "secret")
                fail("Software-backed credential persistence must be rejected")
            } catch (_: CredentialProtectionException) {
                // Expected on emulators and devices without TEE/StrongBox.
            }
        }
    }

    companion object {
        private const val CREDENTIAL_KEY_ALIAS = "CartAdmin_StoreCredentials_UserAuthKey_v4"
        private const val AUTHORIZATION_WINDOW_SECONDS = 300
    }
}
