package com.example

import android.app.Application
import android.util.Log
import com.example.notification.NotificationHelper
import com.example.network.BridgeDeviceIdentity
import com.google.firebase.FirebaseApp

class CartAdminApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        runCatching { BridgeDeviceIdentity.initialize() }
            .onFailure { Log.e(TAG, "Hardware device identity unavailable") }
        
        // 1. Initialize notification channels safely
        try {
            NotificationHelper.createNotificationChannels(this)
        } catch (e: Exception) {
            Log.w(TAG, "Notification channel init skipped: ${e.message}")
        }

        // Firebase deve essere inizializzato soltanto dalla configurazione firmata dell'app.
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseApp init handled: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "CartAdminApplication"
    }
}
