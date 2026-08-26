package com.example.notification

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.entity.OrderEntity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CartAdminFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Push token refreshed successfully")
        }
        FcmTokenManager.saveToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Push message event received")
        }

        val data = remoteMessage.data
        val notificationType = data["type"] ?: "new_order"

        when (notificationType) {
            "new_order", "order" -> {
                val orderId = data["order_id"] ?: "ord_${(1000..9999).random()}"
                val orderNumber = data["order_number"] ?: data["order_id"]?.let { "#$it" } ?: "#${(1000..9999).random()}"
                val customerName = data["customer_name"] ?: data["customer"] ?: "Cliente OpenCart"
                val customerEmail = data["customer_email"] ?: data["email"] ?: "cliente@email.com"
                val totalStr = data["total"] ?: "0.00"
                val total = totalStr.replace("€", "").replace(",", ".").trim().toDoubleOrNull() ?: 0.0
                val dateAdded = data["date_added"] ?: "Adesso"
                val itemsCount = data["items_count"]?.toIntOrNull() ?: 1

                NotificationHelper.sendNewOrderNotification(
                    context = applicationContext,
                    orderNumber = orderNumber,
                    customerName = customerName,
                    total = total
                )

                // Cache order in Room database for offline access
                serviceScope.launch {
                    try {
                        val db = AppDatabase.getDatabase(applicationContext)
                        val orderEntity = OrderEntity(
                            id = orderId,
                            storeId = data["store_id"] ?: "store_1",
                            orderNumber = orderNumber,
                            customerName = customerName,
                            customerEmail = customerEmail,
                            total = total,
                            status = data["status"] ?: "CONFIRMED",
                            dateAdded = dateAdded,
                            itemsCount = itemsCount,
                            shippingMethod = data["shipping_method"] ?: "Corriere Espresso (GLS)",
                            paymentMethod = data["payment_method"] ?: "Carta di Credito / Stripe"
                        )
                        db.orderDao().insertOrder(orderEntity)
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, "Failed to cache order to Room")
                        }
                    }
                }
            }

            "stock_alert", "low_stock" -> {
                val productName = data["product_name"] ?: data["name"] ?: "Prodotto OpenCart"
                val remainingQty = data["quantity"]?.toIntOrNull() ?: data["stock"]?.toIntOrNull() ?: 1

                NotificationHelper.sendLowStockNotification(
                    context = applicationContext,
                    productName = productName,
                    remainingQuantity = remainingQty
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
