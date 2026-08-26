package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions_cache ORDER BY cachedAtTimestamp DESC")
    fun getAllSubscriptionsFlow(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions_cache ORDER BY cachedAtTimestamp DESC")
    suspend fun getAllSubscriptions(): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions_cache WHERE storeId = :storeId ORDER BY cachedAtTimestamp DESC")
    fun getSubscriptionsByStoreFlow(storeId: String): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions_cache WHERE id = :id LIMIT 1")
    suspend fun getSubscriptionById(id: String): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: SubscriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptions(subs: List<SubscriptionEntity>)

    @Query("UPDATE subscriptions_cache SET status = :status WHERE id = :id")
    suspend fun updateSubscriptionStatus(id: String, status: String)

    @Query("DELETE FROM subscriptions_cache WHERE id = :id")
    suspend fun deleteSubscriptionById(id: String)

    @Query("DELETE FROM subscriptions_cache WHERE storeId = :storeId")
    suspend fun clearSubscriptionsForStore(storeId: String)

    @Query("DELETE FROM subscriptions_cache")
    suspend fun clearAllSubscriptions()

    @Query("SELECT COUNT(*) FROM subscriptions_cache")
    suspend fun getSubscriptionCount(): Int
}
