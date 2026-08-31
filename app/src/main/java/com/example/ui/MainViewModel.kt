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
import com.example.model.AdminModule
import com.example.model.AdminModuleSnapshot
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
import com.example.model.ProductImageUpload
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
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
    private val _adminModules = MutableStateFlow<Map<AdminModule, AdminModuleSnapshot>>(emptyMap())
    val adminModules: StateFlow<Map<AdminModule, AdminModuleSnapshot>> = _adminModules.asStateFlow()
    private var visitorTelemetryJob: Job? = null

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
            repository.setSubscriptions(cachedSubs.map { it.toDomainModel() })

            val cachedReturns = db.orderReturnDao().getAllReturns()
            repository.setReturns(cachedReturns.map { it.toDomainModel() })

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
                startVisitorTelemetry(primaryStore)
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
        _adminModules.value = emptyMap()
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
                    startVisitorTelemetry(revealedStore)
                }
            }
        }
    }

    fun openStoreSwitcher() { _isStoreSwitcherOpen.value = true }
    fun closeStoreSwitcher() { _isStoreSwitcherOpen.value = false }
    fun selectTimeframe(tf: Timeframe) { _selectedTimeframe.value = tf }
    fun setOrderFilter(st: OrderStatus?) { _selectedOrderFilter.value = st }

    /** Carica sempre dati reali dal bridge; nessun elenco amministrativo demo viene mantenuto. */
    fun loadAdminModule(module: AdminModule, forceRefresh: Boolean = false) {
        if (module == AdminModule.ARTICLES && _adminModules.value[AdminModule.TOPICS]?.records.isNullOrEmpty()) {
            loadAdminModule(AdminModule.TOPICS, forceRefresh)
        }
        val existing = _adminModules.value[module]
        if (existing?.isLoading == true) return
        if (!forceRefresh && existing != null && (existing.records.isNotEmpty() || !existing.supported)) return

        val store = uiState.value.currentStore
        if (store == null || store.url.isBlank() || store.apiKey.isBlank()) {
            _adminModules.value = _adminModules.value + (
                module to AdminModuleSnapshot(
                    module = module,
                    supported = true,
                    message = "Configura e seleziona uno store OpenCart prima di aprire ${module.label}."
                )
            )
            return
        }

        _adminModules.value = _adminModules.value + (
            module to (existing ?: AdminModuleSnapshot(module = module)).copy(
                isLoading = true,
                message = ""
            )
        )

        viewModelScope.launch(Dispatchers.IO) {
            val result = apiClient.fetchAdminModule(
                baseUrl = store.url,
                apiKey = store.apiKey,
                username = store.apiUsername,
                module = module
            )
            _adminModules.value = _adminModules.value + (
                module to result.fold(
                    onSuccess = { it.copy(isLoading = false) },
                    onFailure = { error ->
                        AdminModuleSnapshot(
                            module = module,
                            supported = true,
                            isLoading = false,
                            message = error.localizedMessage ?: "Errore durante il caricamento di ${module.label}."
                        )
                    }
                )
            )
        }
    }

    fun updateAdminRecordStatus(module: AdminModule, recordId: String, active: Boolean) {
        val store = uiState.value.currentStore ?: return
        viewModelScope.launch(Dispatchers.IO) {
            apiClient.updateAdminRecordStatus(
                baseUrl = store.url,
                apiKey = store.apiKey,
                username = store.apiUsername,
                module = module,
                recordId = recordId,
                active = active
            ).onSuccess {
                val current = _adminModules.value[module] ?: return@onSuccess
                _adminModules.value = _adminModules.value + (
                    module to current.copy(
                        records = current.records.map { record ->
                            if (record.id == recordId) {
                                record.copy(
                                    active = active,
                                    statusLabel = if (active) "Attivo" else "Disattivato"
                                )
                            } else record
                        },
                        message = "Stato aggiornato sullo store."
                    )
                )
            }.onFailure { error ->
                val current = _adminModules.value[module] ?: AdminModuleSnapshot(module = module)
                _adminModules.value = _adminModules.value + (
                    module to current.copy(
                        message = error.localizedMessage ?: "Aggiornamento dello stato non riuscito."
                    )
                )
            }
        }
    }

    fun requestSensitiveAdminCommand(module: AdminModule, recordId: String, operation: String) {
        val store = uiState.value.currentStore ?: return
        if (module != AdminModule.CUSTOMER_APPROVALS && module != AdminModule.GDPR) return
        viewModelScope.launch(Dispatchers.IO) {
            apiClient.enqueueAdminCommand(
                baseUrl = store.url,
                apiKey = store.apiKey,
                username = store.apiUsername,
                module = module,
                recordId = recordId,
                operation = operation
            ).onSuccess {
                loadAdminModule(module, forceRefresh = true)
            }.onFailure { error ->
                val current = _adminModules.value[module] ?: AdminModuleSnapshot(module = module)
                _adminModules.value = _adminModules.value + (
                    module to current.copy(message = error.localizedMessage ?: "Invio al pannello OpenCart non riuscito.")
                )
            }
        }
    }

    fun updateAdminContent(module: AdminModule, record: com.example.model.AdminRecord) {
        val store = uiState.value.currentStore ?: return
        if (!record.editable) return
        viewModelScope.launch(Dispatchers.IO) {
            apiClient.updateAdminContent(
                baseUrl = store.url,
                apiKey = store.apiKey,
                username = store.apiUsername,
                module = module,
                record = record
            ).onSuccess {
                loadAdminModule(module, forceRefresh = true)
            }.onFailure { error ->
                val current = _adminModules.value[module] ?: AdminModuleSnapshot(module = module)
                _adminModules.value = _adminModules.value + (
                    module to current.copy(message = error.localizedMessage ?: "Modifica editoriale non riuscita.")
                )
            }
        }
    }

    fun createAdminContent(module: AdminModule, record: com.example.model.AdminRecord) {
        val store = uiState.value.currentStore ?: return
        if (module != AdminModule.ARTICLES && module != AdminModule.TOPICS) return
        viewModelScope.launch(Dispatchers.IO) {
            apiClient.createAdminContent(store.url, store.apiKey, store.apiUsername, module, record)
                .onSuccess {
                    loadAdminModule(module, forceRefresh = true)
                    if (module == AdminModule.TOPICS) loadAdminModule(AdminModule.ARTICLES, forceRefresh = true)
                }
                .onFailure { error ->
                    val current = _adminModules.value[module] ?: AdminModuleSnapshot(module = module)
                    _adminModules.value = _adminModules.value + (
                        module to current.copy(message = error.localizedMessage ?: "Creazione CMS non riuscita.")
                    )
                }
        }
    }

    fun addAntispamKeyword(keyword: String) {
        val cleanKeyword = keyword.trim()
        val store = uiState.value.currentStore ?: return
        if (cleanKeyword.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            apiClient.mutateAntispamKeyword(
                baseUrl = store.url,
                apiKey = store.apiKey,
                username = store.apiUsername,
                operation = "add",
                keyword = cleanKeyword
            ).onSuccess {
                loadAdminModule(AdminModule.ANTISPAM, forceRefresh = true)
            }.onFailure { error ->
                val current = _adminModules.value[AdminModule.ANTISPAM]
                    ?: AdminModuleSnapshot(module = AdminModule.ANTISPAM)
                _adminModules.value = _adminModules.value + (
                    AdminModule.ANTISPAM to current.copy(message = error.localizedMessage ?: "Inserimento non riuscito.")
                )
            }
        }
    }

    fun deleteAntispamKeyword(recordId: String) {
        val store = uiState.value.currentStore ?: return
        viewModelScope.launch(Dispatchers.IO) {
            apiClient.mutateAntispamKeyword(
                baseUrl = store.url,
                apiKey = store.apiKey,
                username = store.apiUsername,
                operation = "delete",
                recordId = recordId
            ).onSuccess {
                loadAdminModule(AdminModule.ANTISPAM, forceRefresh = true)
            }.onFailure { error ->
                val current = _adminModules.value[AdminModule.ANTISPAM]
                    ?: AdminModuleSnapshot(module = AdminModule.ANTISPAM)
                _adminModules.value = _adminModules.value + (
                    AdminModule.ANTISPAM to current.copy(message = error.localizedMessage ?: "Eliminazione non riuscita.")
                )
            }
        }
    }
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
            val telemetryRes = apiClient.fetchVisitorTelemetry(url, apiKey, username)

            var orderCount = 0
            var prodCount = 0
            var catCount = 0
            var subsCount = 0
            var retCount = 0

            telemetryRes.onSuccess(repository::setVisitorStats)

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
                            minQuantityAlert = prod.minQuantityAlert,
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

            if (retRes.isSuccess) {
                val liveReturns = retRes.getOrNull() ?: emptyList()
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
                            minQuantityAlert = prod.minQuantityAlert,
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

            val retRes = apiClient.fetchReturns(url, apiKey, username, limit = 50)
            if (retRes.isSuccess) {
                val liveReturns = retRes.getOrNull() ?: emptyList()
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

            apiClient.fetchVisitorTelemetry(url, apiKey, username)
                .onSuccess(repository::setVisitorStats)

            ordersRes.isSuccess || prodRes.isSuccess || catRes.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    private fun startVisitorTelemetry(store: Store) {
        visitorTelemetryJob?.cancel()
        repository.setVisitorStats(VisitorRealtimeStats())
        if (store.url.isBlank() || store.apiKey.isBlank()) return

        visitorTelemetryJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                apiClient.fetchVisitorTelemetry(store.url, store.apiKey, store.apiUsername)
                    .onSuccess(repository::setVisitorStats)
                delay(30_000)
            }
        }
    }

    fun updateSubscriptionStatus(subscriptionId: String, newStatus: SubscriptionStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            val store = uiState.value.currentStore
            if (store == null || store.url.isBlank()) {
                showOperationMessage("Abbonamento non aggiornato: nessun negozio configurato.")
                return@launch
            }
            val result = apiClient.updateSubscriptionStatus(store.url, store.apiKey, subscriptionId, newStatus.name, store.apiUsername)
            if (result.getOrDefault(false)) {
                repository.updateSubscriptionStatus(subscriptionId, newStatus)
                db.subscriptionDao().updateSubscriptionStatus(subscriptionId, newStatus.name)
                showOperationMessage("Stato abbonamento aggiornato sullo store.")
            } else {
                showOperationMessage("Abbonamento non aggiornato: ${result.exceptionOrNull()?.message ?: "risposta rifiutata dal bridge"}.")
            }
        }
    }

    fun updateReturnStatus(returnId: String, newStatus: ReturnStatus, newAction: String = "In lavorazione") {
        viewModelScope.launch(Dispatchers.IO) {
            val store = uiState.value.currentStore
            if (store == null || store.url.isBlank()) {
                showOperationMessage("Reso non aggiornato: nessun negozio configurato.")
                return@launch
            }
            val statusId = when (newStatus) {
                ReturnStatus.PENDING -> 1
                ReturnStatus.AWAITING_PRODUCTS -> 2
                ReturnStatus.IN_INSPECTION -> 3
                ReturnStatus.COMPLETE_REFUNDED, ReturnStatus.COMPLETE_REPLACED -> 4
                ReturnStatus.DENIED -> 5
            }
            val result = apiClient.updateReturnStatus(store.url, store.apiKey, returnId, statusId, newAction, store.apiUsername)
            if (result.getOrDefault(false)) {
                repository.updateReturnStatus(returnId, newStatus, newAction)
                db.orderReturnDao().updateReturnStatus(returnId, newStatus.name, newAction)
                showOperationMessage("Stato reso aggiornato sullo store.")
            } else {
                showOperationMessage("Reso non aggiornato: ${result.exceptionOrNull()?.message ?: "risposta rifiutata dal bridge"}.")
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            val store = uiState.value.currentStore
            if (store == null || store.url.isBlank()) {
                showOperationMessage("Ordine non aggiornato: nessun negozio configurato.")
                return@launch
            }
            val result = apiClient.updateOrderStatus(store.url, store.apiKey, orderId, newStatus.toOpenCartStatusId(), "Stato aggiornato da CartAdmin App", store.apiUsername)
            if (result.getOrDefault(false)) {
                repository.updateOrderStatus(orderId, newStatus)
                offlineOrderRepository.updateOrderStatus(orderId, newStatus)
                showOperationMessage("Stato ordine aggiornato sullo store.")
            } else {
                showOperationMessage("Ordine non aggiornato: ${result.exceptionOrNull()?.message ?: "risposta rifiutata dal bridge"}.")
            }
        }
    }

    fun updateOrderNotes(orderId: String, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val order = uiState.value.orders.find { it.id == orderId }
            val store = uiState.value.currentStore
            if (order == null || store == null || store.url.isBlank()) {
                showOperationMessage("Note non aggiornate: ordine o negozio non disponibile.")
                return@launch
            }
            val result = apiClient.updateOrderStatus(store.url, store.apiKey, orderId, order.status.toOpenCartStatusId(), notes, store.apiUsername)
            if (result.getOrDefault(false)) {
                repository.updateOrderNotes(orderId, notes)
                offlineOrderRepository.updateOrderNotes(orderId, notes)
                showOperationMessage("Note ordine aggiornate sullo store.")
            } else {
                showOperationMessage("Note non aggiornate: ${result.exceptionOrNull()?.message ?: "risposta rifiutata dal bridge"}.")
            }
        }
    }

    fun updateOrder(orderId: String, newStatus: OrderStatus, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val store = uiState.value.currentStore
            if (store == null || store.url.isBlank()) {
                showOperationMessage("Ordine non aggiornato: nessun negozio configurato.")
                return@launch
            }
            val result = apiClient.updateOrderStatus(store.url, store.apiKey, orderId, newStatus.toOpenCartStatusId(), notes, store.apiUsername)
            if (result.getOrDefault(false)) {
                repository.updateOrderStatusAndNotes(orderId, newStatus, notes)
                offlineOrderRepository.updateOrderStatusAndNotes(orderId, newStatus, notes)
                showOperationMessage("Ordine aggiornato sullo store.")
            } else {
                showOperationMessage("Ordine non aggiornato: ${result.exceptionOrNull()?.message ?: "risposta rifiutata dal bridge"}.")
            }
        }
    }

    private fun OrderStatus.toOpenCartStatusId(): Int = when (this) {
        OrderStatus.PENDING -> 1
        OrderStatus.PROCESSING -> 2
        OrderStatus.SHIPPED -> 3
        OrderStatus.DELIVERED, OrderStatus.COMPLETE -> 5
        OrderStatus.CANCELLED -> 7
        else -> 2
    }

    fun updateProductStock(productId: String, delta: Int) {
        val prod = uiState.value.products.find { it.id == productId } ?: return
        val newQty = (prod.quantity + delta).coerceAtLeast(0)
        viewModelScope.launch(Dispatchers.IO) {
            val store = uiState.value.currentStore
            if (store == null || store.url.isBlank()) {
                showOperationMessage("Quantità non aggiornata: nessun negozio configurato.")
                return@launch
            }
            val result = apiClient.updateProductStock(store.url, store.apiKey, productId, newQty, store.apiUsername)
            if (result.getOrDefault(false)) {
                repository.updateProductStock(productId, newQty)
                offlineCatalogRepository.updateProductStock(productId, newQty)
                showOperationMessage("Quantità aggiornata sullo store: $newQty.")
            } else {
                showOperationMessage("Quantità non aggiornata: ${result.exceptionOrNull()?.message ?: "risposta rifiutata dal bridge"}.")
            }
        }
    }

    fun setDirectProductStock(productId: String, newQty: Int) {
        val clamped = newQty.coerceAtLeast(0)
        viewModelScope.launch(Dispatchers.IO) {
            val store = uiState.value.currentStore
            if (store == null || store.url.isBlank()) {
                showOperationMessage("Quantità non aggiornata: nessun negozio configurato.")
                return@launch
            }
            val result = apiClient.updateProductStock(store.url, store.apiKey, productId, clamped, store.apiUsername)
            if (result.getOrDefault(false)) {
                repository.updateProductStock(productId, clamped)
                offlineCatalogRepository.updateProductStock(productId, clamped)
                showOperationMessage("Quantità aggiornata sullo store: $clamped.")
            } else {
                showOperationMessage("Quantità non aggiornata: ${result.exceptionOrNull()?.message ?: "risposta rifiutata dal bridge"}.")
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
        minQuantityAlert: Int,
        category: String,
        description: String = "",
        status: Boolean = true,
        image: ProductImageUpload? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val store = uiState.value.currentStore
            if (store == null || store.url.isBlank()) {
                showOperationMessage("Prodotto non creato: nessun negozio configurato.")
                return@launch
            }
            val draft = Product(
                id = "",
                name = name.trim(),
                model = model.trim(),
                sku = sku.trim(),
                price = price.coerceAtLeast(0.0),
                specialPrice = null,
                quantity = quantity.coerceAtLeast(0),
                minQuantityAlert = minQuantityAlert.coerceAtLeast(1),
                category = category.trim(),
                description = description.trim(),
                status = status
            )
            val result = apiClient.createProduct(store.url, store.apiKey, draft, image, store.apiUsername)
            val created = result.getOrNull()
            if (created != null) {
                repository.insertProduct(created)
                offlineCatalogRepository.saveProduct(created, store.id)
                showOperationMessage("Prodotto “${created.name}” creato sullo store.")
            } else {
                showOperationMessage("Prodotto non creato: ${result.exceptionOrNull()?.message ?: "risposta rifiutata dal bridge"}.")
            }
        }
    }

    fun updateProduct(product: Product, image: ProductImageUpload? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val store = uiState.value.currentStore
            if (store == null || store.url.isBlank()) {
                showOperationMessage("Prodotto non aggiornato: nessun negozio configurato.")
                return@launch
            }
            val result = apiClient.updateProduct(store.url, store.apiKey, product, image, store.apiUsername)
            if (result.getOrDefault(false)) {
                repository.updateProduct(product)
                offlineCatalogRepository.saveProduct(product, store.id)
                showOperationMessage("Prodotto “${product.name}” aggiornato sullo store.")
            } else {
                showOperationMessage("Prodotto non aggiornato: ${result.exceptionOrNull()?.message ?: "risposta rifiutata dal bridge"}.")
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val store = uiState.value.currentStore
            if (store == null || store.url.isBlank()) {
                showOperationMessage("Prodotto non eliminato: nessun negozio configurato.")
                return@launch
            }
            val result = apiClient.deleteProduct(store.url, store.apiKey, productId, store.apiUsername)
            if (result.getOrDefault(false)) {
                repository.deleteProduct(productId)
                offlineCatalogRepository.deleteProduct(productId)
                showOperationMessage("Prodotto eliminato dallo store.")
            } else {
                showOperationMessage("Prodotto non eliminato: ${result.exceptionOrNull()?.message ?: "risposta rifiutata dal bridge"}.")
            }
        }
    }

    fun toggleProductStatus(productId: String) {
        val prod = uiState.value.products.find { it.id == productId } ?: return
        updateProduct(prod.copy(status = !prod.status))
    }

    fun addNewCategory(name: String, description: String = "", sortOrder: Int = 0, status: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val store = uiState.value.currentStore
            if (store == null || store.url.isBlank()) {
                showOperationMessage("Categoria non creata: nessun negozio configurato.")
                return@launch
            }
            val draft = Category(
                id = "",
                name = name.trim(),
                description = description.trim(),
                productsCount = 0,
                status = status,
                sortOrder = sortOrder.coerceAtLeast(0)
            )
            val result = apiClient.createCategory(store.url, store.apiKey, draft, store.apiUsername)
            val created = result.getOrNull()
            if (created != null) {
                repository.insertCategory(created)
                offlineCatalogRepository.saveCategory(created, store.id)
                showOperationMessage("Categoria “${created.name}” creata sullo store.")
            } else {
                showOperationMessage("Categoria non creata: ${result.exceptionOrNull()?.message ?: "risposta rifiutata dal bridge"}.")
            }
        }
    }

    fun updateCategory(categoryId: String, name: String, description: String, sortOrder: Int, status: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val store = uiState.value.currentStore
            val current = uiState.value.categories.find { it.id == categoryId }
            if (store == null || store.url.isBlank() || current == null) {
                showOperationMessage("Categoria non aggiornata: negozio o categoria non disponibili.")
                return@launch
            }
            val updated = current.copy(
                name = name.trim(),
                description = description.trim(),
                sortOrder = sortOrder.coerceAtLeast(0),
                status = status
            )
            val result = apiClient.updateCategory(store.url, store.apiKey, updated, store.apiUsername)
            if (result.getOrDefault(false)) {
                repository.updateCategory(updated.id, updated.name, updated.description, updated.sortOrder, updated.status)
                offlineCatalogRepository.saveCategory(updated, store.id)
                showOperationMessage("Categoria “${updated.name}” aggiornata sullo store.")
            } else {
                showOperationMessage("Categoria non aggiornata: ${result.exceptionOrNull()?.message ?: "risposta rifiutata dal bridge"}.")
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val store = uiState.value.currentStore
            if (store == null || store.url.isBlank()) {
                showOperationMessage("Categoria non eliminata: nessun negozio configurato.")
                return@launch
            }
            val result = apiClient.deleteCategory(store.url, store.apiKey, categoryId, store.apiUsername)
            if (result.getOrDefault(false)) {
                repository.deleteCategory(categoryId)
                offlineCatalogRepository.deleteCategory(categoryId)
                showOperationMessage("Categoria eliminata dallo store; i prodotti restano disponibili senza questa associazione.")
            } else {
                showOperationMessage("Categoria non eliminata: ${result.exceptionOrNull()?.message ?: "risposta rifiutata dal bridge"}.")
            }
        }
    }

    fun toggleCategoryStatus(categoryId: String) {
        val category = uiState.value.categories.find { it.id == categoryId } ?: return
        updateCategory(category.id, category.name, category.description, category.sortOrder, !category.status)
    }

    private suspend fun showOperationMessage(message: String) {
        _syncSuccessMessage.value = message
        delay(4_000)
        if (_syncSuccessMessage.value == message) {
            _syncSuccessMessage.value = null
        }
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
            db.subscriptionDao().clearAllSubscriptions()
            db.orderReturnDao().clearAllReturns()
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
                    repository.stores.value.find { it.id == storeId }?.let(::startVisitorTelemetry)
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
                val createdStore = repository.addStore(name, url, version, username, key)
                _isStoreSwitcherOpen.value = false
                if (key.isNotBlank()) {
                    syncDataFromOpenCart(url, key, username)
                    startVisitorTelemetry(createdStore)
                }
            } catch (_: CredentialProtectionException) {
                _syncSuccessMessage.value =
                    "Store non salvato: TEE o StrongBox hardware non disponibile."
            }
        }
    }

    fun deleteStore(storeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            visitorTelemetryJob?.cancel()
            repository.setVisitorStats(VisitorRealtimeStats())
            repository.deleteStore(storeId)
        }
    }
}
