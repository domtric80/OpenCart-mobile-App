package com.example.auth

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * One-time biometric gate backed by a non-exportable Android Keystore key.
 *
 * Merely receiving a successful biometric callback is not sufficient: the
 * authenticated Signature must sign the random challenge created for that
 * exact prompt, and the signature is verified before the app session opens.
 */
internal class BiometricUnlockCrypto {
    class PendingOperation internal constructor(
        val cryptoObject: BiometricPrompt.CryptoObject,
        internal val challenge: ByteArray
    )

    fun createOperation(): PendingOperation {
        val signer = initializeSignerWithRecovery()
        return PendingOperation(
            cryptoObject = BiometricPrompt.CryptoObject(signer),
            challenge = ByteArray(CHALLENGE_BYTES).also(SecureRandom()::nextBytes)
        )
    }

    fun completeOperation(
        authenticatedSigner: Signature?,
        pendingOperation: PendingOperation
    ): Boolean {
        if (authenticatedSigner == null) {
            pendingOperation.challenge.fill(0)
            return false
        }

        return try {
            authenticatedSigner.update(pendingOperation.challenge)
            val signedChallenge = authenticatedSigner.sign()
            val publicKey = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                .getCertificate(KEY_ALIAS)?.publicKey ?: return false
            val verifier = Signature.getInstance(SIGNATURE_ALGORITHM)
            verifier.initVerify(publicKey)
            verifier.update(pendingOperation.challenge)
            verifier.verify(signedChallenge)
        } catch (_: Exception) {
            false
        } finally {
            pendingOperation.challenge.fill(0)
        }
    }

    private fun initializeSignerWithRecovery(): Signature {
        return try {
            initializeSigner(getOrCreateHardwarePrivateKey())
        } catch (_: KeyPermanentlyInvalidatedException) {
            deleteKey()
            initializeSigner(getOrCreateHardwarePrivateKey())
        }
    }

    private fun initializeSigner(privateKey: PrivateKey): Signature =
        Signature.getInstance(SIGNATURE_ALGORITHM).apply { initSign(privateKey) }

    private fun getOrCreateHardwarePrivateKey(): PrivateKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? PrivateKey)?.let { privateKey ->
            requireHardwareBacking(privateKey)
            return privateKey
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            generateStrongBoxOrTeeKeyPair()
        } else {
            generateKeyPair(strongBox = false)
        }

        val generated = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            .getKey(KEY_ALIAS, null) as? PrivateKey
            ?: error("Chiave biometrica non disponibile")
        requireHardwareBacking(generated)
        return generated
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun generateStrongBoxOrTeeKeyPair() {
        try {
            generateKeyPair(strongBox = true)
        } catch (_: StrongBoxUnavailableException) {
            generateKeyPair(strongBox = false)
        }
    }

    private fun generateKeyPair(strongBox: Boolean) {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(strongBox)
        }

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE).apply {
            initialize(builder.build())
            generateKeyPair()
        }
    }

    @Suppress("DEPRECATION")
    private fun requireHardwareBacking(privateKey: PrivateKey) {
        val keyInfo = KeyFactory.getInstance(privateKey.algorithm, ANDROID_KEYSTORE)
            .getKeySpec(privateKey, KeyInfo::class.java)
        val hardwareBacked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX ||
                keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT
        } else {
            keyInfo.isInsideSecureHardware
        }
        if (!hardwareBacked) {
            deleteKey()
            error("La biometria non dispone di una chiave hardware TEE/StrongBox")
        }
    }

    private fun deleteKey() {
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
                deleteEntry(KEY_ALIAS)
            }
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "CartAdmin_BiometricUnlockSigningKey_v1"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val EC_CURVE = "secp256r1"
        private const val CHALLENGE_BYTES = 32
    }
}
