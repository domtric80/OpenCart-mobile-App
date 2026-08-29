package com.example.data

import com.example.data.local.dao.StoreProfileDao
import com.example.data.local.entity.StoreProfileEntity
import com.example.model.ActivityItem
import com.example.model.ActivityType
import com.example.model.Category
import com.example.model.Order
import com.example.model.OrderDetail
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.Store
import com.example.model.VisitorRealtimeStats
import com.example.network.OpenCartApiClient
import com.example.network.OpenCartConnectionResult
import com.example.security.CredentialField
import com.example.security.CredentialProtector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EcomRepository(
    private val storeProfileDao: StoreProfileDao? = null,
    private val apiClient: OpenCartApiClient,
    private val credentialProtector: CredentialProtector
) {

    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    private val _currentStoreId = MutableStateFlow<String>("")
    val currentStoreId: StateFlow<String> = _currentStoreId.asStateFlow()

    private val _activities = MutableStateFlow<List<ActivityItem>>(emptyList())
    val activities: StateFlow<List<ActivityItem>> = _activities.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<com.example.model.Subscription>>(emptyList())
    val subscriptions: StateFlow<List<com.example.model.Subscription>> = _subscriptions.asStateFlow()

    private val _returns = MutableStateFlow<List<com.example.model.OrderReturn>>(emptyList())
    val returns: StateFlow<List<com.example.model.OrderReturn>> = _returns.asStateFlow()

    private val _visitorStats = MutableStateFlow(
        VisitorRealtimeStats(
            trackingEnabled = false,
            dataAvailable = false,
            activeVisitorsNow = 0,
            pageViewsPerMin = 0,
            activeCartsCount = 0,
            activeCheckoutsCount = 0,
            avgDurationSeconds = 0,
            bounceRate = 0.0,
            trafficHistory = emptyList(),
            topPages = emptyList(),
            topCountries = emptyList(),
            trafficSources = emptyList(),
            deviceStats = emptyList(),
            liveEvents = emptyList()
        )
    )
    val visitorStats: StateFlow<VisitorRealtimeStats> = _visitorStats.asStateFlow()

    fun setVisitorStats(stats: VisitorRealtimeStats) {
        _visitorStats.value = stats
    }

    suspend fun loadPersistedStores(): Boolean {
        val dao = storeProfileDao ?: return false
        val entities = dao.getAllStores()
        var credentialsMigrated = false
        if (entities.isNotEmpty()) {
            val revealedStores = entities.map { entity ->
                val username = credentialProtector.reveal(
                    entity.id,
                    CredentialField.API_USERNAME,
                    entity.adminUsername
                )
                val apiKey = credentialProtector.reveal(
                    entity.id,
                    CredentialField.API_KEY,
                    entity.apiKey
                )

                if (username.requiresMigration || apiKey.requiresMigration) {
                    dao.updateProtectedCredentials(
                        storeId = entity.id,
                        protectedUsername = credentialProtector.protect(
                            entity.id,
                            CredentialField.API_USERNAME,
                            username.value
                        ),
                        protectedApiKey = credentialProtector.protect(
                            entity.id,
                            CredentialField.API_KEY,
                            apiKey.value
                        )
                    )
                    credentialsMigrated = true
                }

                Triple(entity, username.value, apiKey.value)
            }
            val domainStores = revealedStores.map { (entity, username, apiKey) ->
                Store(
                    id = entity.id,
                    name = entity.name,
                    url = entity.url,
                    version = entity.openCartVersion,
                    apiUsername = username,
                    apiKey = apiKey,
                    todayRevenue = 0.0,
                    revenueGrowthPercent = 0.0,
                    pendingOrdersCount = 0,
                    stockAlertsCount = 0,
                    lastSyncTime = if (entity.lastSyncTimestamp > 0) "Ultima sinc: ${java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.ITALIAN).format(java.util.Date(entity.lastSyncTimestamp))}" else "Nessuna sincronizzazione"
                )
            }
            _stores.value = domainStores
            val primary = entities.find { it.isPrimary } ?: entities.first()
            _currentStoreId.value = primary.id
        } else {
            _stores.value = emptyList()
            _currentStoreId.value = ""
        }
        return credentialsMigrated
    }

    suspend fun testStoreConnection(url: String, username: String, key: String): OpenCartConnectionResult {
        return apiClient.testConnection(url, username, key)
    }

    fun selectStore(storeId: String) {
        _currentStoreId.value = storeId
    }

    suspend fun addStore(name: String, url: String, version: String, username: String, key: String): Store {
        val newId = "store_${System.currentTimeMillis()}"
        val newStore = Store(
            id = newId,
            name = name.ifBlank { "Mio Negozio OpenCart" },
            url = url,
            version = version.ifBlank { "OpenCart 3.x/4.x" },
            apiUsername = username,
            apiKey = key,
            todayRevenue = 0.0,
            revenueGrowthPercent = 0.0,
            pendingOrdersCount = 0,
            stockAlertsCount = 0,
            lastSyncTime = "Proprio adesso"
        )

        storeProfileDao?.insertOrUpdate(
            StoreProfileEntity(
                id = newId,
                name = newStore.name,
                url = newStore.url,
                apiKey = credentialProtector.protect(newId, CredentialField.API_KEY, key),
                adminUsername = credentialProtector.protect(
                    newId,
                    CredentialField.API_USERNAME,
                    username
                ),
                isPrimary = _stores.value.isEmpty(),
                isActive = true,
                lastSyncTimestamp = System.currentTimeMillis(),
                openCartVersion = newStore.version
            )
        )

        _stores.update { it + newStore }
        _currentStoreId.value = newId

        return newStore
    }

    suspend fun updateStoreCredentials(
        storeId: String,
        name: String,
        url: String,
        username: String,
        key: String,
        version: String
    ) {
        val currentStore = _stores.value.find { it.id == storeId }
        val effectiveKey = key.ifBlank { currentStore?.apiKey.orEmpty() }
        val protectedApiKey = credentialProtector.protect(
            storeId,
            CredentialField.API_KEY,
            effectiveKey
        )
        val protectedUsername = credentialProtector.protect(
            storeId,
            CredentialField.API_USERNAME,
            username
        )

        storeProfileDao?.insertOrUpdate(
            StoreProfileEntity(
                id = storeId,
                name = name,
                url = url,
                apiKey = protectedApiKey,
                adminUsername = protectedUsername,
                isPrimary = true,
                isActive = true,
                lastSyncTimestamp = System.currentTimeMillis(),
                openCartVersion = version
            )
        )

        _stores.update { list ->
            list.map { s ->
                if (s.id == storeId) {
                    s.copy(
                        name = name,
                        url = url,
                        apiUsername = username,
                        apiKey = effectiveKey,
                        version = version,
                        lastSyncTime = "Proprio adesso"
                    )
                } else s
            }
        }

    }

    suspend fun deleteStore(storeId: String) {
        _stores.update { list -> list.filter { it.id != storeId } }
        if (_currentStoreId.value == storeId) {
            _currentStoreId.value = _stores.value.firstOrNull()?.id ?: ""
        }
        storeProfileDao?.deleteStoreById(storeId)
    }

    fun setLiveOrders(orders: List<Order>) {
        _orders.value = orders
        val totalRev = orders.sumOf { it.total }
        val pendingCount = orders.count { it.status == OrderStatus.PENDING || it.status == OrderStatus.PROCESSING }

        // Update active store stats
        val currentId = _currentStoreId.value
        _stores.update { list ->
            list.map { store ->
                if (store.id == currentId) {
                    store.copy(
                        todayRevenue = totalRev,
                        pendingOrdersCount = pendingCount,
                        lastSyncTime = "Sincronizzato adesso"
                    )
                } else store
            }
        }
    }

    fun setLiveProducts(products: List<Product>) {
        _products.value = products
        val stockAlerts = products.count { it.quantity <= it.minQuantityAlert }
        val currentId = _currentStoreId.value
        _stores.update { list ->
            list.map { store ->
                if (store.id == currentId) {
                    store.copy(stockAlertsCount = stockAlerts)
                } else store
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        _orders.update { current ->
            current.map { if (it.id == orderId) it.copy(status = newStatus) else it }
        }
    }

    fun updateOrderNotes(orderId: String, notes: String) {
        _orders.update { current ->
            current.map { if (it.id == orderId) it.copy(notes = notes) else it }
        }
    }

    fun updateOrderStatusAndNotes(orderId: String, newStatus: OrderStatus, notes: String) {
        _orders.update { current ->
            current.map { if (it.id == orderId) it.copy(status = newStatus, notes = notes) else it }
        }
    }

    fun updateProductStock(productId: String, newQuantity: Int) {
        _products.update { current ->
            current.map { if (it.id == productId) it.copy(quantity = newQuantity.coerceAtLeast(0)) else it }
        }
    }

    fun addProduct(
        name: String,
        model: String,
        sku: String,
        price: Double,
        specialPrice: Double?,
        quantity: Int,
        category: String,
        description: String
    ): Product {
        val newProd = Product(
            id = "prod_${System.currentTimeMillis()}",
            name = name,
            model = model,
            sku = sku,
            price = price,
            specialPrice = specialPrice,
            quantity = quantity,
            category = category,
            description = description,
            status = true
        )
        _products.update { listOf(newProd) + it }
        return newProd
    }

    fun updateProduct(product: Product) {
        _products.update { current ->
            current.map { if (it.id == product.id) product else it }
        }
    }

    fun deleteProduct(productId: String) {
        _products.update { current -> current.filterNot { it.id == productId } }
    }

    fun toggleProductStatus(productId: String) {
        _products.update { current ->
            current.map { if (it.id == productId) it.copy(status = !it.status) else it }
        }
    }

    fun addCategory(
        name: String,
        description: String,
        sortOrder: Int = 0,
        status: Boolean = true
    ): Category {
        val newCat = Category(
            id = "cat_${System.currentTimeMillis()}",
            name = name,
            description = description,
            productsCount = 0,
            status = status,
            sortOrder = sortOrder
        )
        _categories.update { it + newCat }
        return newCat
    }

    fun updateCategory(
        categoryId: String,
        name: String,
        description: String,
        sortOrder: Int,
        status: Boolean
    ) {
        _categories.update { current ->
            current.map {
                if (it.id == categoryId) {
                    it.copy(name = name, description = description, sortOrder = sortOrder, status = status)
                } else it
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        _categories.update { current -> current.filterNot { it.id == categoryId } }
    }

    fun toggleCategoryStatus(categoryId: String) {
        _categories.update { current ->
            current.map { if (it.id == categoryId) it.copy(status = !it.status) else it }
        }
    }

    fun getDetailedOrder(order: Order): OrderDetail {
        return OrderDetail(
            order = order,
            items = listOf(
                com.example.model.OrderItem(
                    id = "item_${order.id}",
                    orderId = order.id,
                    productId = "0",
                    name = "Dettaglio Ordine #${order.orderNumber}",
                    model = "OC-ORDER",
                    quantity = order.itemsCount.coerceAtLeast(1),
                    price = order.total,
                    total = order.total
                )
            ),
            customerPhone = "+39 Non specificato",
            shippingAddress = "${order.customerName}\n${order.shippingMethod}",
            paymentAddress = "${order.customerName}\n${order.paymentMethod}",
            subtotal = order.total,
            shippingCost = 0.0,
            taxAmount = 0.0,
            discountAmount = 0.0,
            grandTotal = order.total,
            customerNotes = order.notes,
            isFromLocalCache = true,
            cachedTimestamp = "OpenCart Live / DB"
        )
    }

    fun getAllDetailedOrders(): List<OrderDetail> {
        return _orders.value.map { getDetailedOrder(it) }
    }

    fun updateLiveVisitorCount(delta: Int) {
        _visitorStats.update { current ->
            val newCount = (current.activeVisitorsNow + delta).coerceAtLeast(0)
            current.copy(activeVisitorsNow = newCount)
        }
    }

    fun addLiveVisitorEvent(event: com.example.model.LiveVisitorEvent) {
        _visitorStats.update { current ->
            current.copy(liveEvents = listOf(event) + current.liveEvents.take(15))
        }
    }

    fun setSubscriptions(list: List<com.example.model.Subscription>) {
        _subscriptions.value = list
    }

    fun setCategories(list: List<Category>) {
        _categories.value = list
    }

    fun updateSubscriptionStatus(id: String, newStatus: com.example.model.SubscriptionStatus) {
        _subscriptions.update { current ->
            current.map {
                if (it.id == id || it.subscriptionId == id) it.copy(status = newStatus) else it
            }
        }
    }

    fun setReturns(list: List<com.example.model.OrderReturn>) {
        _returns.value = list
    }

    fun updateReturnStatus(id: String, newStatus: com.example.model.ReturnStatus, newAction: String? = null) {
        _returns.update { current ->
            current.map {
                if (it.id == id || it.returnId == id) {
                    it.copy(
                        status = newStatus,
                        action = newAction ?: it.action
                    )
                } else it
            }
        }
    }

    suspend fun clearDummyData() {
        _orders.value = emptyList()
        _products.value = emptyList()
        _categories.value = emptyList()
        _subscriptions.value = emptyList()
        _returns.value = emptyList()
        _activities.value = emptyList()
        _stores.update { list ->
            list.map {
                it.copy(
                    todayRevenue = 0.0,
                    revenueGrowthPercent = 0.0,
                    pendingOrdersCount = 0,
                    stockAlertsCount = 0,
                    lastSyncTime = "Dati azzerati"
                )
            }
        }
    }
}
