package com.example.notification

import android.util.Log
import com.example.BuildConfig
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CartAdminFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Push message event received")
        }

        val data = remoteMessage.data
        val notificationType = data["type"] ?: "new_order"

        when (notificationType) {
            "new_order", "order" -> {
                NotificationHelper.sendNewOrderNotification(
                    context = applicationContext
                )
            }

            "stock_alert", "low_stock" -> {
                NotificationHelper.sendLowStockNotification(
                    context = applicationContext
                )
            }

            else -> {
                NotificationHelper.sendTestNotification(applicationContext)
            }
        }
    }

    companion object {
        private const val TAG = "CartAdminFCM"
    }
}
