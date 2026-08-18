package com.example.auth

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import java.security.MessageDigest
import java.security.SecureRandom

enum class DeviceLockType {
    NONE,                   // Nessun blocco schermo impostato
    STRONG_BIOMETRIC,       // Biometria forte: Impronta digitale o Riconoscimento Facciale 3D
    WEAK_DEVICE_CREDENTIAL  // PIN, Sequenza (segno), Password o Swipe
}

data class AuthStatus(
    val isPasswordConfigured: Boolean,
    val isLocked: Boolean,
    val lockType: DeviceLockType,
    val isPasswordExpired: Boolean,
    val requiresImmediateAuth: Boolean,
    val expiryMessage: String? = null
)

class SecurityManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "cartadmin_security_prefs"
        private const val KEY_PWD_HASH = "sec_pwd_hash"
        private const val KEY_PWD_SALT = "sec_pwd_salt"
        private const val KEY_LAST_AUTH_TIME = "sec_last_auth_time"
        private const val KEY_PWD_CREATED_AT = "sec_pwd_created_at"
        private const val KEY_BIOMETRIC_ENABLED = "sec_biometric_enabled"

        // 72 ore in millisecondi per PIN / Sequenza
        const val TIMEOUT_72_HOURS_MS = 72L * 60 * 60 * 1000
        // 90 giorni (~3 mesi) in millisecondi per scadenza password
        const val TIMEOUT_90_DAYS_MS = 90L * 24 * 60 * 60 * 1000
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

        // Verifica se è presente e configurata la biometria forte (impronta / face id hardware sicuro)
        val canAuthBiometric = biometricManager?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
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

    fun setPassword(password: String): Boolean {
        val check = validatePasswordStrength(password)
        if (!check.isValid) return false

        val salt = generateSalt()
        val hash = hashPassword(password, salt)

        prefs.edit()
            .putString(KEY_PWD_HASH, hash)
            .putString(KEY_PWD_SALT, salt)
            .putLong(KEY_PWD_CREATED_AT, System.currentTimeMillis())
            .putLong(KEY_LAST_AUTH_TIME, System.currentTimeMillis())
            .putBoolean(KEY_BIOMETRIC_ENABLED, true)
            .apply()
        return true
    }

    fun verifyPassword(password: String): Boolean {
        val savedHash = prefs.getString(KEY_PWD_HASH, null) ?: return false
        val savedSalt = prefs.getString(KEY_PWD_SALT, null) ?: return false

        val inputHash = hashPassword(password, savedSalt)
        if (savedHash == inputHash) {
            recordSuccessfulAuth()
            return true
        }
        return false
    }

    fun recordSuccessfulAuth() {
        prefs.edit()
            .putLong(KEY_LAST_AUTH_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Valuta lo stato di sicurezza in base alle regole:
     * 1. Se nessun blocco schermo: richiesta ad ogni apertura dell'app.
     * 2. Se blocco biometria / Face ID: non scade mai (autenticazione biometrica rapida).
     * 3. Se PIN / Segno / Swipe: scade dopo 72 ore di inattività, e la password deve essere rinnovata ogni 3 mesi.
     */
    fun evaluateAuthStatus(): AuthStatus {
        val isConfigured = isPasswordSet()
        if (!isConfigured) {
            return AuthStatus(
                isPasswordConfigured = false,
                isLocked = true,
                lockType = detectDeviceLockType(),
                isPasswordExpired = false,
                requiresImmediateAuth = true,
                expiryMessage = "Configura una password forte per proteggere l'accesso a CartAdmin."
            )
        }

        val lockType = detectDeviceLockType()
        val now = System.currentTimeMillis()
        val lastAuth = prefs.getLong(KEY_LAST_AUTH_TIME, 0L)
        val pwdCreatedAt = prefs.getLong(KEY_PWD_CREATED_AT, now)

        when (lockType) {
            DeviceLockType.NONE -> {
                // Nessun blocco schermo: chiedi password ad OGNI apertura
                return AuthStatus(
                    isPasswordConfigured = true,
                    isLocked = true,
                    lockType = lockType,
                    isPasswordExpired = false,
                    requiresImmediateAuth = true,
                    expiryMessage = "Nessun blocco schermo sul dispositivo: richiesta password ad ogni avvio."
                )
            }
            DeviceLockType.STRONG_BIOMETRIC -> {
                // Biometria / Face ID: sessione permanente finché la biometria è attiva
                return AuthStatus(
                    isPasswordConfigured = true,
                    isLocked = false,
                    lockType = lockType,
                    isPasswordExpired = false,
                    requiresImmediateAuth = false,
                    expiryMessage = null
                )
            }
            DeviceLockType.WEAK_DEVICE_CREDENTIAL -> {
                // PIN o Segno:
                // a) Scadenza password ogni 3 mesi (90 giorni)
                val isPwdExpired = (now - pwdCreatedAt) > TIMEOUT_90_DAYS_MS
                // b) Scadenza sessione dopo 72 ore di inattività
                val isSessionExpired = (now - lastAuth) > TIMEOUT_72_HOURS_MS

                val isLocked = isPwdExpired || isSessionExpired
                val msg = when {
                    isPwdExpired -> "La tua password è scaduta (valida per 90 giorni con blocco PIN/Segno). Inserisci la password per crearne una nuova."
                    isSessionExpired -> "Sessione scaduta dopo 72 ore di inattività. Inserisci la password di sicurezza."
                    else -> null
                }

                return AuthStatus(
                    isPasswordConfigured = true,
                    isLocked = isLocked,
                    lockType = lockType,
                    isPasswordExpired = isPwdExpired,
                    requiresImmediateAuth = isLocked,
                    expiryMessage = msg
                )
            }
        }
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = "$password:$salt".toByteArray(Charsets.UTF_8)
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
