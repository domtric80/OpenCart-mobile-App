package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Order
import com.example.model.OrderStatus

/**
 * Room Database entity representing a cached OpenCart order for offline access.
 */
@Entity(tableName = "orders_cache")
data class OrderEntity(
    @PrimaryKey
    val id: String,
    val storeId: String = "store_1",
    val orderNumber: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String = "+39 347 889 1234",
    val total: Double,
    val subtotal: Double = 0.0,
    val shippingCost: Double = 6.90,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val status: String,
    val dateAdded: String,
    val itemsCount: Int,
    val shippingMethod: String = "Corriere Espresso (GLS)",
    val paymentMethod: String = "Carta di Credito / Stripe",
    val shippingAddress: String = "Via Roma 42, 20121 Milano (MI), Italia",
    val paymentAddress: String = "Via Roma 42, 20121 Milano (MI), Italia",
    val customerNotes: String? = null,
    val currencyCode: String = "EUR",
    val currencySymbol: String = "€",
    val cachedAtTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Converts this Room entity to the domain Order model used by the UI layer.
     */
    fun toDomainModel(): Order {
        val mappedStatus = try {
            OrderStatus.valueOf(status.uppercase())
        } catch (_: Exception) {
            OrderStatus.entries.find { it.label.equals(status, ignoreCase = true) } ?: OrderStatus.CONFIRMED
        }

        return Order(
            id = id,
            orderNumber = orderNumber,
            customerName = customerName,
            customerEmail = customerEmail,
            total = if (grandTotal > 0) grandTotal else total,
            status = mappedStatus,
            dateAdded = dateAdded,
            itemsCount = itemsCount,
            shippingMethod = shippingMethod,
            paymentMethod = paymentMethod,
            notes = customerNotes
        )
    }
}

/**
 * Extension function to convert a domain Order model to a cached Room entity.
 */
fun Order.toEntity(
    storeId: String = "store_1",
    phone: String = "+39 347 889 1234",
    shippingAddr: String = "Via Roma 42, 20121 Milano (MI), Italia",
    paymentAddr: String = "Via Roma 42, 20121 Milano (MI), Italia",
    notes: String? = this.notes
): OrderEntity {
    val sub = (total - 6.90).coerceAtLeast(0.0)
    return OrderEntity(
        id = id,
        storeId = storeId,
        orderNumber = orderNumber,
        customerName = customerName,
        customerEmail = customerEmail,
        customerPhone = phone,
        total = total,
        subtotal = sub,
        shippingCost = 6.90,
        taxAmount = sub * 0.22,
        discountAmount = 0.0,
        grandTotal = total,
        status = status.name,
        dateAdded = dateAdded,
        itemsCount = itemsCount,
        shippingMethod = shippingMethod,
        paymentMethod = paymentMethod,
        shippingAddress = shippingAddr,
        paymentAddress = paymentAddr,
        customerNotes = notes,
        cachedAtTimestamp = System.currentTimeMillis()
    )
}
