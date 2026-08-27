package com.example.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM credential protection whose non-exportable key must live in
 * StrongBox or the device Trusted Execution Environment (TEE).
 *
 * A software-backed key is deliberately rejected. StrongBox is requested first
 * on supported Android versions and TEE is used only when StrongBox is absent.
 */
class AndroidKeystoreCredentialProtector(context: Context) : CredentialProtector {
    private val applicationId = context.applicationContext.packageName

    override fun protect(storeId: String, field: CredentialField, plainText: String): String {
        if (plainText.isEmpty()) return ""
        validateStoreId(storeId)

        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateHardwareKey())
            cipher.updateAAD(aad(storeId, field))
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            return listOf(
                FORMAT_PREFIX,
                Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
                Base64.encodeToString(encrypted, Base64.NO_WRAP)
            ).joinToString(SEPARATOR)
        } catch (error: CredentialProtectionException) {
            throw error
        } catch (error: Exception) {
            throw CredentialProtectionException("Impossibile cifrare le credenziali con Android Keystore", error)
        }
    }

    override fun reveal(
        storeId: String,
        field: CredentialField,
        persistedValue: String
    ): RevealedCredential {
        if (persistedValue.isEmpty()) return RevealedCredential("", false)
        validateStoreId(storeId)
        if (!isProtectedValue(persistedValue)) {
            return RevealedCredential(persistedValue, true)
        }

        val parts = persistedValue.split(SEPARATOR, limit = 3)
        if (parts.size != 3 || parts[0] != FORMAT_PREFIX) {
            throw CredentialProtectionException("Formato credenziale cifrata non valido")
        }

        try {
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[2], Base64.NO_WRAP)
            if (iv.size != GCM_IV_BYTES || encrypted.size < GCM_TAG_BYTES) {
                throw CredentialProtectionException("Credenziale cifrata danneggiata")
            }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateHardwareKey(),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            cipher.updateAAD(aad(storeId, field))
            val decrypted = cipher.doFinal(encrypted)
            return RevealedCredential(String(decrypted, Charsets.UTF_8), false)
        } catch (error: CredentialProtectionException) {
            throw error
        } catch (error: Exception) {
            throw CredentialProtectionException(
                "Credenziale non decifrabile: chiave hardware assente, invalidata o dati alterati",
                error
            )
        }
    }

    override fun hardwareSecurityLevel(): HardwareSecurityLevel =
        inspectHardwareSecurityLevel(getOrCreateHardwareKey())

    private fun getOrCreateHardwareKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { key ->
            inspectHardwareSecurityLevel(key)
            return key
        }

        val generated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                generateKey(strongBox = true)
            } catch (_: StrongBoxUnavailableException) {
                generateKey(strongBox = false)
            }
        } else {
            generateKey(strongBox = false)
        }

        return try {
            inspectHardwareSecurityLevel(generated)
            generated
        } catch (error: CredentialProtectionException) {
            keyStore.deleteEntry(KEY_ALIAS)
            throw error
        }
    }

    private fun generateKey(strongBox: Boolean): SecretKey {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(strongBox)
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(builder.build())
        return generator.generateKey()
    }

    @Suppress("DEPRECATION")
    private fun inspectHardwareSecurityLevel(key: SecretKey): HardwareSecurityLevel {
        val factory = SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEYSTORE)
        val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return when (keyInfo.getSecurityLevel()) {
                KeyProperties.SECURITY_LEVEL_STRONGBOX -> HardwareSecurityLevel.STRONGBOX
                KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> HardwareSecurityLevel.TRUSTED_ENVIRONMENT
                else -> throw CredentialProtectionException(
                    "Il dispositivo non offre una chiave Android Keystore protetta da TEE o StrongBox"
                )
            }
        }
        if (!keyInfo.isInsideSecureHardware()) {
            throw CredentialProtectionException(
                "Il dispositivo non offre una chiave Android Keystore protetta da hardware"
            )
        }
        return HardwareSecurityLevel.TRUSTED_ENVIRONMENT
    }

    private fun aad(storeId: String, field: CredentialField): ByteArray =
        "$applicationId|$FORMAT_PREFIX|$storeId|${field.storageName}".toByteArray(Charsets.UTF_8)

    private fun validateStoreId(storeId: String) {
        if (storeId.isBlank() || storeId.length > MAX_STORE_ID_LENGTH) {
            throw CredentialProtectionException("Identificativo store non valido per la cifratura")
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "CartAdmin_StoreCredentials_HardwareKey_v3"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val FORMAT_PREFIX = "ca1"
        private const val SEPARATOR = ":"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        private const val GCM_IV_BYTES = 12
        private const val MAX_STORE_ID_LENGTH = 256

        fun isProtectedValue(value: String): Boolean = value.startsWith("$FORMAT_PREFIX$SEPARATOR")
    }
}
