package com.example.model

enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    ERROR
}

data class Store(
    val id: String,
    val name: String,
    val url: String,
    val currency: String = "€",
    val version: String = "OpenCart 3.0.3.8",
    val apiUsername: String = "api_admin_sync",
    val apiKey: String = "",
    val apiToken: String? = null,
    val isConnected: Boolean = true,
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val lastSyncTime: String = "Pochi secondi fa",
    val pendingOrdersCount: Int = 0,
    val stockAlertsCount: Int = 0,
    val todayRevenue: Double = 0.0,
    val revenueGrowthPercent: Double = 0.0
)

data class ActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val timestamp: String,
    val type: ActivityType,
    val amount: Double? = null,
    val orderId: String? = null
)

enum class ActivityType {
    USER_REGISTRATION,
    ORDER_PAYMENT,
    STOCK_ALERT,
    REFUND_REQUEST,
    REVIEW_ADDED
}

data class Order(
    val id: String,
    val orderNumber: String,
    val customerName: String,
    val customerEmail: String,
    val total: Double,
    val status: OrderStatus,
    val dateAdded: String,
    val itemsCount: Int,
    val shippingMethod: String = "Corriere Espresso (GLS)",
    val paymentMethod: String = "Carta di Credito / Stripe",
    val notes: String? = null
)

data class OrderItem(
    val id: String,
    val orderId: String,
    val productId: String,
    val name: String,
    val model: String,
    val quantity: Int,
    val price: Double,
    val total: Double
)

data class OrderDetail(
    val order: Order,
    val items: List<OrderItem> = emptyList(),
    val customerPhone: String = "+39 347 889 1234",
    val shippingAddress: String = "Via Roma 42, 20121 Milano (MI), Italia",
    val paymentAddress: String = "Via Roma 42, 20121 Milano (MI), Italia",
    val subtotal: Double = 0.0,
    val shippingCost: Double = 6.90,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val customerNotes: String? = null,
    val adminNotes: String? = null,
    val isFromLocalCache: Boolean = true,
    val cachedTimestamp: String = "Locale (Room DB)"
)

enum class OrderStatus(val label: String, val englishLabel: String) {
    PENDING("In attesa", "Pending"),
    PROCESSING("In lavorazione", "Processing"),
    CONFIRMED("Confermato", "Confirmed"),
    SHIPPED("Spedito", "Shipped"),
    DELIVERED("Consegnato", "Delivered"),
    COMPLETE("Completato", "Delivered"),
    CANCELLED("Annullato", "Cancelled")
}

data class Category(
    val id: String,
    val name: String,
    val description: String = "",
    val productsCount: Int = 0,
    val status: Boolean = true,
    val sortOrder: Int = 0
)

data class Product(
    val id: String,
    val name: String,
    val model: String,
    val sku: String,
    val price: Double,
    val specialPrice: Double? = null,
    val quantity: Int,
    val minQuantityAlert: Int = 5,
    val category: String,
    val description: String = "",
    val status: Boolean = true
)

data class HourlySalesPoint(
    val hourLabel: String,
    val revenue: Double,
    val orderCount: Int,
    val isCurrentPeak: Boolean = false
)

data class SalesMetrics(
    val totalRevenue: Double,
    val revenueGrowthPercent: Double,
    val orderCount: Int,
    val orderGrowthPercent: Double,
    val completedOrdersCount: Int,
    val pendingOrdersCount: Int,
    val averageOrderValue: Double,
    val aovGrowthPercent: Double,
    val averageItemsPerOrder: Double,
    val conversionRate: Double,
    val salesVelocityPerHour: Double,
    val hourlySales: List<HourlySalesPoint> = emptyList()
)

data class LiveVisitorPoint(
    val timeLabel: String,
    val activeUsers: Int,
    val pageViews: Int
)

data class ActivePageVisit(
    val path: String,
    val title: String,
    val activeUsers: Int,
    val percentage: Double,
    val category: String
)

data class GeoVisitor(
    val country: String,
    val countryCode: String,
    val flagEmoji: String,
    val topCities: String,
    val visitorsCount: Int,
    val percentage: Double
)

data class TrafficSource(
    val source: String,
    val type: String,
    val visitorsCount: Int,
    val percentage: Double,
    val conversionRate: Double
)

data class DeviceBreakdown(
    val deviceType: String,
    val count: Int,
    val percentage: Double,
    val iconName: String
)

data class LiveVisitorEvent(
    val id: String,
    val timestamp: String,
    val eventType: String,
    val description: String,
    val location: String,
    val iconType: String
)

data class VisitorRealtimeStats(
    val trackingEnabled: Boolean = false,
    val dataAvailable: Boolean = false,
    val activeVisitorsNow: Int = 0,
    val pageViewsPerMin: Int = 0,
    val activeCartsCount: Int = 0,
    val activeCheckoutsCount: Int = 0,
    val avgDurationSeconds: Int = 0,
    val bounceRate: Double = 0.0,
    val trafficHistory: List<LiveVisitorPoint> = emptyList(),
    val topPages: List<ActivePageVisit> = emptyList(),
    val topCountries: List<GeoVisitor> = emptyList(),
    val trafficSources: List<TrafficSource> = emptyList(),
    val deviceStats: List<DeviceBreakdown> = emptyList(),
    val liveEvents: List<LiveVisitorEvent> = emptyList(),
    val source: String = "",
    val lastUpdated: String = "",
    val limitations: String = ""
)

enum class OrdersSubSection(val label: String, val shortDesc: String) {
    ORDERS("Ordini", "Vendite e spedizioni"),
    SUBSCRIPTIONS("Abbonamenti", "Ricorrenze e piani"),
    RETURNS("Resi", "RMA e rimborsi")
}

enum class SubscriptionStatus(val label: String, val englishLabel: String) {
    ACTIVE("Attivo", "Active"),
    PENDING("In attesa", "Pending"),
    SUSPENDED("Sospeso", "Suspended"),
    CANCELED("Annullato", "Canceled"),
    EXPIRED("Scaduto", "Expired")
}

data class Subscription(
    val id: String,
    val subscriptionId: String,
    val customerName: String,
    val customerEmail: String,
    val planName: String,
    val cycleFrequency: String = "Mensile (30 gg)",
    val amount: Double,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val nextPaymentDate: String,
    val startDate: String,
    val paymentMethod: String = "Stripe / Carta Ricorrente",
    val failureCount: Int = 0
)

enum class ReturnStatus(val label: String, val englishLabel: String) {
    PENDING("In Attesa", "Pending"),
    AWAITING_PRODUCTS("Attesa Merce", "Awaiting Products"),
    IN_INSPECTION("In Verifica", "In Inspection"),
    COMPLETE_REFUNDED("Rimborsato", "Refunded"),
    COMPLETE_REPLACED("Sostituito", "Replaced"),
    DENIED("Rifiutato", "Denied")
}

data class OrderReturn(
    val id: String,
    val returnId: String,
    val orderId: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String = "",
    val productName: String,
    val productModel: String,
    val quantity: Int = 1,
    val reason: String,
    val opened: Boolean = true,
    val status: ReturnStatus = ReturnStatus.PENDING,
    val action: String = "In attesa di verifica",
    val dateAdded: String,
    val comment: String = ""
)

/** Moduli amministrativi OpenCart esposti dal bridge CartAdmin. */
enum class AdminModule(val apiKey: String, val label: String, val description: String) {
    SUBSCRIPTION_PLANS("subscription_plans", "Piani di abbonamento", "Piani e cicli ricorrenti"),
    PAGES("pages", "Pagine", "Pagine informative dello store"),
    REVIEWS("reviews", "Recensioni", "Recensioni dei prodotti"),
    ARTICLES("articles", "Articoli", "Contenuti editoriali"),
    TOPICS("topics", "Argomenti", "Categorie editoriali"),
    COMMENTS("comments", "Commenti", "Commenti agli articoli"),
    ANTISPAM("antispam", "Antispam", "Parole bloccate nei commenti"),
    CUSTOMERS("customers", "Clienti", "Account registrati nello store"),
    CUSTOMER_APPROVALS("customer_approvals", "Approvazione clienti", "Richieste in attesa"),
    GDPR("gdpr", "GDPR", "Richieste privacy OpenCart")
}

data class AdminRecord(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val statusLabel: String = "",
    val active: Boolean? = null,
    val date: String = "",
    val detail: String = ""
)

data class AdminModuleSnapshot(
    val module: AdminModule,
    val supported: Boolean = true,
    val isLoading: Boolean = false,
    val records: List<AdminRecord> = emptyList(),
    val message: String = "",
    val lastUpdated: String = ""
)

