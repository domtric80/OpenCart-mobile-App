package com.example.notification

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FcmTokenManager {

    private const val PREFS_NAME = "cartadmin_fcm_prefs"
    private const val KEY_FCM_TOKEN = "cached_fcm_token"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getCachedToken(context: Context): String? {
        return getPrefs(context).getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Asynchronously retrieves the current FCM token from Firebase
     */
    suspend fun fetchCurrentToken(context: Context): String = withContext(Dispatchers.IO) {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                val token = FirebaseMessaging.getInstance().token.await()
                saveToken(context, token)
                token
            } else {
                getCachedToken(context) ?: "fcm_token_device_preview_active"
            }
        } catch (_: Throwable) {
            getCachedToken(context) ?: "fcm_token_device_preview_active"
        }
    }
}
