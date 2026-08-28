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
import com.example.model.OrderReturn
import com.example.model.OrderStatus
import com.example.model.OrdersSubSection
import com.example.model.Product
import com.example.model.ReturnStatus
import com.example.model.SalesMetrics
import com.example.model.Store
import com.example.model.Subscription
import com.example.model.SubscriptionStatus
import com.example.model.VisitorRealtimeStats
import com.example.network.OpenCartApiClient
import com.example.network.OpenCartConnectionResult
import com.example.security.AndroidKeystoreCredentialProtector
import com.example.security.CredentialProtectionException
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
    val subscriptions: List<com.example.model.Subscription> = emptyList(),
    val returns: List<com.example.model.OrderReturn> = emptyList(),
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedTimeframe: Timeframe = Timeframe.TODAY,
    val selectedOrderFilter: OrderStatus? = null,
    val selectedOrdersSubSection: com.example.model.OrdersSubSection = com.example.model.OrdersSubSection.ORDERS,
    val selectedSubscriptionFilter: com.example.model.SubscriptionStatus? = null,
    val selectedReturnFilter: com.example.model.ReturnStatus? = null,
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
    private val apiClient = OpenCartApiClient(application)
    private val credentialProtector = AndroidKeystoreCredentialProtector(application)
    private val repository = EcomRepository(db.storeProfileDao(), apiClient, credentialProtector)
    private val offlineOrderRepository = OfflineOrderRepository(db.orderDao())
    private val offlineCatalogRepository = OfflineCatalogRepository(db.productDao(), db.categoryDao())
    private val offlineAuditRepository = OfflineAuditRepository(db.auditLogDao())
    private val securityManager = com.example.auth.SecurityManager(application)

    private val _selectedTimeframe = MutableStateFlow(Timeframe.TODAY)
    private val _selectedOrderFilter = MutableStateFlow<OrderStatus?>(null)
    private val _selectedOrdersSubSection = MutableStateFlow(com.example.model.OrdersSubSection.ORDERS)
    private val _selectedSubscriptionFilter = MutableStateFlow<com.example.model.SubscriptionStatus?>(null)
    private val _selectedReturnFilter = MutableStateFlow<com.example.model.ReturnStatus?>(null)
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
            try {
                val migrated = repository.loadPersistedStores()
                if (migrated && !db.sanitizeAfterCredentialMigration()) {
                    _syncSuccessMessage.value =
                        "Credenziali cifrate; pulizia delle pagine SQLite da ripetere."
                }
            } catch (error: CredentialProtectionException) {
                _syncSuccessMessage.value =
                    "Credenziali bloccate: il dispositivo richiede TEE o StrongBox funzionante."
            }

            // Carica ordini locali cached dal DB Room
            val cachedOrders = db.orderDao().getAllOrders()
            if (cachedOrders.isNotEmpty()) {
                repository.setLiveOrders(cachedOrders.map { it.toDomainModel() })
            }

            val cachedProducts = db.productDao().getAllProducts()
            if (cachedProducts.isNotEmpty()) {
                repository.setLiveProducts(cachedProducts.map { it.toDomainModel() })
            }

            val cachedSubs = db.subscriptionDao().getAllSubscriptions()
            if (cachedSubs.isNotEmpty()) {
                repository.setSubscriptions(cachedSubs.map { it.toDomainModel() })
            } else {
                // Inizializza con abbonamenti di esempio per visualizzazione immediata
                val defaultSubs = listOf(
                    com.example.model.Subscription(
                        id = "sub_101",
                        subscriptionId = "#SUB-101",
                        customerName = "Marco Rossi",
                        customerEmail = "marco.rossi@email.it",
                        planName = "Fornitura Caffè Espresso Gold",
                        cycleFrequency = "Ogni 30 giorni",
                        amount = 34.90,
                        status = com.example.model.SubscriptionStatus.ACTIVE,
                        nextPaymentDate = "28/09/2026",
                        startDate = "28/05/2026",
                        paymentMethod = "Carta di Credito (Stripe)"
                    ),
                    com.example.model.Subscription(
                        id = "sub_102",
                        subscriptionId = "#SUB-102",
                        customerName = "Laura Bianchi",
                        customerEmail = "laura.b@gmail.com",
                        planName = "Box Biologica Stagionale XL",
                        cycleFrequency = "Ogni 14 giorni",
                        amount = 49.00,
                        status = com.example.model.SubscriptionStatus.ACTIVE,
                        nextPaymentDate = "15/09/2026",
                        startDate = "01/06/2026",
                        paymentMethod = "PayPal Ricorrente"
                    ),
                    com.example.model.Subscription(
                        id = "sub_103",
                        subscriptionId = "#SUB-103",
                        customerName = "Studio Tecnico Rossi",
                        customerEmail = "ordini@rossistudio.it",
                        planName = "Manutenzione & Ricambi Trimestrale",
                        cycleFrequency = "Ogni 90 giorni",
                        amount = 180.00,
                        status = com.example.model.SubscriptionStatus.SUSPENDED,
                        nextPaymentDate = "10/10/2026",
                        startDate = "10/01/2026",
                        paymentMethod = "Addebito Diretto SEPA"
                    )
                )
                repository.setSubscriptions(defaultSubs)
            }

            val cachedReturns = db.orderReturnDao().getAllReturns()
            if (cachedReturns.isNotEmpty()) {
                repository.setReturns(cachedReturns.map { it.toDomainModel() })
            } else {
                // Inizializza con resi di esempio per visualizzazione immediata
                val defaultReturns = listOf(
                    com.example.model.OrderReturn(
                        id = "ret_501",
                        returnId = "RMA-501",
                        orderId = "#10042",
                        customerName = "Giuseppe Verdi",
                        customerEmail = "g.verdi@pec.it",
                        customerPhone = "+39 333 4567890",
                        productName = "Cuffie Bluetooth Noise Cancelling Pro",
                        productModel = "AUDIO-PRO-X",
                        quantity = 1,
                        reason = "Pacco arrivato danneggiato / non funzionante",
                        opened = true,
                        status = com.example.model.ReturnStatus.AWAITING_PRODUCTS,
                        action = "In attesa di ricezione merce in magazzino",
                        dateAdded = "22/08/2026",
                        comment = "Il cliente segnala rottura dell'archetto sinistro."
                    ),
                    com.example.model.OrderReturn(
                        id = "ret_502",
                        returnId = "RMA-502",
                        orderId = "#10038",
                        customerName = "Alessia Ferrari",
                        customerEmail = "alessia.f@libero.it",
                        customerPhone = "+39 347 1122334",
                        productName = "Scarpe Running Ultra Grip - Taglia 39",
                        productModel = "SH-ULTRA-39",
                        quantity = 1,
                        reason = "Taglia errata / richiesta sostituzione",
                        opened = true,
                        status = com.example.model.ReturnStatus.PENDING,
                        action = "Da autorizzare con etichetta reso",
                        dateAdded = "24/08/2026",
                        comment = "Richiede sostituzione con taglia 40."
                    ),
                    com.example.model.OrderReturn(
                        id = "ret_503",
                        returnId = "RMA-503",
                        orderId = "#10015",
                        customerName = "Matteo Conti",
                        customerEmail = "m.conti@yahoo.it",
                        customerPhone = "+39 320 9876543",
                        productName = "Smartwatch AMOLED IP68 Steel",
                        productModel = "SW-AMOL-BK",
                        quantity = 1,
                        reason = "Difetto firmware dopo 3 giorni",
                        opened = true,
                        status = com.example.model.ReturnStatus.COMPLETE_REFUNDED,
                        action = "Rimborso emesso su carta di credito",
                        dateAdded = "12/08/2026",
                        comment = "Verificato difetto da laboratorio, rimborso effettuato."
                    )
                )
                repository.setReturns(defaultReturns)
            }

            // Se è presente uno store configurato, avvia la sincronizzazione automatica reale
            val primaryStore = repository.stores.value.find {
                it.id == repository.currentStoreId.value
            }
            if (primaryStore != null && primaryStore.url.isNotBlank()) {
                syncDataFromOpenCart(
                    primaryStore.url,
                    primaryStore.apiKey,
                    primaryStore.apiUsername
                )
            }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.stores,
        repository.currentStoreId,
        repository.activities,
        repository.orders,
        repository.subscriptions,
        repository.returns,
        repository.products,
        repository.categories,
        repository.visitorStats,
        offlineAuditRepository.getAllAuditLogs(),
        _selectedTimeframe,
        _selectedOrderFilter,
        _selectedOrdersSubSection,
        _selectedSubscriptionFilter,
        _selectedReturnFilter,
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
        val subs = args[4] as List<com.example.model.Subscription>
        @Suppress("UNCHECKED_CAST")
        val retList = args[5] as List<com.example.model.OrderReturn>
        @Suppress("UNCHECKED_CAST")
        val products = args[6] as List<Product>
        @Suppress("UNCHECKED_CAST")
        val categories = args[7] as List<Category>
        val visitorStats = args[8] as VisitorRealtimeStats
        @Suppress("UNCHECKED_CAST")
        val auditLogs = args[9] as List<AuditLog>
        val timeframe = args[10] as Timeframe
        val orderFilter = args[11] as OrderStatus?
        val subSection = args[12] as com.example.model.OrdersSubSection
        val subFilter = args[13] as com.example.model.SubscriptionStatus?
        val retFilter = args[14] as com.example.model.ReturnStatus?
        val query = args[15] as String
        val isSwitcherOpen = args[16] as Boolean
        val selectedOrder = args[17] as Order?
        val selectedDetail = args[18] as OrderDetail?
        val syncMessage = args[19] as String?
        val isTesting = args[20] as Boolean
        val connectionResult = args[21] as OpenCartConnectionResult?

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
            subscriptions = subs,
            returns = retList,
            products = products,
            categories = categories,
            selectedTimeframe = timeframe,
            selectedOrderFilter = orderFilter,
            selectedOrdersSubSection = subSection,
            selectedSubscriptionFilter = subFilter,
            selectedReturnFilter = retFilter,
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
                val revealedStore = repository.stores.value.find { it.id == storeId }
                if (revealedStore != null && revealedStore.url.isNotBlank()) {
                    syncDataFromOpenCart(
                        revealedStore.url,
                        revealedStore.apiKey,
                        revealedStore.apiUsername
                    )
                }
            }
        }
    }

    fun openStoreSwitcher() { _isStoreSwitcherOpen.value = true }
    fun closeStoreSwitcher() { _isStoreSwitcherOpen.value = false }
    fun selectTimeframe(tf: Timeframe) { _selectedTimeframe.value = tf }
    fun setOrderFilter(st: OrderStatus?) { _selectedOrderFilter.value = st }
    fun selectOrdersSubSection(subSection: com.example.model.OrdersSubSection) { _selectedOrdersSubSection.value = subSection }
    fun setSubscriptionFilter(status: com.example.model.SubscriptionStatus?) { _selectedSubscriptionFilter.value = status }
    fun setReturnFilter(status: com.example.model.ReturnStatus?) { _selectedReturnFilter.value = status }
    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun selectOrderForDetail(order: Order) {
        _selectedOrderForDetail.value = order
        _selectedOrderDetail.value = repository.getDetailedOrder(order)
    }

    fun clearSelectedOrder() {
        _selectedOrderForDetail.value = null
        _selectedOrderDetail.value = null
    }

    fun triggerSync(explicitUrl: String? = null, explicitKey: String? = null, explicitUsername: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentStore = uiState.value.currentStore
            val url = explicitUrl?.takeIf { it.isNotBlank() } ?: currentStore?.url ?: ""
            val key = explicitKey?.takeIf { it.isNotBlank() } ?: currentStore?.apiKey ?: ""
            val user = explicitUsername?.takeIf { it.isNotBlank() } ?: currentStore?.apiUsername ?: ""

            if (url.isNotBlank()) {
                _syncSuccessMessage.value = "Sincronizzazione con $url in corso..."
                val syncResult = syncDataFromOpenCartDetailed(url, key, user)
                _syncSuccessMessage.value = syncResult
            } else {
                _syncSuccessMessage.value = "Nessun URL specificato. Inserisci URL e Chiave API."
            }
            kotlinx.coroutines.delay(4000)
            _syncSuccessMessage.value = null
        }
    }

    private suspend fun syncDataFromOpenCartDetailed(url: String, apiKey: String, username: String = ""): String {
        return try {
            val ordersRes = apiClient.fetchOrders(url, apiKey, username, limit = 50)
            val prodRes = apiClient.fetchProducts(url, apiKey, username, limit = 100)
            val catRes = apiClient.fetchCategories(url, apiKey, username, limit = 100)
            val subsRes = apiClient.fetchSubscriptions(url, apiKey, username, limit = 50)
            val retRes = apiClient.fetchReturns(url, apiKey, username, limit = 50)

            var orderCount = 0
            var prodCount = 0
            var catCount = 0
            var subsCount = 0
            var retCount = 0

            if (ordersRes.isSuccess) {
                val liveOrders = ordersRes.getOrNull() ?: emptyList()
                orderCount = liveOrders.size
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

            if (prodRes.isSuccess) {
                val liveProds = prodRes.getOrNull() ?: emptyList()
                prodCount = liveProds.size
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

            if (catRes.isSuccess) {
                val liveCats = catRes.getOrNull() ?: emptyList()
                if (liveCats.isNotEmpty()) {
                    catCount = liveCats.size
                    repository.setCategories(liveCats)

                    db.categoryDao().clearAllCategories()
                    liveCats.forEach { cat ->
                        db.categoryDao().insertCategory(
                            com.example.data.local.entity.CategoryEntity(
                                id = cat.id,
                                storeId = uiState.value.currentStore?.id ?: "store_1",
                                name = cat.name,
                                description = cat.description,
                                sortOrder = cat.sortOrder,
                                status = cat.status
                            )
                        )
                    }
                }
            }

            if (subsRes.isSuccess) {
                val liveSubs = subsRes.getOrNull() ?: emptyList()
                if (liveSubs.isNotEmpty()) {
                    subsCount = liveSubs.size
                    repository.setSubscriptions(liveSubs)
                    db.subscriptionDao().clearAllSubscriptions()
                    liveSubs.forEach { sub ->
                        db.subscriptionDao().insertSubscription(
                            com.example.data.local.entity.SubscriptionEntity(
                                id = sub.id,
                                storeId = uiState.value.currentStore?.id ?: "store_1",
                                subscriptionId = sub.subscriptionId,
                                customerName = sub.customerName,
                                customerEmail = sub.customerEmail,
                                planName = sub.planName,
                                cycleFrequency = sub.cycleFrequency,
                                amount = sub.amount,
                                status = sub.status.name,
                                nextPaymentDate = sub.nextPaymentDate,
                                startDate = sub.startDate,
                                paymentMethod = sub.paymentMethod
                            )
                        )
                    }
                }
            }

            if (retRes.isSuccess) {
                val liveReturns = retRes.getOrNull() ?: emptyList()
                if (liveReturns.isNotEmpty()) {
                    retCount = liveReturns.size
                    repository.setReturns(liveReturns)
                    db.orderReturnDao().clearAllReturns()
                    liveReturns.forEach { ret ->
                        db.orderReturnDao().insertReturn(
                            com.example.data.local.entity.OrderReturnEntity(
                                id = ret.id,
                                storeId = uiState.value.currentStore?.id ?: "store_1",
                                returnId = ret.returnId,
                                orderId = ret.orderId,
                                customerName = ret.customerName,
                                customerEmail = ret.customerEmail,
                                customerPhone = ret.customerPhone,
                                productName = ret.productName,
                                productModel = ret.productModel,
                                quantity = ret.quantity,
                                reason = ret.reason,
                                opened = ret.opened,
                                status = ret.status.name,
                                action = ret.action,
                                dateAdded = ret.dateAdded,
                                comment = ret.comment
                            )
                        )
                    }
                }
            }

            if (ordersRes.isSuccess || prodRes.isSuccess || catRes.isSuccess) {
                "Sincronizzazione completata: $orderCount ordini, $prodCount prodotti, $catCount categorie, $subsCount abbonamenti e $retCount resi!"
            } else {
                val err = ordersRes.exceptionOrNull()?.message ?: prodRes.exceptionOrNull()?.message ?: "Errore sconosciuto"
                "Errore sinc: $err. Verifica URL e Chiave API in Impostazioni."
            }
        } catch (e: Exception) {
            "Errore durante la sincronizzazione: ${e.localizedMessage}"
        }
    }

    private suspend fun syncDataFromOpenCart(url: String, apiKey: String, username: String = ""): Boolean {
        return try {
            val ordersRes = apiClient.fetchOrders(url, apiKey, username, limit = 50)
            if (ordersRes.isSuccess) {
                val liveOrders = ordersRes.getOrNull() ?: emptyList()
                repository.setLiveOrders(liveOrders)

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

            val prodRes = apiClient.fetchProducts(url, apiKey, username, limit = 100)
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

            val catRes = apiClient.fetchCategories(url, apiKey, username, limit = 100)
            if (catRes.isSuccess) {
                val liveCats = catRes.getOrNull() ?: emptyList()
                if (liveCats.isNotEmpty()) {
                    repository.setCategories(liveCats)
                    db.categoryDao().clearAllCategories()
                    liveCats.forEach { cat ->
                        db.categoryDao().insertCategory(
                            com.example.data.local.entity.CategoryEntity(
                                id = cat.id,
                                storeId = uiState.value.currentStore?.id ?: "store_1",
                                name = cat.name,
                                description = cat.description,
                                sortOrder = cat.sortOrder,
                                status = cat.status
                            )
                        )
                    }
                }
            }

            val subsRes = apiClient.fetchSubscriptions(url, apiKey, username, limit = 50)
            if (subsRes.isSuccess) {
                val liveSubs = subsRes.getOrNull() ?: emptyList()
                if (liveSubs.isNotEmpty()) {
                    repository.setSubscriptions(liveSubs)
                    db.subscriptionDao().clearAllSubscriptions()
                    liveSubs.forEach { sub ->
                        db.subscriptionDao().insertSubscription(
                            com.example.data.local.entity.SubscriptionEntity(
                                id = sub.id,
                                storeId = uiState.value.currentStore?.id ?: "store_1",
                                subscriptionId = sub.subscriptionId,
                                customerName = sub.customerName,
                                customerEmail = sub.customerEmail,
                                planName = sub.planName,
                                cycleFrequency = sub.cycleFrequency,
                                amount = sub.amount,
                                status = sub.status.name,
                                nextPaymentDate = sub.nextPaymentDate,
                                startDate = sub.startDate,
                                paymentMethod = sub.paymentMethod
                            )
                        )
                    }
                }
            }

            val retRes = apiClient.fetchReturns(url, apiKey, username, limit = 50)
            if (retRes.isSuccess) {
                val liveReturns = retRes.getOrNull() ?: emptyList()
                if (liveReturns.isNotEmpty()) {
                    repository.setReturns(liveReturns)
                    db.orderReturnDao().clearAllReturns()
                    liveReturns.forEach { ret ->
                        db.orderReturnDao().insertReturn(
                            com.example.data.local.entity.OrderReturnEntity(
                                id = ret.id,
                                storeId = uiState.value.currentStore?.id ?: "store_1",
                                returnId = ret.returnId,
                                orderId = ret.orderId,
                                customerName = ret.customerName,
                                customerEmail = ret.customerEmail,
                                customerPhone = ret.customerPhone,
                                productName = ret.productName,
                                productModel = ret.productModel,
                                quantity = ret.quantity,
                                reason = ret.reason,
                                opened = ret.opened,
                                status = ret.status.name,
                                action = ret.action,
                                dateAdded = ret.dateAdded,
                                comment = ret.comment
                            )
                        )
                    }
                }
            }

            ordersRes.isSuccess || prodRes.isSuccess || catRes.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    fun updateSubscriptionStatus(subscriptionId: String, newStatus: SubscriptionStatus) {
        repository.updateSubscriptionStatus(subscriptionId, newStatus)
        viewModelScope.launch(Dispatchers.IO) {
            db.subscriptionDao().updateSubscriptionStatus(subscriptionId, newStatus.name)
            val store = uiState.value.currentStore
            if (store != null && store.url.isNotBlank()) {
                apiClient.updateSubscriptionStatus(store.url, store.apiKey, subscriptionId, newStatus.name, store.apiUsername)
            }
        }
    }

    fun updateReturnStatus(returnId: String, newStatus: ReturnStatus, newAction: String = "In lavorazione") {
        repository.updateReturnStatus(returnId, newStatus, newAction)
        viewModelScope.launch(Dispatchers.IO) {
            db.orderReturnDao().updateReturnStatus(returnId, newStatus.name, newAction)
            val store = uiState.value.currentStore
            if (store != null && store.url.isNotBlank()) {
                val statusId = when (newStatus) {
                    ReturnStatus.PENDING -> 1
                    ReturnStatus.AWAITING_PRODUCTS -> 2
                    ReturnStatus.IN_INSPECTION -> 3
                    ReturnStatus.COMPLETE_REFUNDED, ReturnStatus.COMPLETE_REPLACED -> 4
                    ReturnStatus.DENIED -> 5
                }
                apiClient.updateReturnStatus(store.url, store.apiKey, returnId, statusId, newAction, store.apiUsername)
            }
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
            try {
                val effectiveKey = key.ifBlank {
                    repository.stores.value.find { it.id == storeId }?.apiKey.orEmpty()
                }
                repository.updateStoreCredentials(storeId, name, url, username, key, version)
                _syncSuccessMessage.value = "Parametri store cifrati nel chip hardware."
                if (effectiveKey.isNotBlank()) {
                    syncDataFromOpenCart(url, effectiveKey, username)
                }
            } catch (_: CredentialProtectionException) {
                _syncSuccessMessage.value =
                    "Salvataggio rifiutato: TEE o StrongBox hardware non disponibile."
            }
            kotlinx.coroutines.delay(2500)
            _syncSuccessMessage.value = null
        }
    }

    fun addStore(name: String, url: String, version: String, username: String = "", key: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.addStore(name, url, version, username, key)
                _isStoreSwitcherOpen.value = false
                if (key.isNotBlank()) {
                    syncDataFromOpenCart(url, key, username)
                }
            } catch (_: CredentialProtectionException) {
                _syncSuccessMessage.value =
                    "Store non salvato: TEE o StrongBox hardware non disponibile."
            }
        }
    }

    fun deleteStore(storeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStore(storeId)
        }
    }
}
