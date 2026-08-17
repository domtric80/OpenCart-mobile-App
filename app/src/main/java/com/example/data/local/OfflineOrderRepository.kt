package com.example.data.local

import com.example.data.local.dao.OrderDao
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.OrderItemEntity
import com.example.data.local.entity.toEntity
import com.example.model.Order
import com.example.model.OrderDetail
import com.example.model.OrderItem
import com.example.model.OrderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository providing clean offline-first cached access to OpenCart orders, line items, and details.
 */
class OfflineOrderRepository(private val orderDao: OrderDao) {

    /**
     * Reactive stream of all cached orders mapped to domain models.
     */
    val cachedOrders: Flow<List<Order>> = orderDao.getAllOrdersFlow().map { entities ->
        entities.map { it.toDomainModel() }
    }

    /**
     * Reactive stream of orders for a given store ID.
     */
    fun getCachedOrdersForStore(storeId: String): Flow<List<Order>> {
        return orderDao.getOrdersByStoreFlow(storeId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Observes full order details (items, addresses, totals) from Room cache.
     */
    fun getOrderDetailFlow(orderId: String): Flow<OrderDetail?> {
        return orderDao.getOrderWithItemsFlow(orderId).map { withItems ->
            withItems?.toOrderDetail()
        }
    }

    /**
     * Fetches full order details directly from Room cache.
     */
    suspend fun getOrderDetail(orderId: String): OrderDetail? {
        return orderDao.getOrderWithItems(orderId)?.toOrderDetail()
    }

    /**
     * Caches or updates a single order locally.
     */
    suspend fun cacheOrder(order: Order, storeId: String = "store_1") {
        orderDao.insertOrder(order.toEntity(storeId))
    }

    /**
     * Caches a full order with its detailed line items into Room.
     */
    suspend fun cacheFullOrderDetail(detail: OrderDetail, storeId: String = "store_1") {
        val orderEntity = OrderEntity(
            id = detail.order.id,
            storeId = storeId,
            orderNumber = detail.order.orderNumber,
            customerName = detail.order.customerName,
            customerEmail = detail.order.customerEmail,
            customerPhone = detail.customerPhone,
            total = detail.order.total,
            subtotal = detail.subtotal,
            shippingCost = detail.shippingCost,
            taxAmount = detail.taxAmount,
            discountAmount = detail.discountAmount,
            grandTotal = detail.grandTotal,
            status = detail.order.status.name,
            dateAdded = detail.order.dateAdded,
            itemsCount = if (detail.items.isNotEmpty()) detail.items.sumOf { it.quantity } else detail.order.itemsCount,
            shippingMethod = detail.order.shippingMethod,
            paymentMethod = detail.order.paymentMethod,
            shippingAddress = detail.shippingAddress,
            paymentAddress = detail.paymentAddress,
            customerNotes = detail.customerNotes,
            cachedAtTimestamp = System.currentTimeMillis()
        )

        val itemEntities = detail.items.map { item ->
            OrderItemEntity(
                id = item.id,
                orderId = detail.order.id,
                productId = item.productId,
                name = item.name,
                model = item.model,
                quantity = item.quantity,
                price = item.price,
                total = item.total
            )
        }

        orderDao.insertFullOrder(orderEntity, itemEntities)
    }

    /**
     * Caches a batch of recent orders locally.
     */
    suspend fun cacheRecentOrders(orders: List<Order>, storeId: String = "store_1") {
        val entities = orders.map { it.toEntity(storeId) }
        orderDao.insertOrders(entities)
    }

    /**
     * Atomically refreshes the offline cache for a store.
     */
    suspend fun refreshStoreCache(storeId: String, orders: List<Order>) {
        val entities = orders.map { it.toEntity(storeId) }
        orderDao.refreshStoreOrders(storeId, entities)
    }

    /**
     * Updates an order status in the local cache.
     */
    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        orderDao.updateOrderStatus(orderId, newStatus.name)
    }

    /**
     * Updates order notes in the local cache.
     */
    suspend fun updateOrderNotes(orderId: String, notes: String) {
        orderDao.updateOrderNotes(orderId, notes)
    }

    /**
     * Updates both status and notes in the local cache.
     */
    suspend fun updateOrderStatusAndNotes(orderId: String, newStatus: OrderStatus, notes: String) {
        orderDao.updateOrderStatusAndNotes(orderId, newStatus.name, notes)
    }

    /**
     * Deletes an order from the cache.
     */
    suspend fun removeOrder(orderId: String) {
        orderDao.deleteOrderById(orderId)
    }

    /**
     * Clears all cached orders.
     */
    suspend fun clearAllCache() {
        orderDao.clearAllOrders()
        orderDao.clearAllOrderItems()
    }
}
