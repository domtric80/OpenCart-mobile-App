package com.example.network

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import androidx.annotation.RequiresApi
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.UUID

internal data class BridgeDeviceProof(
    val deviceId: String,
    val publicKey: String,
    val timestampSeconds: Long,
    val nonce: String,
    val signature: String
)

/**
 * Identità crittografica non esportabile dell'installazione.
 *
 * La chiave privata ECDSA viene generata nel TEE/StrongBox tramite Android Keystore. Il server
 * associa al token la sola chiave pubblica e verifica una firma fresca per ogni richiesta.
 */
internal object BridgeDeviceIdentity {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "CartAdmin_BridgeDeviceSigningKey_v1"

    @Volatile
    private var initialized = false
    private var testDeviceId: String? = null

    fun initialize() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            getOrCreateHardwareKeyPair()
            initialized = true
        }
    }

    fun createProof(method: String, requestTarget: String): BridgeDeviceProof {
        testDeviceId?.let { deviceId ->
            return BridgeDeviceProof(deviceId, "dGVzdC1wdWJsaWMta2V5", 1_700_000_000L, "00000000-0000-4000-8000-000000000001", "dGVzdC1zaWduYXR1cmU=")
        }
        check(initialized) { "BridgeDeviceIdentity non inizializzata" }

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey
            ?: error("Chiave privata del dispositivo non disponibile")
        val publicKey = keyStore.getCertificate(KEY_ALIAS)?.publicKey
            ?: error("Chiave pubblica del dispositivo non disponibile")
        val publicKeyBytes = publicKey.encoded
        val deviceId = MessageDigest.getInstance("SHA-256")
            .digest(publicKeyBytes)
            .take(16)
            .joinToString("") { "%02x".format(it) }
        val timestamp = System.currentTimeMillis() / 1000L
        val nonce = UUID.randomUUID().toString()
        val canonical = "${method.uppercase()}\n$requestTarget\n$timestamp\n$nonce"
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(privateKey)
        signer.update(canonical.toByteArray(StandardCharsets.UTF_8))

        return BridgeDeviceProof(
            deviceId = deviceId,
            publicKey = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP),
            timestampSeconds = timestamp,
            nonce = nonce,
            signature = Base64.encodeToString(signer.sign(), Base64.NO_WRAP)
        )
    }

    private fun getOrCreateHardwareKeyPair() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            requireHardwareBacking(keyStore)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            generateStrongBoxOrTeeKeyPair()
        } else {
            generateKeyPair(false)
        }
        requireHardwareBacking(KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) })
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun generateStrongBoxOrTeeKeyPair() {
        try {
            generateKeyPair(true)
        } catch (_: StrongBoxUnavailableException) {
            generateKeyPair(false)
        }
    }

    private fun generateKeyPair(strongBox: Boolean) {
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(strongBox)
        }
        generator.initialize(builder.build())
        generator.generateKeyPair()
    }

    private fun requireHardwareBacking(keyStore: KeyStore) {
        val privateKey = keyStore.getKey(KEY_ALIAS, null)
            ?: error("Chiave dispositivo non disponibile")
        val keyInfo = KeyFactory.getInstance(privateKey.algorithm, ANDROID_KEYSTORE)
            .getKeySpec(privateKey, KeyInfo::class.java)
        val hardwareBacked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX ||
                keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT
        } else {
            @Suppress("DEPRECATION")
            keyInfo.isInsideSecureHardware
        }
        if (!hardwareBacked) {
            keyStore.deleteEntry(KEY_ALIAS)
            error("Il dispositivo non offre una chiave hardware TEE/StrongBox")
        }
    }

    internal fun setForTests(value: String) {
        require(value.matches(Regex("[a-f0-9]{32}")))
        testDeviceId = value
        initialized = true
    }
}
