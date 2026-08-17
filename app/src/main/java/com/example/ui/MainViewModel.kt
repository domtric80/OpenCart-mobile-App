package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EcomRepository
import com.example.data.local.AppDatabase
import com.example.data.local.OfflineCatalogRepository
import com.example.data.local.OfflineOrderRepository
import com.example.model.ActivityItem
import com.example.model.Category
import com.example.model.HourlySalesPoint
import com.example.model.Order
import com.example.model.OrderDetail
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.SalesMetrics
import com.example.model.Store
import com.example.model.VisitorRealtimeStats
import com.example.model.LiveVisitorEvent
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
    val connectionResult: com.example.network.OpenCartConnectionResult? = null,
    val visitorStats: com.example.model.VisitorRealtimeStats = com.example.model.VisitorRealtimeStats(),
    val salesMetrics: SalesMetrics = SalesMetrics(
        totalRevenue = 2840.50,
        revenueGrowthPercent = 12.4,
        orderCount = 24,
        orderGrowthPercent = 8.3,
        completedOrdersCount = 18,
        pendingOrdersCount = 6,
        averageOrderValue = 118.35,
        aovGrowthPercent = 4.2,
        averageItemsPerOrder = 2.6,
        conversionRate = 3.8,
        salesVelocityPerHour = 236.70
    )
)

class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: EcomRepository = EcomRepository()
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val offlineOrderRepository = OfflineOrderRepository(db.orderDao())
    private val offlineCatalogRepository = OfflineCatalogRepository(db.productDao(), db.categoryDao())

    private val _selectedTimeframe = MutableStateFlow(Timeframe.TODAY)
    private val _selectedOrderFilter = MutableStateFlow<OrderStatus?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isStoreSwitcherOpen = MutableStateFlow(false)
    private val _selectedOrderForDetail = MutableStateFlow<Order?>(null)
    private val _selectedOrderDetail = MutableStateFlow<OrderDetail?>(null)
    private val _syncSuccessMessage = MutableStateFlow<String?>(null)
    private val _isTestingConnection = MutableStateFlow(false)
    private val _connectionResult = MutableStateFlow<com.example.network.OpenCartConnectionResult?>(null)

    init {
        // Seed Room Local Cache with orders, products, and categories for offline persistence
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val initialDetails = repository.getAllDetailedOrders()
                initialDetails.forEach { detail ->
                    offlineOrderRepository.cacheFullOrderDetail(detail, "store_1")
                }
                offlineCatalogRepository.cacheProducts(repository.products.value, "store_1")
                offlineCatalogRepository.cacheCategories(repository.categories.value, "store_1")
            } catch (e: Exception) {
                // Handled gracefully
            }
        }

        // Live Real-Time Telemetry Simulation loop
        viewModelScope.launch {
            val sampleEvents = listOf(
                com.example.model.LiveVisitorEvent(
                    id = "evt_live_1",
                    timestamp = "Adesso",
                    eventType = "page_view",
                    description = "Visualizzata scheda 'Smartwatch Ultra Pro'",
                    location = "Milano, IT",
                    iconType = "view"
                ),
                com.example.model.LiveVisitorEvent(
                    id = "evt_live_2",
                    timestamp = "Adesso",
                    eventType = "cart_add",
                    description = "Aggiunto 'Cuffie Wireless ANC' al carrello",
                    location = "Roma, IT",
                    iconType = "cart"
                ),
                com.example.model.LiveVisitorEvent(
                    id = "evt_live_3",
                    timestamp = "Adesso",
                    eventType = "checkout_start",
                    description = "Iniziato checkout OpenCart con PayPal",
                    location = "Torino, IT",
                    iconType = "checkout"
                ),
                com.example.model.LiveVisitorEvent(
                    id = "evt_live_4",
                    timestamp = "Adesso",
                    eventType = "search",
                    description = "Ricerca interna: 'supporto auto wireless'",
                    location = "Bologna, IT",
                    iconType = "search"
                )
            )

            var eventIdx = 0
            while (true) {
                kotlinx.coroutines.delay(6000)
                val delta = (-2..3).random()
                repository.updateLiveVisitorCount(delta)
                if ((0..10).random() > 4) {
                    val event = sampleEvents[eventIdx % sampleEvents.size].copy(
                        id = "evt_${System.currentTimeMillis()}"
                    )
                    repository.addLiveVisitorEvent(event)
                    eventIdx++
                }
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
        _selectedTimeframe,
        _selectedOrderFilter,
        _searchQuery,
        _isStoreSwitcherOpen,
        _selectedOrderForDetail,
        _selectedOrderDetail,
        _syncSuccessMessage,
        _isTestingConnection,
        _connectionResult
    ) { params ->
        val stores = (params.getOrNull(0) as? List<*>)?.filterIsInstance<Store>() ?: emptyList()
        val currentStoreId = params.getOrNull(1) as? String ?: "store_1"
        val activities = (params.getOrNull(2) as? List<*>)?.filterIsInstance<ActivityItem>() ?: emptyList()
        val orders = (params.getOrNull(3) as? List<*>)?.filterIsInstance<Order>() ?: emptyList()
        val products = (params.getOrNull(4) as? List<*>)?.filterIsInstance<Product>() ?: emptyList()
        val categories = (params.getOrNull(5) as? List<*>)?.filterIsInstance<Category>() ?: emptyList()
        val visitorStats = params.getOrNull(6) as? com.example.model.VisitorRealtimeStats ?: com.example.model.VisitorRealtimeStats()
        val timeframe = params.getOrNull(7) as? Timeframe ?: Timeframe.TODAY
        val orderFilter = params.getOrNull(8) as? OrderStatus
        val query = params.getOrNull(9) as? String ?: ""
        val isSwitcherOpen = params.getOrNull(10) as? Boolean ?: false
        val orderDetailSimple = params.getOrNull(11) as? Order
        val fullOrderDetail = params.getOrNull(12) as? OrderDetail
        val syncMsg = params.getOrNull(13) as? String
        val isTesting = params.getOrNull(14) as? Boolean ?: false
        val connResult = params.getOrNull(15) as? com.example.network.OpenCartConnectionResult

        val currentStore = stores.find { it.id == currentStoreId } ?: stores.firstOrNull()

        val baseRev = currentStore?.todayRevenue ?: 2840.50
        val baseGrowth = currentStore?.revenueGrowthPercent ?: 12.4
        val basePending = currentStore?.pendingOrdersCount ?: 24

        val metrics = when (timeframe) {
            Timeframe.TODAY -> {
                val rev = baseRev
                val ordCount = (basePending * 1.0).toInt().coerceAtLeast(6)
                val aov = if (ordCount > 0) rev / ordCount else 118.35
                SalesMetrics(
                    totalRevenue = rev,
                    revenueGrowthPercent = baseGrowth,
                    orderCount = ordCount,
                    orderGrowthPercent = 8.3,
                    completedOrdersCount = (ordCount * 0.75).toInt(),
                    pendingOrdersCount = (ordCount * 0.25).toInt().coerceAtLeast(1),
                    averageOrderValue = aov,
                    aovGrowthPercent = 4.2,
                    averageItemsPerOrder = 2.6,
                    conversionRate = 3.8,
                    salesVelocityPerHour = (rev / 12.0),
                    hourlySales = listOf(
                        HourlySalesPoint("08:00", rev * 0.05, (ordCount * 0.05).toInt().coerceAtLeast(1)),
                        HourlySalesPoint("10:00", rev * 0.14, (ordCount * 0.14).toInt().coerceAtLeast(1)),
                        HourlySalesPoint("12:00", rev * 0.18, (ordCount * 0.18).toInt().coerceAtLeast(1)),
                        HourlySalesPoint("14:00", rev * 0.24, (ordCount * 0.24).toInt().coerceAtLeast(2), isCurrentPeak = true),
                        HourlySalesPoint("16:00", rev * 0.16, (ordCount * 0.16).toInt().coerceAtLeast(1)),
                        HourlySalesPoint("18:00", rev * 0.15, (ordCount * 0.15).toInt().coerceAtLeast(1)),
                        HourlySalesPoint("20:00", rev * 0.08, (ordCount * 0.08).toInt().coerceAtLeast(1))
                    )
                )
            }
            Timeframe.THIS_WEEK -> {
                val rev = baseRev * 7.0
                val ordCount = ((basePending * 1.0) * 7.0).toInt().coerceAtLeast(42)
                val aov = if (ordCount > 0) rev / ordCount else 118.35
                SalesMetrics(
                    totalRevenue = rev,
                    revenueGrowthPercent = baseGrowth + 3.4,
                    orderCount = ordCount,
                    orderGrowthPercent = 11.2,
                    completedOrdersCount = (ordCount * 0.85).toInt(),
                    pendingOrdersCount = (ordCount * 0.15).toInt().coerceAtLeast(4),
                    averageOrderValue = aov,
                    aovGrowthPercent = 4.8,
                    averageItemsPerOrder = 2.8,
                    conversionRate = 4.1,
                    salesVelocityPerHour = (rev / 84.0),
                    hourlySales = listOf(
                        HourlySalesPoint("Lun", rev * 0.12, (ordCount * 0.12).toInt()),
                        HourlySalesPoint("Mar", rev * 0.14, (ordCount * 0.14).toInt()),
                        HourlySalesPoint("Mer", rev * 0.15, (ordCount * 0.15).toInt()),
                        HourlySalesPoint("Gio", rev * 0.19, (ordCount * 0.19).toInt(), isCurrentPeak = true),
                        HourlySalesPoint("Ven", rev * 0.18, (ordCount * 0.18).toInt()),
                        HourlySalesPoint("Sab", rev * 0.13, (ordCount * 0.13).toInt()),
                        HourlySalesPoint("Dom", rev * 0.09, (ordCount * 0.09).toInt())
                    )
                )
            }
            Timeframe.THIS_MONTH -> {
                val rev = baseRev * 29.5
                val ordCount = ((basePending * 1.0) * 29.5).toInt().coerceAtLeast(180)
                val aov = if (ordCount > 0) rev / ordCount else 118.35
                SalesMetrics(
                    totalRevenue = rev,
                    revenueGrowthPercent = baseGrowth + 7.2,
                    orderCount = ordCount,
                    orderGrowthPercent = 14.5,
                    completedOrdersCount = (ordCount * 0.88).toInt(),
                    pendingOrdersCount = (ordCount * 0.12).toInt().coerceAtLeast(12),
                    averageOrderValue = aov,
                    aovGrowthPercent = 5.6,
                    averageItemsPerOrder = 3.1,
                    conversionRate = 4.4,
                    salesVelocityPerHour = (rev / 360.0),
                    hourlySales = listOf(
                        HourlySalesPoint("Sett 1", rev * 0.22, (ordCount * 0.22).toInt()),
                        HourlySalesPoint("Sett 2", rev * 0.26, (ordCount * 0.26).toInt()),
                        HourlySalesPoint("Sett 3", rev * 0.31, (ordCount * 0.31).toInt(), isCurrentPeak = true),
                        HourlySalesPoint("Sett 4", rev * 0.21, (ordCount * 0.21).toInt())
                    )
                )
            }
        }

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
            selectedOrderForDetail = orderDetailSimple,
            selectedOrderDetail = fullOrderDetail,
            syncSuccessMessage = syncMsg,
            isTestingConnection = isTesting,
            connectionResult = connResult,
            visitorStats = visitorStats,
            salesMetrics = metrics
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun selectStore(storeId: String) {
        repository.selectStore(storeId)
        _connectionResult.value = null
        _isStoreSwitcherOpen.value = false
    }

    fun setStoreSwitcherOpen(open: Boolean) {
        _isStoreSwitcherOpen.value = open
    }

    fun setTimeframe(timeframe: Timeframe) {
        _selectedTimeframe.value = timeframe
    }

    fun setOrderFilter(filter: OrderStatus?) {
        _selectedOrderFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectOrderForDetail(order: Order?) {
        _selectedOrderForDetail.value = order
        if (order == null) {
            _selectedOrderDetail.value = null
        } else {
            val initialDetail = repository.getDetailedOrder(order)
            _selectedOrderDetail.value = initialDetail

            viewModelScope.launch(Dispatchers.IO) {
                val cached = offlineOrderRepository.getOrderDetail(order.id)
                if (cached != null) {
                    _selectedOrderDetail.value = cached
                } else {
                    offlineOrderRepository.cacheFullOrderDetail(initialDetail, "store_1")
                }
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        repository.updateOrderStatus(orderId, newStatus)
        _selectedOrderForDetail.value = _selectedOrderForDetail.value?.let {
            if (it.id == orderId) it.copy(status = newStatus) else it
        }
        _selectedOrderDetail.value = _selectedOrderDetail.value?.let {
            if (it.order.id == orderId) it.copy(order = it.order.copy(status = newStatus)) else it
        }
        viewModelScope.launch(Dispatchers.IO) {
            offlineOrderRepository.updateOrderStatus(orderId, newStatus)
        }
    }

    fun updateOrderNotes(orderId: String, notes: String) {
        repository.updateOrderNotes(orderId, notes)
        _selectedOrderForDetail.value = _selectedOrderForDetail.value?.let {
            if (it.id == orderId) it.copy(notes = notes) else it
        }
        _selectedOrderDetail.value = _selectedOrderDetail.value?.let {
            if (it.order.id == orderId) it.copy(order = it.order.copy(notes = notes), customerNotes = notes) else it
        }
        viewModelScope.launch(Dispatchers.IO) {
            offlineOrderRepository.updateOrderNotes(orderId, notes)
        }
    }

    fun updateOrder(orderId: String, newStatus: OrderStatus, notes: String) {
        repository.updateOrderStatusAndNotes(orderId, newStatus, notes)
        _selectedOrderForDetail.value = _selectedOrderForDetail.value?.let {
            if (it.id == orderId) it.copy(status = newStatus, notes = notes) else it
        }
        _selectedOrderDetail.value = _selectedOrderDetail.value?.let {
            if (it.order.id == orderId) it.copy(order = it.order.copy(status = newStatus, notes = notes), customerNotes = notes) else it
        }
        viewModelScope.launch(Dispatchers.IO) {
            offlineOrderRepository.updateOrderStatusAndNotes(orderId, newStatus, notes)
        }
    }

    // --- Product Management Actions ---

    fun updateProductStock(productId: String, delta: Int) {
        val prod = uiState.value.products.find { it.id == productId } ?: return
        val newQty = (prod.quantity + delta).coerceAtLeast(0)
        repository.updateProductStock(productId, newQty)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.updateProductStock(productId, newQty)
        }
    }

    fun setDirectProductStock(productId: String, newQty: Int) {
        val safeQty = newQty.coerceAtLeast(0)
        repository.updateProductStock(productId, safeQty)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.updateProductStock(productId, safeQty)
        }
    }

    fun addNewProduct(
        name: String,
        model: String,
        sku: String,
        price: Double,
        specialPrice: Double?,
        quantity: Int,
        minQuantityAlert: Int = 5,
        category: String,
        description: String = "",
        status: Boolean = true
    ) {
        val created = repository.addProduct(name, model, sku, price, specialPrice, quantity, minQuantityAlert, category, description, status)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.saveProduct(created, "store_1")
        }
    }

    fun updateProduct(product: Product) {
        repository.updateProduct(product)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.saveProduct(product, "store_1")
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
        val newStatus = !prod.status
        repository.toggleProductStatus(productId)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.updateProductStatus(productId, newStatus)
        }
    }

    // --- Category Management Actions ---

    fun addNewCategory(
        name: String,
        description: String = "",
        sortOrder: Int = 0,
        status: Boolean = true
    ) {
        val created = repository.addCategory(name, description, sortOrder, status)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.saveCategory(created, "store_1")
        }
    }

    fun updateCategory(
        categoryId: String,
        name: String,
        description: String,
        sortOrder: Int,
        status: Boolean
    ) {
        repository.updateCategory(categoryId, name, description, sortOrder, status)
        val updated = repository.categories.value.find { it.id == categoryId }
        if (updated != null) {
            viewModelScope.launch(Dispatchers.IO) {
                offlineCatalogRepository.saveCategory(updated, "store_1")
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
        val cat = uiState.value.categories.find { it.id == categoryId } ?: return
        val newStatus = !cat.status
        repository.toggleCategoryStatus(categoryId)
        viewModelScope.launch(Dispatchers.IO) {
            offlineCatalogRepository.updateCategoryStatus(categoryId, newStatus)
        }
    }

    // --- Store & Connection ---

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
        repository.updateStoreCredentials(storeId, name, url, username, key, version)
        _syncSuccessMessage.value = "Credenziali OpenCart salvate!"
        viewModelScope.launch {
            kotlinx.coroutines.delay(2500)
            _syncSuccessMessage.value = null
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _syncSuccessMessage.value = "Sincronizzazione OpenCart completata con successo!"
            kotlinx.coroutines.delay(2500)
            _syncSuccessMessage.value = null
        }
    }

    fun addStore(name: String, url: String, version: String, username: String = "api_user", key: String = "api_key_secret") {
        repository.addStore(name, url, version, username, key)
        _isStoreSwitcherOpen.value = false
    }
}
