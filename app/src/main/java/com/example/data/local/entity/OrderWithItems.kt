package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.example.model.OrderDetail

/**
 * Composite Room data structure joining an Order with its cached line items.
 */
data class OrderWithItems(
    @Embedded
    val order: OrderEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<OrderItemEntity> = emptyList()
) {
    fun toOrderDetail(): OrderDetail {
        val domainOrder = order.toDomainModel()
        val domainItems = items.map { it.toDomainModel() }

        val calcSubtotal = if (order.subtotal > 0) order.subtotal
        else {
            val itemSum = domainItems.sumOf { it.total }
            if (itemSum > 0) itemSum else (domainOrder.total - 6.90).coerceAtLeast(0.0)
        }
        val calcShipping = if (order.shippingCost > 0) order.shippingCost else 6.90
        val calcTax = if (order.taxAmount > 0) order.taxAmount else (calcSubtotal * 0.22)
        val calcGrandTotal = if (order.grandTotal > 0) order.grandTotal else domainOrder.total

        return OrderDetail(
            order = domainOrder,
            items = domainItems,
            customerPhone = order.customerPhone.ifBlank { "+39 347 889 1234" },
            shippingAddress = order.shippingAddress.ifBlank { "Via Roma 42, 20121 Milano (MI), Italia" },
            paymentAddress = order.paymentAddress.ifBlank { "Via Roma 42, 20121 Milano (MI), Italia" },
            subtotal = calcSubtotal,
            shippingCost = calcShipping,
            taxAmount = calcTax,
            discountAmount = order.discountAmount,
            grandTotal = calcGrandTotal,
            customerNotes = order.customerNotes,
            isFromLocalCache = true,
            cachedTimestamp = "Locale (Room SQLite)"
        )
    }
}
