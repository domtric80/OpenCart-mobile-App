package com.example.network

import android.content.Context
import java.security.SecureRandom

/**
 * Identificatore casuale dell'installazione CartAdmin.
 *
 * Non è un segreto e non deriva da IMEI, Android ID o altri identificatori
 * personali. Il backup dell'app è disabilitato, quindi una nuova installazione
 * riceve un nuovo identificatore e deve essere nuovamente associata al token.
 */
internal object BridgeDeviceIdentity {
    private const val PREFS_NAME = "cartadmin_bridge_identity"
    private const val KEY_INSTALLATION_ID = "installation_id_v1"
    private const val HEX = "0123456789abcdef"
    private val validId = Regex("[a-f0-9]{32}")

    @Volatile
    private var installationId: String = ""

    fun initialize(context: Context) {
        if (installationId.isNotEmpty()) return
        synchronized(this) {
            if (installationId.isNotEmpty()) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stored = prefs.getString(KEY_INSTALLATION_ID, null).orEmpty()
            installationId = if (validId.matches(stored)) {
                stored
            } else {
                val bytes = ByteArray(16).also(SecureRandom()::nextBytes)
                buildString(32) {
                    bytes.forEach { byte ->
                        val value = byte.toInt() and 0xff
                        append(HEX[value ushr 4])
                        append(HEX[value and 0x0f])
                    }
                }.also {
                    check(prefs.edit().putString(KEY_INSTALLATION_ID, it).commit()) {
                        "Impossibile salvare l'identità locale CartAdmin"
                    }
                }
            }
        }
    }

    fun current(): String = installationId.takeIf(validId::matches)
        ?: error("BridgeDeviceIdentity non inizializzata")

    internal fun setForTests(value: String) {
        require(validId.matches(value))
        installationId = value
    }
}
