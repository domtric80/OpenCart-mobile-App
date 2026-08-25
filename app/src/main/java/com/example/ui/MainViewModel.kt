package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EcomRepository
import com.example.data.local.AppDatabase
import com.example.data.local.OfflineAuditRepository
import com.example.data.local.OfflineCatalogRepository
import com.example.data.local.OfflineOrderRepository
import com.example.model.ActivityItem
import com.example.model.AuditActionType
import com.example.model.AuditLog
import com.example.model.Category
import com.example.model.HourlySalesPoint
import com.example.model.Order
import com.example.model.OrderDetail
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.SalesMetrics
import com.example.model.Store
import com.example.model.VisitorRealtimeStats
import com.example.network.OpenCartApiClient
import com.example.network.OpenCartConnectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Timeframe(val label: String) {
    TODAY("Oggi"),
    THIS_WEEK("Settimana"),
    THIS_MONTH("Mese")
}

data class DashboardUiState(
    val stores: List<Store> = emptyList(),
    val currentStore: Store? = null,
    val activities: List<ActivityItem> = emptyList(),
    val orders: List<Order> = emptyList(),
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedTimeframe: Timeframe = Timeframe.TODAY,
    val selectedOrderFilter: OrderStatus? = null,
    val searchQuery: String = "",
    val isStoreSwitcherOpen: Boolean = false,
    val selectedOrderForDetail: Order? = null,
    val selectedOrderDetail: OrderDetail? = null,
    val syncSuccessMessage: String? = null,
    val isTestingConnection: Boolean = false,
    val connectionResult: OpenCartConnectionResult? = null,
    val visitorStats: VisitorRealtimeStats = VisitorRealtimeStats(),
    val auditLogs: List<AuditLog> = emptyList(),
    val salesMetrics: SalesMetrics = SalesMetrics(
        totalRevenue = 0.0,
        revenueGrowthPercent = 0.0,
        orderCount = 0,
        orderGrowthPercent = 0.0,
        completedOrdersCount = 0,
        pendingOrdersCount = 0,
        averageOrderValue = 0.0,
        aovGrowthPercent = 0.0,
        averageItemsPerOrder = 0.0,
        conversionRate = 0.0,
        salesVelocityPerHour = 0.0
    )
)

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val apiClient = OpenCartApiClient()
    private val repository = EcomRepository(db.storeProfileDao(), apiClient)
    private val offlineOrderRepository = OfflineOrderRepository(db.orderDao())
    private val offlineCatalogRepository = OfflineCatalogRepository(db.productDao(), db.categoryDao())
    private val offlineAuditRepository = OfflineAuditRepository(db.auditLogDao())
    private val securityManager = com.example.auth.SecurityManager(application)

    private val _selectedTimeframe = MutableStateFlow(Timeframe.TODAY)
    private val _selectedOrderFilter = MutableStateFlow<OrderStatus?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isStoreSwitcherOpen = MutableStateFlow(false)
    private val _selectedOrderForDetail = MutableStateFlow<Order?>(null)
    private val _selectedOrderDetail = MutableStateFlow<OrderDetail?>(null)
    private val _syncSuccessMessage = MutableStateFlow<String?>(null)
    private val _isTestingConnection = MutableStateFlow(false)
    private val _connectionResult = MutableStateFlow<OpenCartConnectionResult?>(null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Carica store salvati sul database Room
            repository.loadPersistedStores()

            // Carica ordini locali cached dal DB Room
            val cachedOrders = db.orderDao().getAllOrders()
            if (cachedOrders.isNotEmpty()) {
                repository.setLiveOrders(cachedOrders.map { it.toDomainModel() })
            }

            val cachedProducts = db.productDao().getAllProducts()
            if (cachedProducts.isNotEmpty()) {
                repository.setLiveProducts(cachedProducts.map { it.toDomainModel() })
            }

            // Se è presente uno store configurato, avvia la sincronizzazione automatica reale
            val primaryStore = db.storeProfileDao().getPrimaryStore()
            if (primaryStore != null && primaryStore.url.isNotBlank()) {
                syncDataFromOpenCart(primaryStore.url, primaryStore.apiKey)
            }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.stores,
        repository.currentStoreId,
        repository.activities,
        repository.orders,
        repository.products,
        repository.categories,
        repository.visitorStats,
        offlineAuditRepository.getAllAuditLogs(),
        _selectedTimeframe,
        _selectedOrderFilter,
        _searchQuery,
        _isStoreSwitcherOpen,
        _selectedOrderForDetail,
        _selectedOrderDetail,
        _syncSuccessMessage,
        _isTestingConnection,
        _connectionResult
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val stores = args[0] as List<Store>
        val currentStoreId = args[1] as String
        @Suppress("UNCHECKED_CAST")
        val activities = args[2] as List<ActivityItem>
        @Suppress("UNCHECKED_CAST")
        val orders = args[3] as List<Order>
        @Suppress("UNCHECKED_CAST")
        val products = args[4] as List<Product>
        @Suppress("UNCHECKED_CAST")
        val categories = args[5] as List<Category>
        val visitorStats = args[6] as VisitorRealtimeStats
        @Suppress("UNCHECKED_CAST")
        val auditLogs = args[7] as List<AuditLog>
        val timeframe = args[8] as Timeframe
        val orderFilter = args[9] as OrderStatus?
        val query = args[10] as String
        val isSwitcherOpen = args[11] as Boolean
        val selectedOrder = args[12] as Order?
        val selectedDetail = args[13] as OrderDetail?
        val syncMessage = args[14] as String?
        val isTesting = args[15] as Boolean
        val connectionResult = args[16] as OpenCartConnectionResult?

        val currentStore = stores.find { it.id == currentStoreId } ?: stores.firstOrNull()

        val totalRev = orders.sumOf { it.total }
        val orderCount = orders.size
        val completedCount = orders.count { it.status == OrderStatus.DELIVERED || it.status == OrderStatus.COMPLETE }
        val pendingCount = orders.count { it.status == OrderStatus.PENDING || it.status == OrderStatus.PROCESSING }
        val aov = if (orderCount > 0) totalRev / orderCount else 0.0

        val metrics = SalesMetrics(
            totalRevenue = totalRev,
            revenueGrowthPercent = 0.0,
            orderCount = orderCount,
            orderGrowthPercent = 0.0,
            completedOrdersCount = completedCount,
            pendingOrdersCount = pendingCount,
            averageOrderValue = aov,
            aovGrowthPercent = 0.0,
            averageItemsPerOrder = 1.0,
            conversionRate = 0.0,
            salesVelocityPerHour = if (orderCount > 0) totalRev / 24.0 else 0.0
        )

        DashboardUiState(
            stores = stores,
            currentStore = currentStore,
            activities = activities,
            orders = orders,
            products = products,
            categories = categories,
            selectedTimeframe = timeframe,
            selectedOrderFilter = orderFilter,
            searchQuery = query,
            isStoreSwitcherOpen = isSwitcherOpen,
            selectedOrderForDetail = selectedOrder,
            selectedOrderDetail = selectedDetail,
            syncSuccessMessage = syncMessage,
            isTestingConnection = isTesting,
            connectionResult = connectionResult,
            visitorStats = visitorStats,
            auditLogs = auditLogs,
            salesMetrics = metrics
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun selectStore(storeId: String) {
        repository.selectStore(storeId)
        viewModelScope.launch(Dispatchers.IO) {
            val store = db.storeProfileDao().getStoreById(storeId)
            if (store != null) {
                db.storeProfileDao().setPrimaryStore(storeId)
                if (store.url.isNotBlank()) {
                    syncDataFromOpenCart(store.url, store.apiKey)
                }
            }
        }
    }

    fun openStoreSwitcher() { _isStoreSwitcherOpen.value = true }
    fun closeStoreSwitcher() { _isStoreSwitcherOpen.value = false }
    fun selectTimeframe(tf: Timeframe) { _selectedTimeframe.value = tf }
    fun setOrderFilter(st: OrderStatus?) { _selectedOrderFilter.value = st }
    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun selectOrderForDetail(order: Order) {
        _selectedOrderForDetail.value = order
        _selectedOrderDetail.value = repository.getDetailedOrder(order)
    }

    fun clearSelectedOrder() {
        _selectedOrderForDetail.value = null
        _selectedOrderDetail.value = null
    }

    fun triggerSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentStore = uiState.value.currentStore
            if (currentStore != null && currentStore.url.isNotBlank()) {
                _syncSuccessMessage.value = "Sincronizzazione in corso..."
                val success = syncDataFromOpenCart(currentStore.url, currentStore.apiKey)
                if (success) {
                    _syncSuccessMessage.value = "Sincronizzazione con OpenCart completata con successo!"
                } else {
                    _syncSuccessMessage.value = "Errore di sincronizzazione con ${currentStore.url}. Verifica credenziali."
                }
            } else {
                _syncSuccessMessage.value = "Nessuno store OpenCart configurato. Vai in Impostazioni per aggiungerlo."
            }
            kotlinx.coroutines.delay(3000)
            _syncSuccessMessage.value = null
        }
    }

    private suspend fun syncDataFromOpenCart(url: String, apiKey: String): Boolean {
        return try {
            val ordersRes = apiClient.fetchOrders(url, apiKey, limit = 50)
            if (ordersRes.isSuccess) {
                val liveOrders = ordersRes.getOrNull() ?: emptyList()
                repository.setLiveOrders(liveOrders)

                // Salva nella persistenza locale Room
                db.orderDao().clearAllOrders()
                liveOrders.forEach { order ->
                    db.orderDao().insertOrder(
                        com.example.data.local.entity.OrderEntity(
                            id = order.id,
                            storeId = uiState.value.currentStore?.id ?: "store_1",
                            orderNumber = order.orderNumber,
                            customerName = order.customerName,
                            customerEmail = order.customerEmail,
                            total = order.total,
                            status = order.status.name,
                            dateAdded = order.dateAdded,
                            itemsCount = order.itemsCount,
                            shippingMethod = order.shippingMethod,
                            paymentMethod = order.paymentMethod,
                            customerNotes = order.notes
                        )
                    )
                }
            }

            val prodRes = apiClient.fetchProducts(url, apiKey, limit = 100)
            if (prodRes.isSuccess) {
                val liveProds = prodRes.getOrNull() ?: emptyList()
                repository.setLiveProducts(liveProds)

                db.productDao().clearAllProducts()
                liveProds.forEach { prod ->
                    db.productDao().insertProduct(
                        com.example.data.local.entity.ProductEntity(
                            id = prod.id,
                            storeId = uiState.value.currentStore?.id ?: "store_1",
                            name = prod.name,
                            model = prod.model,
                            sku = prod.sku,
                            price = prod.price,
                            specialPrice = prod.specialPrice,
                            quantity = prod.quantity,
                            category = prod.category,
                            description = prod.description,
                            status = prod.status
                        )
                    )
                }
            }
            ordersRes.isSuccess || prodRes.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        repository.updateOrderStatus(orderId, newStatus)
        viewModelScope.launch(Dispatchers.IO) {
            offlineOrderRepository.updateOrderStatus(orderId, newStatus)
            val store = uiState.value.currentStore
            if (store != null && store.url.isNotBlank()) {
                val statusId = when (newStatus) {
                    OrderStatus.PENDING -> 1
                    OrderStatus.PROCESSING -> 2
                    OrderStatus.SHIPPED -> 3
                    OrderStatus.DELIVERED, OrderStatus.COMPLETE -> 5
                    OrderStatus.CANCELLED -> 7
                    else -> 2
                }
                apiClient.updateOrderStatus(store.url, store.apiKey, orderId, statusId, "Stato aggiornato da CartAdmin App")
            }
        }
    }

    fun updateOrderNotes(orderId: String, notes: String) {
        repository.updateOrderNotes(orderId, notes)
        viewModelScope.launch(Dispatchers.IO) {
            offlineOrderRepository.updateOrderNotes(orderId, notes)
        }
    }

    fun updateOrder(orderId: String, newStatus: OrderStatus, notes: String) {
        repository.updateOrderStatusAndNotes(orderId, newStatus, notes)
        viewModelScope.launch(Dispatchers.IO) {
            offlineOrderRepository.updateOrderStatusAndNotes(orderId, newStatus, notes)
            val store = uiState.value.currentStore
            if (store != null && store.url.isNotBlank()) {
                val statusId = when (newStatus) {
                    OrderStatus.PENDING -> 1
                    OrderStatus.PROCESSING -> 2
                    OrderStatus.SHIPPED -> 3
                    OrderStatus.DELIVERED, OrderStatus.COMPLETE -> 5
                    OrderStatus.CANCELLED -> 7
                    else -> 2
                }
                apiClient.updateOrderStatus(store.url, store.apiKey, orderId, statusId, notes)
            }
        }
    }

    fun updateProductStock(productId: String, delta: Int) {
        val prod = uiState.value.products.find { it.id == productId } ?: return
        val newQty = (prod.quantity + delta).coerceAtLeast(0)
        repository.updateProductStock(productId, newQty)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.updateProductStock(productId, newQty)
            val store = uiState.value.currentStore
            if (store != null && store.url.isNotBlank()) {
                apiClient.updateProductStock(store.url, store.apiKey, productId, newQty)
            }
        }
    }

    fun setDirectProductStock(productId: String, newQty: Int) {
        val clamped = newQty.coerceAtLeast(0)
        repository.updateProductStock(productId, clamped)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.updateProductStock(productId, clamped)
            val store = uiState.value.currentStore
            if (store != null && store.url.isNotBlank()) {
                apiClient.updateProductStock(store.url, store.apiKey, productId, clamped)
            }
        }
    }

    fun addNewProduct(
        name: String,
        model: String,
        sku: String,
        price: Double,
        specialPrice: Double? = null,
        quantity: Int,
        category: String,
        description: String = ""
    ) {
        val created = repository.addProduct(name, model, sku, price, specialPrice, quantity, category, description)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.saveProduct(created, uiState.value.currentStore?.id ?: "store_1")
        }
    }

    fun updateProduct(product: Product) {
        repository.updateProduct(product)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.saveProduct(product, uiState.value.currentStore?.id ?: "store_1")
        }
    }

    fun deleteProduct(productId: String) {
        repository.deleteProduct(productId)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.deleteProduct(productId)
        }
    }

    fun toggleProductStatus(productId: String) {
        val prod = uiState.value.products.find { it.id == productId } ?: return
        repository.toggleProductStatus(productId)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.updateProductStatus(productId, !prod.status)
        }
    }

    fun addNewCategory(name: String, description: String = "", sortOrder: Int = 0, status: Boolean = true) {
        val created = repository.addCategory(name, description, sortOrder, status)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.saveCategory(created, uiState.value.currentStore?.id ?: "store_1")
        }
    }

    fun updateCategory(categoryId: String, name: String, description: String, sortOrder: Int, status: Boolean) {
        repository.updateCategory(categoryId, name, description, sortOrder, status)
        val updated = repository.categories.value.find { it.id == categoryId }
        if (updated != null) {
            viewModelScope.launch(Dispatchers.IO) {
                offlineCatalogRepository.saveCategory(updated, uiState.value.currentStore?.id ?: "store_1")
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        repository.deleteCategory(categoryId)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.deleteCategory(categoryId)
        }
    }

    fun toggleCategoryStatus(categoryId: String) {
        repository.toggleCategoryStatus(categoryId)
    }

    fun clearDummyData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearDummyData()
            _selectedOrderForDetail.value = null
            _selectedOrderDetail.value = null
            db.orderDao().clearAllOrderItems()
            db.orderDao().clearAllOrders()
            db.productDao().clearAllProducts()
            db.categoryDao().clearAllCategories()
            _syncSuccessMessage.value = "Tutti i dati locali sono stati eliminati definitivamente."
            kotlinx.coroutines.delay(2500)
            _syncSuccessMessage.value = null
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            offlineAuditRepository.clearAuditLogs()
        }
    }

    fun testOpenCartConnection(url: String, username: String, key: String) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionResult.value = null
            val result = repository.testStoreConnection(url, username, key)
            _connectionResult.value = result
            _isTestingConnection.value = false
        }
    }

    fun saveStoreCredentials(
        storeId: String,
        name: String,
        url: String,
        username: String,
        key: String,
        version: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStoreCredentials(storeId, name, url, username, key, version)
            _syncSuccessMessage.value = "Parametri store salvati su memoria permanente!"
            syncDataFromOpenCart(url, key)
            kotlinx.coroutines.delay(2500)
            _syncSuccessMessage.value = null
        }
    }

    fun addStore(name: String, url: String, version: String, username: String = "api_user", key: String = "api_key_secret") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addStore(name, url, version, username, key)
            _isStoreSwitcherOpen.value = false
            syncDataFromOpenCart(url, key)
        }
    }

    fun deleteStore(storeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStore(storeId)
        }
    }
}
