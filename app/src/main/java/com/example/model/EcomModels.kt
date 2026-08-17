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
    val apiKey: String = "oc_key_live_8947239847293847293",
    val apiToken: String? = "sess_oc_98234abcf",
    val isConnected: Boolean = true,
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val lastSyncTime: String = "Pochi secondi fa",
    val pendingOrdersCount: Int = 24,
    val stockAlertsCount: Int = 12,
    val todayRevenue: Double = 2840.50,
    val revenueGrowthPercent: Double = 12.4
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
    val activeVisitorsNow: Int = 42,
    val pageViewsPerMin: Int = 138,
    val activeCartsCount: Int = 8,
    val activeCheckoutsCount: Int = 3,
    val avgDurationSeconds: Int = 245,
    val bounceRate: Double = 28.4,
    val trafficHistory: List<LiveVisitorPoint> = emptyList(),
    val topPages: List<ActivePageVisit> = emptyList(),
    val topCountries: List<GeoVisitor> = emptyList(),
    val trafficSources: List<TrafficSource> = emptyList(),
    val deviceStats: List<DeviceBreakdown> = emptyList(),
    val liveEvents: List<LiveVisitorEvent> = emptyList()
)

