package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.notification.NotificationHelper
import com.example.network.BridgeDeviceIdentity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class CartAdminApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        BridgeDeviceIdentity.initialize(this)
        
        // 1. Initialize notification channels safely
        try {
            NotificationHelper.createNotificationChannels(this)
        } catch (e: Exception) {
            Log.w(TAG, "Notification channel init skipped: ${e.message}")
        }

        // 2. Safely initialize Firebase if google-services.json was not provided
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:1029384756:android:cartadmin")
                    .setApiKey("AIzaSyFakeKeyForLocalInitializationOnly12345")
                    .setProjectId("cartadmin-opencart")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d(TAG, "FirebaseApp initialized with fallback credentials")
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseApp init handled: ${e.message}")
        }

        // 3. Pre-warm database instance safely
        try {
            AppDatabase.getDatabase(this)
        } catch (e: Exception) {
            Log.e(TAG, "Database pre-warm error: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "CartAdminApplication"
    }
}
