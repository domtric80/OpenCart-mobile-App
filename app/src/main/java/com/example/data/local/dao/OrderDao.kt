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

    /**
     * Observes all cached orders ordered by newest cache entry.
     */
    @Query("SELECT * FROM orders_cache ORDER BY cachedAtTimestamp DESC")
    fun getAllOrdersFlow(): Flow<List<OrderEntity>>

    /**
     * Observes cached orders for a specific store.
     */
    @Query("SELECT * FROM orders_cache WHERE storeId = :storeId ORDER BY cachedAtTimestamp DESC")
    fun getOrdersByStoreFlow(storeId: String): Flow<List<OrderEntity>>

    /**
     * Observes a single order's details by ID.
     */
    @Query("SELECT * FROM orders_cache WHERE id = :orderId LIMIT 1")
    fun getOrderByIdFlow(orderId: String): Flow<OrderEntity?>

    /**
     * Fetches a single order's details by ID directly.
     */
    @Query("SELECT * FROM orders_cache WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    /**
     * Observes a full order with its items for detailed view.
     */
    @Transaction
    @Query("SELECT * FROM orders_cache WHERE id = :orderId LIMIT 1")
    fun getOrderWithItemsFlow(orderId: String): Flow<OrderWithItems?>

    /**
     * Fetches a full order with its items synchronously.
     */
    @Transaction
    @Query("SELECT * FROM orders_cache WHERE id = :orderId LIMIT 1")
    suspend fun getOrderWithItems(orderId: String): OrderWithItems?

    /**
     * Observes cached orders filtered by status.
     */
    @Query("SELECT * FROM orders_cache WHERE status = :status ORDER BY cachedAtTimestamp DESC")
    fun getOrdersByStatusFlow(status: String): Flow<List<OrderEntity>>

    /**
     * Inserts or replaces a single cached order.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    /**
     * Inserts or replaces a list of cached orders.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    /**
     * Inserts line items for orders.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    /**
     * Inserts full order and its items in a transaction.
     */
    @Transaction
    suspend fun insertFullOrder(order: OrderEntity, items: List<OrderItemEntity>) {
        insertOrder(order)
        deleteItemsForOrder(order.id)
        if (items.isNotEmpty()) {
            insertOrderItems(items)
        }
    }

    /**
     * Updates the status of an existing cached order.
     */
    @Query("UPDATE orders_cache SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)

    /**
     * Updates the notes of an existing cached order.
     */
    @Query("UPDATE orders_cache SET customerNotes = :notes WHERE id = :orderId")
    suspend fun updateOrderNotes(orderId: String, notes: String)

    /**
     * Updates both status and notes of an existing cached order.
     */
    @Query("UPDATE orders_cache SET status = :status, customerNotes = :notes WHERE id = :orderId")
    suspend fun updateOrderStatusAndNotes(orderId: String, status: String, notes: String)

    /**
     * Deletes a cached order by ID.
     */
    @Query("DELETE FROM orders_cache WHERE id = :orderId")
    suspend fun deleteOrderById(orderId: String)

    /**
     * Deletes items for a specific order.
     */
    @Query("DELETE FROM order_items_cache WHERE orderId = :orderId")
    suspend fun deleteItemsForOrder(orderId: String)

    /**
     * Clears all cached orders for a specific store.
     */
    @Query("DELETE FROM orders_cache WHERE storeId = :storeId")
    suspend fun clearOrdersForStore(storeId: String)

    /**
     * Clears the entire offline orders cache.
     */
    @Query("DELETE FROM orders_cache")
    suspend fun clearAllOrders()

    /**
     * Clears all order items.
     */
    @Query("DELETE FROM order_items_cache")
    suspend fun clearAllOrderItems()

    /**
     * Gets the total number of cached orders.
     */
    @Query("SELECT COUNT(*) FROM orders_cache")
    fun getOrderCountFlow(): Flow<Int>

    /**
     * Gets synchronous count of cached orders.
     */
    @Query("SELECT COUNT(*) FROM orders_cache")
    suspend fun getOrderCount(): Int

    /**
     * Replaces all orders for a store in a single atomic transaction.
     */
    @Transaction
    suspend fun refreshStoreOrders(storeId: String, orders: List<OrderEntity>) {
        clearOrdersForStore(storeId)
        insertOrders(orders)
    }
}
