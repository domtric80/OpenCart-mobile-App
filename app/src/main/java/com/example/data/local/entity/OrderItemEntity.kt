package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.OrderItem

/**
 * Room Database entity representing an item line of a cached order.
 */
@Entity(
    tableName = "order_items_cache",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"])]
)
data class OrderItemEntity(
    @PrimaryKey
    val id: String,
    val orderId: String,
    val productId: String,
    val name: String,
    val model: String,
    val quantity: Int,
    val price: Double,
    val total: Double
) {
    fun toDomainModel(): OrderItem {
        return OrderItem(
            id = id,
            orderId = orderId,
            productId = productId,
            name = name,
            model = model,
            quantity = quantity,
            price = price,
            total = total
        )
    }
}

fun OrderItem.toEntity(): OrderItemEntity {
    return OrderItemEntity(
        id = id,
        orderId = orderId,
        productId = productId,
        name = name,
        model = model,
        quantity = quantity,
        price = price,
        total = total
    )
}
