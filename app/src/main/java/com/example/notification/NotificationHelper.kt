package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_ORDERS_ID = "opencart_orders_channel"
    const val CHANNEL_STOCK_ID = "opencart_stock_channel"

    const val NOTIFICATION_ID_TEST = 1001
    const val NOTIFICATION_ID_ORDER = 1002
    const val NOTIFICATION_ID_STOCK = 1003

    /**
     * Initializes notification channels for Android 8.0 (API 26) and higher
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val ordersChannel = NotificationChannel(
                CHANNEL_ORDERS_ID,
                "Nuovi Ordini OpenCart",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche immediate per nuovi ordini ricevuti su OpenCart"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
            }

            val stockChannel = NotificationChannel(
                CHANNEL_STOCK_ID,
                "Allarmi Magazzino & Sottoscorta",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avvisi quando i prodotti scendono sotto la soglia minima"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
            }

            notificationManager.createNotificationChannel(ordersChannel)
            notificationManager.createNotificationChannel(stockChannel)
        }
    }

    /**
     * Creates an explicitly targeted PendingIntent for MainActivity
     */
    internal fun createExplicitMainActivityPendingIntent(
        context: Context,
        requestCode: Int,
        action: String? = null,
        configureExtras: (Intent.() -> Unit)? = null
    ): PendingIntent {
        val intent = Intent()
        intent.setClassName(context.packageName, MainActivity::class.java.name)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (action != null) {
            intent.action = action
        }
        configureExtras?.invoke(intent)

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    /**
     * Sends a test notification to verify delivery on the user's phone
     */
    fun sendTestNotification(context: Context): Boolean {
        createNotificationChannels(context)

        val pendingIntent = createExplicitMainActivityPendingIntent(
            context = context,
            requestCode = NOTIFICATION_ID_TEST,
            action = "com.example.cartadmin.ACTION_TEST_NOTIFICATION"
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ORDERS_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("🛍️ CartAdmin: Notifiche Attive!")
            .setContentText("Il canale di notifica OpenCart è configurato e pronto a ricevere avvisi.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("✅ Il tuo smartphone riceverà notifiche istantanee ogni volta che un cliente effettua un ordine o quando un prodotto va sottoscorta.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Sends a notification when a new order is received
     */
    fun sendNewOrderNotification(
        context: Context
    ): Boolean {
        createNotificationChannels(context)

        val pendingIntent = createExplicitMainActivityPendingIntent(
            context = context,
            requestCode = NOTIFICATION_ID_ORDER,
            action = "com.example.cartadmin.ACTION_ORDER_NOTIFICATION"
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ORDERS_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Nuovo evento ordine")
            .setContentText("Sblocca CartAdmin per visualizzare i dettagli.")
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ORDER, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Sends a notification when a product is low in stock
     */
    fun sendLowStockNotification(
        context: Context
    ): Boolean {
        createNotificationChannels(context)

        val pendingIntent = createExplicitMainActivityPendingIntent(
            context = context,
            requestCode = NOTIFICATION_ID_STOCK,
            action = "com.example.cartadmin.ACTION_STOCK_NOTIFICATION"
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_STOCK_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Allarme magazzino")
            .setContentText("Sblocca CartAdmin per visualizzare i dettagli.")
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_STOCK, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
