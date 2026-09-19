package com.example.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BiometricCryptoBindingRegressionTest {
    private val projectRoot = File("..").canonicalFile

    @Test
    fun biometricSuccessMustConsumeCryptoObjectBeforeUnlocking() {
        val source = File(
            projectRoot,
            "app/src/main/java/com/example/auth/AuthLockScreen.kt"
        ).readText()

        val callbackStart = source.indexOf("override fun onAuthenticationSucceeded")
        val callbackEnd = source.indexOf("override fun onAuthenticationError", callbackStart)
        val callback = source.substring(callbackStart, callbackEnd)

        assertTrue(callback.contains("result.cryptoObject?.signature"))
        assertTrue(callback.contains("completeBiometricUnlock"))
        assertTrue(callback.indexOf("completeBiometricUnlock") < callback.indexOf("onUnlockSuccess"))
        assertTrue(source.contains("prompt.authenticate(promptInfo, pendingOperation.cryptoObject)"))
    }

    @Test
    fun cryptoPromptAllowsOnlyStrongBiometrics() {
        val source = File(
            projectRoot,
            "app/src/main/java/com/example/auth/AuthLockScreen.kt"
        ).readText()

        assertTrue(source.contains("setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)"))
        assertFalse(source.contains("DEVICE_CREDENTIAL"))
    }

    @Test
    fun unlockKeyIsAuthPerUseAndHardwareChecked() {
        val source = File(
            projectRoot,
            "app/src/main/java/com/example/auth/BiometricUnlockCrypto.kt"
        ).readText()

        assertTrue(
            Regex("setUserAuthenticationParameters\\s*\\(\\s*0\\s*,")
                .containsMatchIn(source)
        )
        assertTrue(source.contains("KeyProperties.AUTH_BIOMETRIC_STRONG"))
        assertTrue(source.contains("setUserAuthenticationValidityDurationSeconds(-1)"))
        assertTrue(source.contains("SECURITY_LEVEL_TRUSTED_ENVIRONMENT"))
        assertTrue(source.contains("SECURITY_LEVEL_STRONGBOX"))
        assertTrue(source.contains("verifier.verify(signedChallenge)"))
    }

    @Test
    fun cancellationAndAuthenticationErrorsCannotUnlockTheSession() {
        val source = File(
            projectRoot,
            "app/src/main/java/com/example/auth/AuthLockScreen.kt"
        ).readText()

        val errorStart = source.indexOf("override fun onAuthenticationError")
        val callbackEnd = source.indexOf("}", errorStart)
        val errorCallback = source.substring(errorStart, callbackEnd)

        assertTrue(errorStart >= 0)
        assertFalse(errorCallback.contains("onUnlockSuccess"))
        assertTrue(source.contains("setNegativeButtonText(\"Usa password CartAdmin\")"))
    }

    @Test
    fun invalidatedBiometricKeyIsDeletedAndRecreatedBeforeRetry() {
        val source = File(
            projectRoot,
            "app/src/main/java/com/example/auth/BiometricUnlockCrypto.kt"
        ).readText()

        val recoveryStart = source.indexOf("catch (_: KeyPermanentlyInvalidatedException)")
        val recoveryEnd = source.indexOf("}", recoveryStart)
        val recovery = source.substring(recoveryStart, recoveryEnd)

        assertTrue(recoveryStart >= 0)
        assertTrue(recovery.contains("deleteKey()"))
        assertTrue(recovery.contains("initializeSigner(getOrCreateHardwarePrivateKey())"))
    }
}
