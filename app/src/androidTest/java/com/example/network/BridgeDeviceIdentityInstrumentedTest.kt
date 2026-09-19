package com.example.network

import android.os.Build
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BridgeDeviceIdentityInstrumentedTest {
    @Test
    fun signingKeyIsHardwareBackedAndDoesNotImpersonateLocalAuthentication() {
        try {
            BridgeDeviceIdentity.initialize()
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val privateKey = keyStore.getKey(BRIDGE_KEY_ALIAS, null) as PrivateKey
            val keyInfo = KeyFactory.getInstance(privateKey.algorithm, "AndroidKeyStore")
                .getKeySpec(privateKey, KeyInfo::class.java)

            val hardwareBacked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX ||
                    keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT
            } else {
                @Suppress("DEPRECATION")
                keyInfo.isInsideSecureHardware
            }
            assertTrue(hardwareBacked)
            assertFalse(keyInfo.isUserAuthenticationRequired)
        } catch (_: IllegalStateException) {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            assertFalse(keyStore.containsAlias(BRIDGE_KEY_ALIAS))
        }
    }

    companion object {
        private const val BRIDGE_KEY_ALIAS = "CartAdmin_BridgeDeviceSigningKey_v1"
    }
}
