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
            }

            val stockChannel = NotificationChannel(
                CHANNEL_STOCK_ID,
                "Allarmi Magazzino & Sottoscorta",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avvisi quando i prodotti scendono sotto la soglia minima"
                enableVibration(true)
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
        val intent = Intent(context, MainActivity::class.java).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (action != null) {
                this.action = action
            }
            configureExtras?.invoke(this)
        }

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
        context: Context,
        orderNumber: String,
        customerName: String,
        total: Double
    ): Boolean {
        createNotificationChannels(context)

        val pendingIntent = createExplicitMainActivityPendingIntent(
            context = context,
            requestCode = NOTIFICATION_ID_ORDER,
            action = "com.example.cartadmin.ACTION_ORDER_NOTIFICATION"
        ) {
            putExtra("notification_order_number", orderNumber)
        }

        val formattedTotal = String.format("€%.2f", total)
        val notification = NotificationCompat.Builder(context, CHANNEL_ORDERS_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("🛒 Nuovo Ordine $orderNumber: $formattedTotal")
            .setContentText("Cliente: $customerName • Tocca per visualizzare")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Nuovo ordine ricevuto su OpenCart da $customerName per un totale di $formattedTotal. Tocca per gestire la spedizione.")
            )
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
        context: Context,
        productName: String,
        remainingQuantity: Int
    ): Boolean {
        createNotificationChannels(context)

        val pendingIntent = createExplicitMainActivityPendingIntent(
            context = context,
            requestCode = NOTIFICATION_ID_STOCK,
            action = "com.example.cartadmin.ACTION_STOCK_NOTIFICATION"
        ) {
            putExtra("notification_product_name", productName)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_STOCK_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("⚠️ Allarme Sottoscorta: $productName")
            .setContentText("Rimaste solo $remainingQuantity unità a magazzino.")
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
