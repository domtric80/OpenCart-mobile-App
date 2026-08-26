package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Subscription
import com.example.model.SubscriptionStatus

@Entity(tableName = "subscriptions_cache")
data class SubscriptionEntity(
    @PrimaryKey
    val id: String,
    val storeId: String = "store_1",
    val subscriptionId: String,
    val customerName: String,
    val customerEmail: String,
    val planName: String,
    val cycleFrequency: String = "Mensile (30 gg)",
    val amount: Double,
    val status: String = "ACTIVE",
    val nextPaymentDate: String,
    val startDate: String,
    val paymentMethod: String = "Stripe / Carta Ricorrente",
    val failureCount: Int = 0,
    val cachedAtTimestamp: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): Subscription {
        val mappedStatus = try {
            SubscriptionStatus.valueOf(status.uppercase())
        } catch (_: Exception) {
            SubscriptionStatus.ACTIVE
        }

        return Subscription(
            id = id,
            subscriptionId = subscriptionId,
            customerName = customerName,
            customerEmail = customerEmail,
            planName = planName,
            cycleFrequency = cycleFrequency,
            amount = amount,
            status = mappedStatus,
            nextPaymentDate = nextPaymentDate,
            startDate = startDate,
            paymentMethod = paymentMethod,
            failureCount = failureCount
        )
    }
}

fun Subscription.toEntity(storeId: String = "store_1"): SubscriptionEntity {
    return SubscriptionEntity(
        id = id,
        storeId = storeId,
        subscriptionId = subscriptionId,
        customerName = customerName,
        customerEmail = customerEmail,
        planName = planName,
        cycleFrequency = cycleFrequency,
        amount = amount,
        status = status.name,
        nextPaymentDate = nextPaymentDate,
        startDate = startDate,
        paymentMethod = paymentMethod,
        failureCount = failureCount
    )
}
