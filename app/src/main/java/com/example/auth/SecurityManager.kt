package com.example.auth

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class DeviceLockType {
    NONE,                   // Nessun blocco schermo impostato sul dispositivo
    STRONG_BIOMETRIC,       // Biometria forte: Impronta digitale o Riconoscimento Facciale 3D hardware
    WEAK_DEVICE_CREDENTIAL  // PIN, Sequenza (segno), Password o Swipe
}

data class AuthStatus(
    val isPasswordConfigured: Boolean,
    val isLocked: Boolean,
    val lockType: DeviceLockType,
    val isPasswordExpired: Boolean,
    val requiresImmediateAuth: Boolean,
    val isBiometricEnabled: Boolean,
    val canUseBiometric: Boolean,
    val expiryMessage: String? = null
)

/**
 * SecurityManager — Sicurezza bancaria e crittografia hardware per CartAdmin.
 *
 * Caratteristiche:
 * - Crittografia Hardware con AndroidKeyStore (AES-256 GCM) per credenziali e dati sensibili
 * - Inattività massima di 5 minuti (stile app bancarie)
 * - Blocco immediato e richiesta login / sblocco biometrico ad ogni apertura
 * - Opzione nelle impostazioni per abilitare/disabilitare l'accesso biometrico rapido
 * - Policy password forte (min 8 car, Maiuscola, Numero, Simbolo)
 */
class SecurityManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "cartadmin_security_vault"
        private const val KEY_OPERATOR_USERNAME = "sec_vault_username"
        private const val KEY_PWD_HASH = "sec_vault_pwd_hash"
        private const val KEY_PWD_SALT = "sec_vault_pwd_salt"
        private const val KEY_LAST_ACTIVE_TIME = "sec_vault_last_active_time"
        private const val KEY_PWD_CREATED_AT = "sec_vault_pwd_created_at"
        private const val KEY_BIOMETRIC_PREF = "sec_vault_biometric_enabled"
        private const val LEGACY_KEY_ENCRYPTED_STORE_CREDS = "sec_vault_enc_store_creds"
        private const val LEGACY_KEY_STORE_CREDS_IV = "sec_vault_store_creds_iv"
        private const val KEY_BIOMETRIC_PROOF = "sec_vault_biometric_proof"
        private const val KEY_BIOMETRIC_PROOF_IV = "sec_vault_biometric_proof_iv"

        // Timeout inattività: 5 MINUTI (stile app bancaria)
        const val TIMEOUT_INACTIVITY_MS = 5L * 60 * 1000 // 5 minuti in millisecondi
        // 90 giorni per rotazione password consigliata
        const val TIMEOUT_90_DAYS_MS = 90L * 24 * 60 * 60 * 1000

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val LEGACY_KEYSTORE_ALIAS = "CartAdmin_HardwareMasterKey_v2"
        private const val BIOMETRIC_KEYSTORE_ALIAS = "CartAdmin_BiometricProofKey_v1"
        private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private val BIOMETRIC_PROOF_PLAINTEXT = "CartAdmin biometric proof v1".toByteArray(Charsets.UTF_8)
    }

    init {
        purgeLegacyCredentialCopy()
    }

    private fun purgeLegacyCredentialCopy() {
        prefs.edit()
            .remove(LEGACY_KEY_ENCRYPTED_STORE_CREDS)
            .remove(LEGACY_KEY_STORE_CREDS_IV)
            .apply()
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
                deleteEntry(LEGACY_KEYSTORE_ALIAS)
            }
        } catch (_: Exception) {
            // The obsolete duplicate was never the authoritative credential store.
        }
    }

    /**
     * Prepares an auth-bound cipher. The returned CryptoObject must be supplied to
     * BiometricPrompt.authenticate; a successful callback alone is not sufficient to unlock.
     */
    fun prepareBiometricCryptoObject(): BiometricPrompt.CryptoObject? {
        return prepareBiometricCryptoObjectInternal(retryAfterReset = true)
    }

    private fun prepareBiometricCryptoObjectInternal(
        retryAfterReset: Boolean
    ): BiometricPrompt.CryptoObject? {
        return try {
            val secretKey = getOrCreateBiometricKey()
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            val encryptedProof = prefs.getString(KEY_BIOMETRIC_PROOF, null)
            val proofIv = prefs.getString(KEY_BIOMETRIC_PROOF_IV, null)

            if (encryptedProof != null && proofIv != null) {
                val iv = Base64.decode(proofIv, Base64.NO_WRAP)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            }
            BiometricPrompt.CryptoObject(cipher)
        } catch (_: Exception) {
            if (!retryAfterReset) return null
            resetBiometricProof()
            prepareBiometricCryptoObjectInternal(retryAfterReset = false)
        }
    }

    /** Completes and verifies the Keystore operation authorized by the biometric prompt. */
    fun completeBiometricAuthentication(cryptoObject: BiometricPrompt.CryptoObject?): Boolean {
        val cipher = cryptoObject?.cipher ?: return false
        return try {
            val encryptedProof = prefs.getString(KEY_BIOMETRIC_PROOF, null)
            if (encryptedProof == null) {
                val proof = cipher.doFinal(BIOMETRIC_PROOF_PLAINTEXT)
                prefs.edit()
                    .putString(KEY_BIOMETRIC_PROOF, Base64.encodeToString(proof, Base64.NO_WRAP))
                    .putString(KEY_BIOMETRIC_PROOF_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                    .apply()
                true
            } else {
                val decrypted = cipher.doFinal(Base64.decode(encryptedProof, Base64.NO_WRAP))
                MessageDigest.isEqual(decrypted, BIOMETRIC_PROOF_PLAINTEXT)
            }
        } catch (_: Exception) {
            resetBiometricProof()
            false
        }
    }

    private fun getOrCreateBiometricKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(BIOMETRIC_KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            BIOMETRIC_KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
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

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    private fun resetBiometricProof() {
        prefs.edit()
            .remove(KEY_BIOMETRIC_PROOF)
            .remove(KEY_BIOMETRIC_PROOF_IV)
            .apply()
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
                deleteEntry(BIOMETRIC_KEYSTORE_ALIAS)
            }
        } catch (_: Exception) {
            // A new key will be generated on the next biometric attempt.
        }
    }

    /**
     * Rileva il tipo di blocco dello schermo presente sul dispositivo Android
     */
    fun detectDeviceLockType(): DeviceLockType {
        val biometricManager = try {
            BiometricManager.from(context)
        } catch (_: Exception) {
            null
        }

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val hasDeviceSecure = keyguardManager?.isDeviceSecure ?: false

        // Verifica biometria forte (impronta / face id hardware sicuro)
        val canAuthBiometric = biometricManager?.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )
        if (canAuthBiometric == BiometricManager.BIOMETRIC_SUCCESS) {
            return DeviceLockType.STRONG_BIOMETRIC
        }

        // Se è protetto da PIN, Segno o Password dispositivo
        if (hasDeviceSecure) {
            return DeviceLockType.WEAK_DEVICE_CREDENTIAL
        }

        return DeviceLockType.NONE
    }

    /**
     * Verifica se il dispositivo ha hardware biometrico funzionante e configurato.
     */
    fun isHardwareBiometricAvailable(): Boolean {
        val biometricManager = try {
            BiometricManager.from(context)
        } catch (_: Exception) {
            null
        }
        val status = biometricManager?.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Preferenza utente per l'uso della biometria (attivabile/disattivabile nelle impostazioni).
     */
    fun isBiometricEnabledByUser(): Boolean {
        // Se non specificato, se la biometria è supportata è abilitata di default per massima comodità
        return prefs.getBoolean(KEY_BIOMETRIC_PREF, true)
    }

    fun setBiometricEnabledByUser(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_PREF, enabled).apply()
    }

    /**
     * Verifica la complessità richiesta per la password:
     * - Almeno 8 caratteri
     * - Almeno una lettera maiuscola
     * - Almeno un numero
     * - Almeno un carattere speciale/simbolo
     */
    fun validatePasswordStrength(password: String): PasswordStrengthResult {
        if (password.length < 8) {
            return PasswordStrengthResult(false, "La password deve contenere almeno 8 caratteri.")
        }
        if (!password.any { it.isUpperCase() }) {
            return PasswordStrengthResult(false, "La password deve contenere almeno una lettera MAIUSCOLA.")
        }
        if (!password.any { it.isDigit() }) {
            return PasswordStrengthResult(false, "La password deve contenere almeno un numero (0-9).")
        }
        val symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?/~`'\""
        if (!password.any { symbols.contains(it) }) {
            return PasswordStrengthResult(false, "La password deve contenere almeno un simbolo speciale (!@#\$%^&*...).")
        }
        return PasswordStrengthResult(true, "Password forte valida.")
    }

    data class PasswordStrengthResult(val isValid: Boolean, val message: String)

    fun isPasswordSet(): Boolean {
        return prefs.getString(KEY_PWD_HASH, null) != null
    }

    fun getOperatorUsername(): String {
        return prefs.getString(KEY_OPERATOR_USERNAME, "admin") ?: "admin"
    }

    fun setCredentials(username: String, password: String): Boolean {
        val check = validatePasswordStrength(password)
        if (!check.isValid) return false

        val cleanUsername = if (username.isNotBlank()) username.trim() else "admin"
        val salt = generateSalt()
        val hash = hashPassword(password, salt)

        prefs.edit()
            .putString(KEY_OPERATOR_USERNAME, cleanUsername)
            .putString(KEY_PWD_HASH, hash)
            .putString(KEY_PWD_SALT, salt)
            .putLong(KEY_PWD_CREATED_AT, System.currentTimeMillis())
            .putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis())
            .putBoolean(KEY_BIOMETRIC_PREF, isHardwareBiometricAvailable())
            .apply()
        return true
    }

    fun setPassword(password: String): Boolean {
        return setCredentials(getOperatorUsername(), password)
    }

    fun getDeviceModelName(): String {
        val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: ""
        val model = Build.MODEL ?: "Android Device"
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }.trim()
    }

    fun getAndroidVersionString(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    fun verifyPassword(password: String): Boolean {
        val savedHash = prefs.getString(KEY_PWD_HASH, null) ?: return false
        val savedSalt = prefs.getString(KEY_PWD_SALT, null) ?: return false

        val passwordChars = password.toCharArray()
        val isModernHash = PasswordHasher.isModernHash(savedHash)
        val isValid = if (isModernHash) {
            PasswordHasher.verify(passwordChars, savedSalt, savedHash)
        } else {
            PasswordHasher.verifyLegacySha256(passwordChars, savedSalt, savedHash)
        }
        passwordChars.fill('\u0000')

        if (isValid) {
            if (!isModernHash) {
                val migratedSalt = generateSalt()
                prefs.edit()
                    .putString(KEY_PWD_SALT, migratedSalt)
                    .putString(KEY_PWD_HASH, hashPassword(password, migratedSalt))
                    .apply()
            }
            recordSuccessfulAuth()
            return true
        }
        return false
    }

    /**
     * Registra l'attività utente / autenticazione riuscita, aggiornando il timestamp di sessione attiva.
     */
    fun recordSuccessfulAuth() {
        prefs.edit()
            .putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Chiamato quando l'app va in background o quando passano più di 5 minuti di inattività.
     */
    fun lockSession() {
        prefs.edit()
            .putLong(KEY_LAST_ACTIVE_TIME, 0L)
            .apply()
    }

    /**
     * Valutazione stato sicurezza banking-grade:
     * - Richiesta autenticazione all'avvio o dopo 5 minuti di inattività in background
     * - Supporto Biometrico per sblocco istantaneo
     */
    fun evaluateAuthStatus(isFreshAppLaunch: Boolean = false): AuthStatus {
        val isConfigured = isPasswordSet()
        val lockType = detectDeviceLockType()
        val hasBiometricHw = isHardwareBiometricAvailable()
        val isBioEnabled = isBiometricEnabledByUser()
        val canUseBio = hasBiometricHw && isBioEnabled

        if (!isConfigured) {
            return AuthStatus(
                isPasswordConfigured = false,
                isLocked = true,
                lockType = lockType,
                isPasswordExpired = false,
                requiresImmediateAuth = true,
                isBiometricEnabled = isBioEnabled,
                canUseBiometric = canUseBio,
                expiryMessage = "Configura le credenziali di accesso e proteggi i dati del tuo OpenCart."
            )
        }

        val now = System.currentTimeMillis()
        val lastActive = prefs.getLong(KEY_LAST_ACTIVE_TIME, 0L)
        val pwdCreatedAt = prefs.getLong(KEY_PWD_CREATED_AT, now)

        val isPwdExpired = (now - pwdCreatedAt) > TIMEOUT_90_DAYS_MS
        val isSessionTimedOut = (now - lastActive) > TIMEOUT_INACTIVITY_MS || lastActive == 0L || isFreshAppLaunch

        val isLocked = isPwdExpired || isSessionTimedOut

        val expiryMsg = when {
            isPwdExpired -> "La password è scaduta (rotazione programmata ogni 90 giorni). Inseriscila per rinnovarla."
            isSessionTimedOut -> "Sessione protetta: sblocca l'app con impronta/volto o inserisci la password."
            else -> null
        }

        return AuthStatus(
            isPasswordConfigured = true,
            isLocked = isLocked,
            lockType = lockType,
            isPasswordExpired = isPwdExpired,
            requiresImmediateAuth = isLocked,
            isBiometricEnabled = isBioEnabled,
            canUseBiometric = canUseBio,
            expiryMessage = expiryMsg
        )
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String {
        val passwordChars = password.toCharArray()
        return try {
            PasswordHasher.hash(passwordChars, salt)
        } finally {
            passwordChars.fill('\u0000')
        }
    }
}

