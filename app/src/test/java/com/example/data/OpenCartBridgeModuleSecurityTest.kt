package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenCartBridgeModuleSecurityTest {

    @Test
    fun generatedBridgeNeverExposesOrAdoptsCredentials() {
        val script = OpenCartBridgeModule.generatePhpScript("valid-secret-key-1234567890")

        assertFalse(script.contains("get_key_setup"))
        assertFalse(script.contains("_REQUEST['api_key']"))
        assertFalse(script.contains("Access-Control-Allow-Origin: *"))
        assertTrue(script.contains("HTTP_X_CARTADMIN_KEY"))
        assertTrue(script.contains("hash_equals"))
        assertTrue(script.contains("/^Bearer\\s+([^\\s]+)$/i"))
        assertTrue(script.contains("valid-secret-key-1234567890"))
    }

    @Test
    fun unsafeEmbeddedKeyIsReplacedInsteadOfInterpolated() {
        val maliciousKey = "bad'; phpinfo(); //"
        val script = OpenCartBridgeModule.generatePhpScript(maliciousKey)

        assertFalse(script.contains(maliciousKey))
        assertTrue(script.contains("CARTADMIN_"))
    }
}
