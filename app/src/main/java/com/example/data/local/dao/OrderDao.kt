package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.OrderItemEntity
import com.example.data.local.entity.OrderWithItems
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for querying, inserting, and managing cached OpenCart orders and items.
 */
@Dao
interface OrderDao {

    @Query("SELECT * FROM orders_cache ORDER BY cachedAtTimestamp DESC")
    fun getAllOrdersFlow(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders_cache ORDER BY cachedAtTimestamp DESC")
    suspend fun getAllOrders(): List<OrderEntity>

    @Query("SELECT * FROM orders_cache WHERE storeId = :storeId ORDER BY cachedAtTimestamp DESC")
    fun getOrdersByStoreFlow(storeId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders_cache WHERE id = :orderId LIMIT 1")
    fun getOrderByIdFlow(orderId: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders_cache WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Transaction
    @Query("SELECT * FROM orders_cache WHERE id = :orderId LIMIT 1")
    fun getOrderWithItemsFlow(orderId: String): Flow<OrderWithItems?>

    @Transaction
    @Query("SELECT * FROM orders_cache WHERE id = :orderId LIMIT 1")
    suspend fun getOrderWithItems(orderId: String): OrderWithItems?

    @Query("SELECT * FROM orders_cache WHERE status = :status ORDER BY cachedAtTimestamp DESC")
    fun getOrdersByStatusFlow(status: String): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Transaction
    suspend fun insertFullOrder(order: OrderEntity, items: List<OrderItemEntity>) {
        insertOrder(order)
        deleteItemsForOrder(order.id)
        if (items.isNotEmpty()) {
            insertOrderItems(items)
        }
    }

    @Query("UPDATE orders_cache SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)

    @Query("UPDATE orders_cache SET customerNotes = :notes WHERE id = :orderId")
    suspend fun updateOrderNotes(orderId: String, notes: String)

    @Query("UPDATE orders_cache SET status = :status, customerNotes = :notes WHERE id = :orderId")
    suspend fun updateOrderStatusAndNotes(orderId: String, status: String, notes: String)

    @Query("DELETE FROM orders_cache WHERE id = :orderId")
    suspend fun deleteOrderById(orderId: String)

    @Query("DELETE FROM order_items_cache WHERE orderId = :orderId")
    suspend fun deleteItemsForOrder(orderId: String)

    @Query("DELETE FROM orders_cache WHERE storeId = :storeId")
    suspend fun clearOrdersForStore(storeId: String)

    @Query("DELETE FROM orders_cache")
    suspend fun clearAllOrders()

    @Query("DELETE FROM order_items_cache")
    suspend fun clearAllOrderItems()

    @Query("SELECT COUNT(*) FROM orders_cache")
    fun getOrderCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM orders_cache")
    suspend fun getOrderCount(): Int

    @Transaction
    suspend fun refreshStoreOrders(storeId: String, orders: List<OrderEntity>) {
        clearOrdersForStore(storeId)
        insertOrders(orders)
    }
}
