package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.OrderReturn
import com.example.model.ReturnStatus

@Entity(tableName = "returns_cache")
data class OrderReturnEntity(
    @PrimaryKey
    val id: String,
    val storeId: String = "store_1",
    val returnId: String,
    val orderId: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String = "",
    val productName: String,
    val productModel: String,
    val quantity: Int = 1,
    val reason: String,
    val opened: Boolean = true,
    val status: String = "PENDING",
    val action: String = "In attesa di verifica",
    val dateAdded: String,
    val comment: String = "",
    val cachedAtTimestamp: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): OrderReturn {
        val mappedStatus = try {
            ReturnStatus.valueOf(status.uppercase())
        } catch (_: Exception) {
            ReturnStatus.PENDING
        }

        return OrderReturn(
            id = id,
            returnId = returnId,
            orderId = orderId,
            customerName = customerName,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            productName = productName,
            productModel = productModel,
            quantity = quantity,
            reason = reason,
            opened = opened,
            status = mappedStatus,
            action = action,
            dateAdded = dateAdded,
            comment = comment
        )
    }
}

fun OrderReturn.toEntity(storeId: String = "store_1"): OrderReturnEntity {
    return OrderReturnEntity(
        id = id,
        storeId = storeId,
        returnId = returnId,
        orderId = orderId,
        customerName = customerName,
        customerEmail = customerEmail,
        customerPhone = customerPhone,
        productName = productName,
        productModel = productModel,
        quantity = quantity,
        reason = reason,
        opened = opened,
        status = status.name,
        action = action,
        dateAdded = dateAdded,
        comment = comment
    )
}
