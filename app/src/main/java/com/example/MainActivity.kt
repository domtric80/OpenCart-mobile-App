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
import com.example.ui.MainViewModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.HeaderSection
import com.example.ui.components.NavigationTab
import com.example.ui.components.OrderDetailSheet
import com.example.ui.components.OrdersSubSectionSheet
import com.example.ui.components.StoreSwitcherSheet
import com.example.ui.screens.AuditScreen
import com.example.ui.screens.CatalogScreen
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
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var isOrdersMenuOpen by remember { mutableStateOf(false) }

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
                selectedTab = currentTab,
                onTabSelected = { selected ->
                    currentTab = selected
                },
                onOrdersTabMenuClick = {
                    isOrdersMenuOpen = true
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
                    CatalogScreen(
                        products = uiState.products,
                        categories = uiState.categories,
                        onUpdateStock = { productId, delta ->
                            viewModel.updateProductStock(productId, delta)
                        },
                        onSetDirectStock = { productId, newQty ->
                            viewModel.setDirectProductStock(productId, newQty)
                        },
                        onAddNewProduct = { name, model, sku, price, special, qty, minAlert, category, desc, status ->
                            viewModel.addNewProduct(name, model, sku, price, special, qty, category, desc)
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
                        }
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
                        onTriggerSync = { url, key, user ->
                            viewModel.triggerSync(url, key, user)
                        },
                        onClearDummyData = {
                            viewModel.clearDummyData()
                        }
                    )
                }

                NavigationTab.LICENSE -> {
                    LicenseScreen()
                }
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
        }
    }
}
