package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.OrderReturnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderReturnDao {

    @Query("SELECT * FROM returns_cache ORDER BY cachedAtTimestamp DESC")
    fun getAllReturnsFlow(): Flow<List<OrderReturnEntity>>

    @Query("SELECT * FROM returns_cache ORDER BY cachedAtTimestamp DESC")
    suspend fun getAllReturns(): List<OrderReturnEntity>

    @Query("SELECT * FROM returns_cache WHERE storeId = :storeId ORDER BY cachedAtTimestamp DESC")
    fun getReturnsByStoreFlow(storeId: String): Flow<List<OrderReturnEntity>>

    @Query("SELECT * FROM returns_cache WHERE id = :id LIMIT 1")
    suspend fun getReturnById(id: String): OrderReturnEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(orderReturn: OrderReturnEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturns(returns: List<OrderReturnEntity>)

    @Query("UPDATE returns_cache SET status = :status, action = :action WHERE id = :id")
    suspend fun updateReturnStatus(id: String, status: String, action: String)

    @Query("DELETE FROM returns_cache WHERE id = :id")
    suspend fun deleteReturnById(id: String)

    @Query("DELETE FROM returns_cache WHERE storeId = :storeId")
    suspend fun clearReturnsForStore(storeId: String)

    @Query("DELETE FROM returns_cache")
    suspend fun clearAllReturns()

    @Query("SELECT COUNT(*) FROM returns_cache")
    suspend fun getReturnCount(): Int
}
