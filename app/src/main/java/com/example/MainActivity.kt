package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.auth.AuthLockScreen
import com.example.auth.SecurityManager
import com.example.model.Order
import com.example.model.AdminModule
import com.example.ui.MainViewModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.HeaderSection
import com.example.ui.components.NavigationTab
import com.example.ui.components.NavigationMenuItem
import com.example.ui.components.NavigationMenuSheet
import com.example.ui.components.OrderDetailSheet
import com.example.ui.components.OrdersSubSectionSheet
import com.example.ui.components.StoreSwitcherSheet
import com.example.ui.screens.AuditScreen
import com.example.ui.screens.AdminModuleScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.CatalogTab
import com.example.ui.screens.ConfigScreen
import com.example.ui.screens.DashboardHomeScreen
import com.example.ui.screens.LicenseScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.VisitorsRealtimeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContainer(viewModel = viewModel, context = this)
            }
        }
    }
}

@Composable
fun MainAppContainer(
    viewModel: MainViewModel,
    context: Context
) {
    val securityManager = remember { SecurityManager(context) }
    var isUnlocked by remember {
        mutableStateOf(!securityManager.evaluateAuthStatus(true).isLocked)
    }

    if (!isUnlocked) {
        AuthLockScreen(
            securityManager = securityManager,
            onUnlockSuccess = {
                isUnlocked = true
            }
        )
    } else {
        MainAppContent(viewModel = viewModel)
    }
}

@Composable
fun MainAppContent(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val adminModules by viewModel.adminModules.collectAsState()
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var isOrdersMenuOpen by remember { mutableStateOf(false) }
    var openNavigationMenu by remember { mutableStateOf<NavigationTab?>(null) }
    var catalogTab by remember { mutableStateOf(CatalogTab.PRODUCTS) }
    var selectedCatalogModule by remember { mutableStateOf<AdminModule?>(null) }
    var selectedAdminModule by remember { mutableStateOf(AdminModule.CUSTOMERS) }

    val selectedBottomTab = when (currentTab) {
        NavigationTab.HOME -> NavigationTab.HOME
        NavigationTab.ORDERS -> NavigationTab.ORDERS
        NavigationTab.CATALOG -> NavigationTab.CATALOG
        NavigationTab.CUSTOMERS -> NavigationTab.CUSTOMERS
        else -> NavigationTab.MORE
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                HeaderSection(
                    onMenuClick = {
                        viewModel.openStoreSwitcher()
                    },
                    onAvatarClick = {
                        currentTab = NavigationTab.CONFIG
                    },
                    onSyncClick = {
                        viewModel.triggerSync()
                    }
                )
            }
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedBottomTab,
                onTabSelected = { selected ->
                    if (selected == NavigationTab.HOME) currentTab = selected
                },
                onMenuTabClick = { selected ->
                    when (selected) {
                        NavigationTab.ORDERS -> {
                            currentTab = NavigationTab.ORDERS
                            isOrdersMenuOpen = true
                        }
                        NavigationTab.CATALOG -> {
                            currentTab = NavigationTab.CATALOG
                            openNavigationMenu = NavigationTab.CATALOG
                        }
                        NavigationTab.CUSTOMERS -> {
                            currentTab = NavigationTab.CUSTOMERS
                            openNavigationMenu = NavigationTab.CUSTOMERS
                        }
                        NavigationTab.MORE -> openNavigationMenu = NavigationTab.MORE
                        else -> Unit
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.HOME -> {
                    DashboardHomeScreen(
                        currentStore = uiState.currentStore,
                        activities = uiState.activities,
                        salesMetrics = uiState.salesMetrics,
                        visitorStats = uiState.visitorStats,
                        selectedTimeframe = uiState.selectedTimeframe,
                        syncMessage = uiState.syncSuccessMessage,
                        onStoreClick = {
                            viewModel.openStoreSwitcher()
                        },
                        onSelectTimeframe = { timeframe ->
                            viewModel.selectTimeframe(timeframe)
                        },
                        onPendingOrdersClick = {
                            viewModel.selectOrdersSubSection(com.example.model.OrdersSubSection.ORDERS)
                            currentTab = NavigationTab.ORDERS
                        },
                        onStockAlertsClick = {
                            selectedCatalogModule = null
                            catalogTab = CatalogTab.PRODUCTS
                            currentTab = NavigationTab.CATALOG
                        },
                        onAovClick = {
                            viewModel.selectOrdersSubSection(com.example.model.OrdersSubSection.ORDERS)
                            currentTab = NavigationTab.ORDERS
                        },
                        onVisitorsClick = {
                            currentTab = NavigationTab.VISITORS
                        },
                        onViewAllActivitiesClick = {
                            viewModel.selectOrdersSubSection(com.example.model.OrdersSubSection.ORDERS)
                            currentTab = NavigationTab.ORDERS
                        },
                        onActivityClick = { activity ->
                            if (activity.orderId != null) {
                                val order = uiState.orders.find { it.id == activity.orderId }
                                if (order != null) {
                                    viewModel.selectOrderForDetail(order)
                                }
                            }
                        }
                    )
                }

                NavigationTab.ORDERS -> {
                    OrdersScreen(
                        orders = uiState.orders,
                        selectedFilter = uiState.selectedOrderFilter,
                        onSelectFilter = { filter ->
                            viewModel.setOrderFilter(filter)
                        },
                        onOrderClick = { order ->
                            viewModel.selectOrderForDetail(order)
                        },
                        subSection = uiState.selectedOrdersSubSection,
                        onSubSectionChange = { subSection ->
                            viewModel.selectOrdersSubSection(subSection)
                        },
                        onOpenSubSectionMenu = {
                            isOrdersMenuOpen = true
                        },
                        subscriptions = uiState.subscriptions,
                        selectedSubscriptionFilter = uiState.selectedSubscriptionFilter,
                        onSelectSubscriptionFilter = { filter ->
                            viewModel.setSubscriptionFilter(filter)
                        },
                        onUpdateSubscriptionStatus = { subId, status ->
                            viewModel.updateSubscriptionStatus(subId, status)
                        },
                        returns = uiState.returns,
                        selectedReturnFilter = uiState.selectedReturnFilter,
                        onSelectReturnFilter = { filter ->
                            viewModel.setReturnFilter(filter)
                        },
                        onUpdateReturnStatus = { returnId, status, action ->
                            viewModel.updateReturnStatus(returnId, status, action)
                        }
                    )
                }

                NavigationTab.CATALOG -> {
                    val managementModule = selectedCatalogModule
                    if (managementModule != null) {
                        AdminModuleScreen(
                            module = managementModule,
                            snapshot = adminModules[managementModule],
                            onRefresh = { viewModel.loadAdminModule(managementModule, forceRefresh = true) },
                            onStatusChange = { id, active -> viewModel.updateAdminRecordStatus(managementModule, id, active) },
                            onAddAntispam = viewModel::addAntispamKeyword,
                            onDeleteAntispam = viewModel::deleteAntispamKeyword,
                            onContentUpdate = { record -> viewModel.updateAdminContent(managementModule, record) },
                            onSensitiveAction = { id, operation -> viewModel.requestSensitiveAdminCommand(managementModule, id, operation) }
                        )
                    } else CatalogScreen(
                        products = uiState.products,
                        categories = uiState.categories,
                        onUpdateStock = { productId, delta ->
                            viewModel.updateProductStock(productId, delta)
                        },
                        onSetDirectStock = { productId, newQty ->
                            viewModel.setDirectProductStock(productId, newQty)
                        },
                        onAddNewProduct = { name, model, sku, price, special, qty, minAlert, category, desc, status ->
                            viewModel.addNewProduct(name, model, sku, price, special, qty, minAlert, category, desc, status)
                        },
                        onUpdateProduct = { product ->
                            viewModel.updateProduct(product)
                        },
                        onDeleteProduct = { productId ->
                            viewModel.deleteProduct(productId)
                        },
                        onToggleProductStatus = { productId ->
                            viewModel.toggleProductStatus(productId)
                        },
                        onAddNewCategory = { name, desc, sortOrder, status ->
                            viewModel.addNewCategory(name, desc, sortOrder, status)
                        },
                        onUpdateCategory = { categoryId, name, desc, sortOrder, status ->
                            viewModel.updateCategory(categoryId, name, desc, sortOrder, status)
                        },
                        onDeleteCategory = { categoryId ->
                            viewModel.deleteCategory(categoryId)
                        },
                        onToggleCategoryStatus = { categoryId ->
                            viewModel.toggleCategoryStatus(categoryId)
                        },
                        requestedTab = catalogTab,
                        operationMessage = uiState.syncSuccessMessage
                    )
                }

                NavigationTab.CUSTOMERS,
                NavigationTab.CMS -> {
                    AdminModuleScreen(
                        module = selectedAdminModule,
                        snapshot = adminModules[selectedAdminModule],
                        onRefresh = { viewModel.loadAdminModule(selectedAdminModule, forceRefresh = true) },
                        onStatusChange = { id, active -> viewModel.updateAdminRecordStatus(selectedAdminModule, id, active) },
                        onAddAntispam = viewModel::addAntispamKeyword,
                        onDeleteAntispam = viewModel::deleteAntispamKeyword,
                        onContentUpdate = { record -> viewModel.updateAdminContent(selectedAdminModule, record) },
                        onSensitiveAction = { id, operation -> viewModel.requestSensitiveAdminCommand(selectedAdminModule, id, operation) }
                    )
                }

                NavigationTab.VISITORS -> {
                    VisitorsRealtimeScreen(
                        visitorStats = uiState.visitorStats,
                        currentStore = uiState.currentStore
                    )
                }

                NavigationTab.AUDIT -> {
                    AuditScreen(
                        auditLogs = uiState.auditLogs,
                        onClearLogs = {
                            viewModel.clearAuditLogs()
                        }
                    )
                }

                NavigationTab.CONFIG -> {
                    ConfigScreen(
                        currentStore = uiState.currentStore,
                        isTestingConnection = uiState.isTestingConnection,
                        connectionResult = uiState.connectionResult,
                        onTestConnection = { url, user, key ->
                            viewModel.testOpenCartConnection(url, user, key)
                        },
                        onSaveStoreCredentials = { storeId, name, url, user, key, ver ->
                            viewModel.saveStoreCredentials(storeId, name, url, user, key, ver)
                        },
                        onAddStore = { name, url, user, key, ver ->
                            viewModel.addStore(name, url, ver, user, key)
                        },
                        onTriggerSync = { url, key, user ->
                            viewModel.triggerSync(url, key, user)
                        },
                        onClearDummyData = {
                            viewModel.clearDummyData()
                        },
                        onOpenAudit = { currentTab = NavigationTab.AUDIT },
                        onOpenLicense = { currentTab = NavigationTab.LICENSE }
                    )
                }

                NavigationTab.LICENSE -> {
                    LicenseScreen()
                }

                NavigationTab.MORE -> Unit
            }

            // Store Switcher Modal Sheet
            if (uiState.isStoreSwitcherOpen) {
                StoreSwitcherSheet(
                    stores = uiState.stores,
                    currentStoreId = uiState.currentStore?.id,
                    onSelectStore = { storeId ->
                        viewModel.selectStore(storeId)
                    },
                    onAddStore = { name, url, version ->
                        viewModel.addStore(name, url, version)
                    },
                    onDeleteStore = { storeId ->
                        viewModel.deleteStore(storeId)
                    },
                    onDismiss = {
                        viewModel.closeStoreSwitcher()
                    }
                )
            }

            // Order Detail Modal Sheet utilizing Room Local Cache
            val currentDetail = uiState.selectedOrderDetail ?: uiState.selectedOrderForDetail?.let {
                com.example.model.OrderDetail(
                    order = it,
                    items = emptyList(),
                    subtotal = (it.total - 6.90).coerceAtLeast(0.0),
                    shippingCost = 6.90,
                    taxAmount = (it.total - 6.90).coerceAtLeast(0.0) * 0.22,
                    grandTotal = it.total
                )
            }

            currentDetail?.let { orderDetail ->
                OrderDetailSheet(
                    orderDetail = orderDetail,
                    onStatusChange = { newStatus ->
                        viewModel.updateOrderStatus(orderDetail.order.id, newStatus)
                    },
                    onSaveNotes = { newNotes ->
                        viewModel.updateOrderNotes(orderDetail.order.id, newNotes)
                    },
                    onUpdateOrder = { newStatus, newNotes ->
                        viewModel.updateOrder(orderDetail.order.id, newStatus, newNotes)
                    },
                    onDismiss = {
                        viewModel.clearSelectedOrder()
                    }
                )
            }

            // Orders Sub-Sections Bottom Sheet (Ordini, Abbonamenti, Resi)
            if (isOrdersMenuOpen) {
                OrdersSubSectionSheet(
                    selectedSubSection = uiState.selectedOrdersSubSection,
                    onSelectSubSection = { sub ->
                        viewModel.selectOrdersSubSection(sub)
                        currentTab = NavigationTab.ORDERS
                        isOrdersMenuOpen = false
                    },
                    onDismiss = {
                        isOrdersMenuOpen = false
                    },
                    ordersCount = uiState.orders.size,
                    subscriptionsCount = uiState.subscriptions.count { it.status == com.example.model.SubscriptionStatus.ACTIVE },
                    returnsCount = uiState.returns.count { it.status == com.example.model.ReturnStatus.PENDING || it.status == com.example.model.ReturnStatus.AWAITING_PRODUCTS }
                )
            }

            when (openNavigationMenu) {
                NavigationTab.CATALOG -> NavigationMenuSheet(
                    title = "Catalogo",
                    description = "Scegli la sezione del catalogo OpenCart",
                    items = listOf(
                        NavigationMenuItem("products", "Prodotti", "Prezzi, quantità e schede prodotto", Icons.Default.Inventory2),
                        NavigationMenuItem("categories", "Categorie", "Organizzazione del catalogo", Icons.Default.Category),
                        NavigationMenuItem(AdminModule.SUBSCRIPTION_PLANS.apiKey, AdminModule.SUBSCRIPTION_PLANS.label, AdminModule.SUBSCRIPTION_PLANS.description, Icons.Default.Payments),
                        NavigationMenuItem(AdminModule.PAGES.apiKey, AdminModule.PAGES.label, AdminModule.PAGES.description, Icons.Default.Description),
                        NavigationMenuItem(AdminModule.REVIEWS.apiKey, AdminModule.REVIEWS.label, AdminModule.REVIEWS.description, Icons.Default.RateReview)
                    ),
                    onSelect = { item ->
                        when (item.key) {
                            "products" -> {
                                selectedCatalogModule = null
                                catalogTab = CatalogTab.PRODUCTS
                            }
                            "categories" -> {
                                selectedCatalogModule = null
                                catalogTab = CatalogTab.CATEGORIES
                            }
                            else -> AdminModule.entries.find { it.apiKey == item.key }?.let { module ->
                                selectedCatalogModule = module
                                viewModel.loadAdminModule(module)
                            }
                        }
                        currentTab = NavigationTab.CATALOG
                        openNavigationMenu = null
                    },
                    onDismiss = { openNavigationMenu = null }
                )

                NavigationTab.CUSTOMERS -> NavigationMenuSheet(
                    title = "Clienti",
                    description = "Account, approvazioni e richieste privacy reali",
                    items = listOf(
                        NavigationMenuItem(AdminModule.CUSTOMERS.apiKey, AdminModule.CUSTOMERS.label, AdminModule.CUSTOMERS.description, Icons.Default.People),
                        NavigationMenuItem(AdminModule.CUSTOMER_APPROVALS.apiKey, AdminModule.CUSTOMER_APPROVALS.label, AdminModule.CUSTOMER_APPROVALS.description, Icons.Default.PersonAdd),
                        NavigationMenuItem(AdminModule.GDPR.apiKey, AdminModule.GDPR.label, AdminModule.GDPR.description, Icons.Default.Policy)
                    ),
                    onSelect = { item ->
                        AdminModule.entries.find { it.apiKey == item.key }?.let { module ->
                            selectedAdminModule = module
                            viewModel.loadAdminModule(module)
                        }
                        currentTab = NavigationTab.CUSTOMERS
                        openNavigationMenu = null
                    },
                    onDismiss = { openNavigationMenu = null }
                )

                NavigationTab.MORE -> NavigationMenuSheet(
                    title = "Altre funzioni",
                    description = "Telemetria, contenuti e configurazione",
                    items = listOf(
                        NavigationMenuItem("traffic", "Traffic", "Visitatori online e sorgenti disponibili", Icons.Default.Sensors),
                        NavigationMenuItem("cms", "CMS", "Articoli, argomenti, commenti e antispam", Icons.Default.Article),
                        NavigationMenuItem(
                            "config",
                            "Configurazione",
                            "Store, sicurezza, Audit e Licenza",
                            Icons.Default.Settings,
                            badge = "v${BuildConfig.VERSION_NAME}"
                        )
                    ),
                    onSelect = { item ->
                        when (item.key) {
                            "traffic" -> currentTab = NavigationTab.VISITORS
                            "cms" -> {
                                currentTab = NavigationTab.CMS
                                openNavigationMenu = NavigationTab.CMS
                                return@NavigationMenuSheet
                            }
                            "config" -> currentTab = NavigationTab.CONFIG
                        }
                        openNavigationMenu = null
                    },
                    onDismiss = { openNavigationMenu = null }
                )

                NavigationTab.CMS -> NavigationMenuSheet(
                    title = "CMS",
                    description = "Gestione editoriale nativa di OpenCart 4",
                    items = listOf(
                        NavigationMenuItem(AdminModule.ARTICLES.apiKey, AdminModule.ARTICLES.label, AdminModule.ARTICLES.description, Icons.Default.Article),
                        NavigationMenuItem(AdminModule.TOPICS.apiKey, AdminModule.TOPICS.label, AdminModule.TOPICS.description, Icons.Default.Forum),
                        NavigationMenuItem(AdminModule.COMMENTS.apiKey, AdminModule.COMMENTS.label, AdminModule.COMMENTS.description, Icons.Default.Comment),
                        NavigationMenuItem(AdminModule.ANTISPAM.apiKey, AdminModule.ANTISPAM.label, AdminModule.ANTISPAM.description, Icons.Default.Security)
                    ),
                    onSelect = { item ->
                        AdminModule.entries.find { it.apiKey == item.key }?.let { module ->
                            selectedAdminModule = module
                            viewModel.loadAdminModule(module)
                        }
                        currentTab = NavigationTab.CMS
                        openNavigationMenu = null
                    },
                    onDismiss = { openNavigationMenu = null }
                )

                else -> Unit
            }
        }
    }
}
