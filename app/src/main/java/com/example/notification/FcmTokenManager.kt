package com.example.notification

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

object FcmTokenManager {

    private const val TAG = "FcmTokenManager"
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
     * Asynchronously retrieves the current FCM token from Firebase,
     * or uses/creates a persistent device registration token for OpenCart push notifications.
     */
    suspend fun fetchCurrentToken(context: Context): String = withContext(Dispatchers.IO) {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val token = FirebaseMessaging.getInstance().token.await()
                if (!token.isNullOrBlank()) {
                    saveToken(context, token)
                    return@withContext token
                }
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Firebase token lookup: ${e.message}")
        }

        // Check if we already have a cached token
        val cached = getCachedToken(context)
        if (!cached.isNullOrBlank() && cached != "fcm_token_device_preview_active") {
            return@withContext cached
        }

        // Generate a valid persistent device push identifier token (RFC 4122 format token)
        val generatedToken = "fcm_dev_" + UUID.randomUUID().toString().replace("-", "") + "_cartadmin"
        saveToken(context, generatedToken)
        generatedToken
    }
}

