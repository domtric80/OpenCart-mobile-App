package com.example.data

import com.example.model.ActivityItem
import com.example.model.ActivityType
import com.example.model.Category
import com.example.model.Order
import com.example.model.OrderDetail
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.Store
import com.example.model.VisitorRealtimeStats
import com.example.model.LiveVisitorPoint
import com.example.model.ActivePageVisit
import com.example.model.GeoVisitor
import com.example.model.TrafficSource
import com.example.model.DeviceBreakdown
import com.example.model.LiveVisitorEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EcomRepository {

    private val _stores = MutableStateFlow(
        listOf(
            Store(
                id = "store_1",
                name = "TechGadgets Italy",
                url = "https://shop.techgadgets.it",
                version = "OpenCart 3.0.3.8",
                todayRevenue = 2840.50,
                revenueGrowthPercent = 12.0,
                pendingOrdersCount = 24,
                stockAlertsCount = 12
            ),
            Store(
                id = "store_2",
                name = "Fashion & Glam Store",
                url = "https://glamstore.eu",
                version = "OpenCart 4.0.2.3",
                todayRevenue = 1420.00,
                revenueGrowthPercent = -3.5,
                pendingOrdersCount = 9,
                stockAlertsCount = 4
            ),
            Store(
                id = "store_3",
                name = "BioFood & Nutrition B2B",
                url = "https://b2b.biofooditaly.com",
                version = "OpenCart 3.0.2.0",
                todayRevenue = 5690.00,
                revenueGrowthPercent = 28.1,
                pendingOrdersCount = 38,
                stockAlertsCount = 19
            )
        )
    )
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    private val _currentStoreId = MutableStateFlow("store_1")
    val currentStoreId: StateFlow<String> = _currentStoreId.asStateFlow()

    private val _activities = MutableStateFlow(
        listOf(
            ActivityItem(
                id = "act_1",
                title = "Marco Rossi",
                subtitle = "New account created",
                timestamp = "2m ago",
                type = ActivityType.USER_REGISTRATION
            ),
            ActivityItem(
                id = "act_2",
                title = "Order #8842",
                subtitle = "Payment confirmed • €149.00",
                timestamp = "15m ago",
                type = ActivityType.ORDER_PAYMENT,
                amount = 149.00,
                orderId = "ord_8842"
            ),
            ActivityItem(
                id = "act_3",
                title = "Giulia Bianchi",
                subtitle = "Order #8841 placed • €89.50",
                timestamp = "42m ago",
                type = ActivityType.ORDER_PAYMENT,
                amount = 89.50,
                orderId = "ord_8841"
            ),
            ActivityItem(
                id = "act_4",
                title = "Stock Alert: Smartwatch X5",
                subtitle = "Only 2 units remaining in stock",
                timestamp = "1h ago",
                type = ActivityType.STOCK_ALERT
            ),
            ActivityItem(
                id = "act_5",
                title = "Order #8840",
                subtitle = "Shipped with tracking GLS78923",
                timestamp = "2h ago",
                type = ActivityType.ORDER_PAYMENT,
                amount = 320.00,
                orderId = "ord_8840"
            )
        )
    )
    val activities: StateFlow<List<ActivityItem>> = _activities.asStateFlow()

    private val _orders = MutableStateFlow(
        listOf(
            Order(
                id = "ord_8842",
                orderNumber = "#8842",
                customerName = "Alessandro Conti",
                customerEmail = "alessandro.conti@email.it",
                total = 149.00,
                status = OrderStatus.PENDING,
                dateAdded = "Oggi, 15:42",
                itemsCount = 2,
                shippingMethod = "Corriere Espresso GLS 24h",
                paymentMethod = "Carta di Credito Stripe",
                notes = "Citofonare interno 4B o lasciare in portineria."
            ),
            Order(
                id = "ord_8841",
                orderNumber = "#8841",
                customerName = "Giulia Bianchi",
                customerEmail = "giulia.b@gmail.com",
                total = 89.50,
                status = OrderStatus.PROCESSING,
                dateAdded = "Oggi, 15:15",
                itemsCount = 1,
                shippingMethod = "BRT Express",
                paymentMethod = "PayPal",
                notes = "Verifica disponibilità colore nero opaco prima di spedire."
            ),
            Order(
                id = "ord_8840",
                orderNumber = "#8840",
                customerName = "Matteo Ferrari",
                customerEmail = "m.ferrari@outlook.com",
                total = 320.00,
                status = OrderStatus.SHIPPED,
                dateAdded = "Oggi, 13:40",
                itemsCount = 4,
                shippingMethod = "DHL Express",
                paymentMethod = "Bonifico Bancario Anticipato",
                notes = "Codice Tracking DHL: 4892719283. Spedizione partita alle 14:00."
            ),
            Order(
                id = "ord_8839",
                orderNumber = "#8839",
                customerName = "Elena Ricci",
                customerEmail = "elena.ricci@yahoo.it",
                total = 45.00,
                status = OrderStatus.PENDING,
                dateAdded = "Oggi, 11:20",
                itemsCount = 1,
                shippingMethod = "Poste Italiane Crono",
                paymentMethod = "Contrassegno (Cash on Delivery)",
                notes = "Richiesta chiamata del corriere 30 min prima della consegna."
            ),
            Order(
                id = "ord_8838",
                orderNumber = "#8838",
                customerName = "Davide Esposito",
                customerEmail = "davide.esp@libero.it",
                total = 590.00,
                status = OrderStatus.DELIVERED,
                dateAdded = "Ieri, 18:30",
                itemsCount = 3,
                shippingMethod = "Corriere Espresso GLS",
                paymentMethod = "Klarna Paga in 3 Rate",
                notes = "Consegnato regolarmente firmato dal destinatario."
            ),
            Order(
                id = "ord_8837",
                orderNumber = "#8837",
                customerName = "Sara Moretti",
                customerEmail = "sara.moretti@icloud.com",
                total = 112.50,
                status = OrderStatus.DELIVERED,
                dateAdded = "Ieri, 16:10",
                itemsCount = 2,
                shippingMethod = "BRT Express",
                paymentMethod = "Carta di Credito Stripe",
                notes = "Pacco consegnato al piano 3."
            ),
            Order(
                id = "ord_8836",
                orderNumber = "#8836",
                customerName = "Marco Colombo",
                customerEmail = "m.colombo@tech.it",
                total = 245.00,
                status = OrderStatus.CONFIRMED,
                dateAdded = "Ieri, 11:45",
                itemsCount = 2,
                shippingMethod = "GLS Express Priority",
                paymentMethod = "Carta di Credito Stripe",
                notes = "Fattura aziendale richiesta con P.IVA IT0982348120."
            ),
            Order(
                id = "ord_8835",
                orderNumber = "#8835",
                customerName = "Federica Rinaldi",
                customerEmail = "fede.rinaldi@gmail.com",
                total = 78.00,
                status = OrderStatus.SHIPPED,
                dateAdded = "2 giorni fa",
                itemsCount = 1,
                shippingMethod = "BRT Express",
                paymentMethod = "PayPal",
                notes = "In transito presso Hub logistico di Bologna."
            )
        )
    )
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _categories = MutableStateFlow(
        listOf(
            Category(
                id = "cat_1",
                name = "Indossabili & Gadget",
                description = "Smartwatch, smartband, tracker fitness e accessori indossabili",
                productsCount = 1,
                status = true,
                sortOrder = 1
            ),
            Category(
                id = "cat_2",
                name = "Audio & Musica",
                description = "Cuffie bluetooth ANC, auricolari wireless e speaker",
                productsCount = 1,
                status = true,
                sortOrder = 2
            ),
            Category(
                id = "cat_3",
                name = "Accessori & Cavi",
                description = "Caricatori GaN veloci, power bank magnetici e cavi USB-C",
                productsCount = 2,
                status = true,
                sortOrder = 3
            ),
            Category(
                id = "cat_4",
                name = "Informatica & Gaming",
                description = "Tastiere meccaniche RGB, webcam 4K, mouse e periferiche da scrivania",
                productsCount = 2,
                status = true,
                sortOrder = 4
            ),
            Category(
                id = "cat_5",
                name = "Fotografia & Video",
                description = "Lenti per smartphone, treppiedi e ring light da studio",
                productsCount = 0,
                status = true,
                sortOrder = 5
            ),
            Category(
                id = "cat_6",
                name = "Domotica & Smart Home",
                description = "Prese intelligenti, sensori di movimento e illuminazione smart",
                productsCount = 0,
                status = true,
                sortOrder = 6
            )
        )
    )
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _products = MutableStateFlow(
        listOf(
            Product(
                id = "prod_1",
                name = "Smartwatch Ultra Pro 4G",
                model = "SW-ULTRA-4G",
                sku = "TECH-0091",
                price = 199.00,
                specialPrice = 149.00,
                quantity = 2,
                minQuantityAlert = 5,
                category = "Indossabili & Gadget",
                description = "Smartwatch con display AMOLED da 1.9 pollici, connettività 4G LTE e sensore SpO2.",
                status = true
            ),
            Product(
                id = "prod_2",
                name = "Cuffie Wireless Active Noise Cancelling",
                model = "ANC-HEAD-80",
                sku = "AUDIO-0442",
                price = 89.90,
                specialPrice = null,
                quantity = 4,
                minQuantityAlert = 5,
                category = "Audio & Musica",
                description = "Cuffie over-ear con cancellazione attiva del rumore fino a 35dB e 40h di autonomia.",
                status = true
            ),
            Product(
                id = "prod_3",
                name = "Caricatore GaN 65W Fast Charge",
                model = "GAN-65W-USB",
                sku = "ACC-0128",
                price = 34.90,
                specialPrice = 27.90,
                quantity = 48,
                minQuantityAlert = 10,
                category = "Accessori & Cavi",
                description = "Caricatore compatto al Nitruro di Gallio con 2 porte USB-C e 1 porta USB-A.",
                status = true
            ),
            Product(
                id = "prod_4",
                name = "Power Bank 20.000mAh Magnetico",
                model = "PB-MAG-20K",
                sku = "ACC-0914",
                price = 49.00,
                specialPrice = null,
                quantity = 1,
                minQuantityAlert = 5,
                category = "Accessori & Cavi",
                description = "Batteria portatile con ricarica wireless MagSafe 15W e display LED percentuale.",
                status = true
            ),
            Product(
                id = "prod_5",
                name = "Tastiera Meccanica RGB Compatta 75%",
                model = "KEY-MECH-75",
                sku = "GAMING-771",
                price = 119.00,
                specialPrice = null,
                quantity = 15,
                minQuantityAlert = 5,
                category = "Informatica & Gaming",
                description = "Switch meccanici lineari hot-swappable con illuminazione RGB programmabile.",
                status = true
            ),
            Product(
                id = "prod_6",
                name = "Webcam 4K Ultra HD con Microfono Stereo",
                model = "CAM-4K-PRO",
                sku = "OFFICE-302",
                price = 79.50,
                specialPrice = null,
                quantity = 3,
                minQuantityAlert = 5,
                category = "Informatica & Gaming",
                description = "Sensore Sony 4K 60fps con autofocus intelligente e otturatore per la privacy.",
                status = true
            )
        )
    )
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val apiClient = com.example.network.OpenCartApiClient()

    fun selectStore(storeId: String) {
        _currentStoreId.value = storeId
    }

    suspend fun testStoreConnection(url: String, username: String, key: String): com.example.network.OpenCartConnectionResult {
        return apiClient.testConnection(url, username, key)
    }

    fun updateStoreCredentials(
        storeId: String,
        name: String,
        url: String,
        username: String,
        key: String,
        version: String
    ) {
        _stores.update { current ->
            current.map { s ->
                if (s.id == storeId) {
                    s.copy(
                        name = name,
                        url = url,
                        apiUsername = username,
                        apiKey = key,
                        version = version,
                        lastSyncTime = "Proprio adesso"
                    )
                } else s
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        _orders.update { current ->
            current.map { order ->
                if (order.id == orderId) order.copy(status = newStatus) else order
            }
        }

        val targetOrder = _orders.value.find { it.id == orderId }
        val activity = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "Ordine ${targetOrder?.orderNumber ?: orderId}",
            subtitle = "Stato aggiornato a: ${newStatus.label} (${newStatus.englishLabel})",
            timestamp = "Adesso",
            type = ActivityType.ORDER_PAYMENT,
            amount = targetOrder?.total,
            orderId = orderId
        )
        _activities.update { listOf(activity) + it }
    }

    fun updateOrderNotes(orderId: String, notes: String) {
        _orders.update { current ->
            current.map { order ->
                if (order.id == orderId) order.copy(notes = notes) else order
            }
        }
    }

    fun updateOrderStatusAndNotes(orderId: String, newStatus: OrderStatus, notes: String) {
        _orders.update { current ->
            current.map { order ->
                if (order.id == orderId) order.copy(status = newStatus, notes = notes) else order
            }
        }

        val targetOrder = _orders.value.find { it.id == orderId }
        val activity = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "Ordine ${targetOrder?.orderNumber ?: orderId}",
            subtitle = "Stato e note aggiornati (${newStatus.label})",
            timestamp = "Adesso",
            type = ActivityType.ORDER_PAYMENT,
            amount = targetOrder?.total,
            orderId = orderId
        )
        _activities.update { listOf(activity) + it }
    }

    // --- Product Management ---

    fun updateProductStock(productId: String, newQuantity: Int) {
        _products.update { current ->
            current.map { prod ->
                if (prod.id == productId) prod.copy(quantity = newQuantity.coerceAtLeast(0)) else prod
            }
        }
    }

    fun addProduct(
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
    ): Product {
        val newProd = Product(
            id = "prod_${System.currentTimeMillis()}",
            name = name,
            model = model,
            sku = sku.ifBlank { "OC-${System.currentTimeMillis() % 10000}" },
            price = price,
            specialPrice = specialPrice,
            quantity = quantity,
            minQuantityAlert = minQuantityAlert,
            category = category,
            description = description,
            status = status
        )
        _products.update { listOf(newProd) + it }
        updateCategoryProductCounts()

        val activity = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "Nuovo Prodotto OpenCart",
            subtitle = "$name (€$price)",
            timestamp = "Adesso",
            type = ActivityType.STOCK_ALERT
        )
        _activities.update { listOf(activity) + it }
        return newProd
    }

    fun updateProduct(product: Product) {
        _products.update { current ->
            current.map { if (it.id == product.id) product else it }
        }
        updateCategoryProductCounts()
    }

    fun deleteProduct(productId: String) {
        val target = _products.value.find { it.id == productId }
        _products.update { current -> current.filterNot { it.id == productId } }
        updateCategoryProductCounts()

        if (target != null) {
            val activity = ActivityItem(
                id = "act_${System.currentTimeMillis()}",
                title = "Prodotto Rimosso",
                subtitle = "${target.name} eliminato dal catalogo",
                timestamp = "Adesso",
                type = ActivityType.STOCK_ALERT
            )
            _activities.update { listOf(activity) + it }
        }
    }

    fun toggleProductStatus(productId: String) {
        _products.update { current ->
            current.map {
                if (it.id == productId) it.copy(status = !it.status) else it
            }
        }
    }

    // --- Category Management ---

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
        updateCategoryProductCounts()

        val activity = ActivityItem(
            id = "act_${System.currentTimeMillis()}",
            title = "Nuova Categoria Creata",
            subtitle = name,
            timestamp = "Adesso",
            type = ActivityType.STOCK_ALERT
        )
        _activities.update { listOf(activity) + it }
        return newCat
    }

    fun updateCategory(
        categoryId: String,
        name: String,
        description: String,
        sortOrder: Int,
        status: Boolean
    ) {
        val oldCat = _categories.value.find { it.id == categoryId }
        _categories.update { current ->
            current.map {
                if (it.id == categoryId) {
                    it.copy(
                        name = name,
                        description = description,
                        sortOrder = sortOrder,
                        status = status
                    )
                } else it
            }
        }

        // If category name changed, update products that had the old category
        if (oldCat != null && oldCat.name != name) {
            _products.update { prods ->
                prods.map { if (it.category == oldCat.name) it.copy(category = name) else it }
            }
        }
        updateCategoryProductCounts()
    }

    fun deleteCategory(categoryId: String) {
        val cat = _categories.value.find { it.id == categoryId }
        _categories.update { current -> current.filterNot { it.id == categoryId } }

        // Reassign affected products to "Non Categorizzato"
        if (cat != null) {
            _products.update { prods ->
                prods.map { if (it.category == cat.name) it.copy(category = "Generale") else it }
            }
        }
        updateCategoryProductCounts()
    }

    fun toggleCategoryStatus(categoryId: String) {
        _categories.update { current ->
            current.map {
                if (it.id == categoryId) it.copy(status = !it.status) else it
            }
        }
    }

    private fun updateCategoryProductCounts() {
        val prods = _products.value
        _categories.update { cats ->
            cats.map { cat ->
                val count = prods.count { it.category == cat.name }
                cat.copy(productsCount = count)
            }
        }
    }

    // --- Order Details Helper ---

    fun getDetailedOrder(order: Order): OrderDetail {
        val items = when (order.id) {
            "ord_8842" -> listOf(
                com.example.model.OrderItem("item_8842_1", order.id, "prod_1", "Smartwatch Ultra Pro 4G", "SW-ULTRA-4G", 1, 149.00, 149.00),
                com.example.model.OrderItem("item_8842_2", order.id, "prod_3", "Cavo Intrecciato Fast Charge 2m", "USB-C-2M", 1, 0.00, 0.00)
            )
            "ord_8841" -> listOf(
                com.example.model.OrderItem("item_8841_1", order.id, "prod_2", "Cuffie Wireless Active Noise Cancelling", "ANC-HEAD-80", 1, 89.50, 89.50)
            )
            "ord_8840" -> listOf(
                com.example.model.OrderItem("item_8840_1", order.id, "prod_5", "Tastiera Meccanica RGB Compatta 75%", "KEY-MECH-75", 2, 119.00, 238.00),
                com.example.model.OrderItem("item_8840_2", order.id, "prod_6", "Webcam 4K Ultra HD con Microfono Stereo", "CAM-4K-PRO", 1, 79.50, 79.50),
                com.example.model.OrderItem("item_8840_3", order.id, "prod_4", "Tappetino Desk Mat XXL Idrorepellente", "MAT-XXL-BLK", 1, 2.50, 2.50)
            )
            "ord_8839" -> listOf(
                com.example.model.OrderItem("item_8839_1", order.id, "prod_4", "Power Bank 20.000mAh Magnetico", "PB-MAG-20K", 1, 45.00, 45.00)
            )
            "ord_8838" -> listOf(
                com.example.model.OrderItem("item_8838_1", order.id, "prod_1", "Smartwatch Ultra Pro 4G Titanio", "SW-ULTRA-TIT", 2, 199.00, 398.00),
                com.example.model.OrderItem("item_8838_2", order.id, "prod_2", "Cuffie Wireless Noise Cancelling Pro", "ANC-PRO-BLK", 1, 185.10, 185.10),
                com.example.model.OrderItem("item_8838_3", order.id, "prod_3", "Caricatore GaN 65W Fast Charge", "GAN-65W-USB", 1, 6.90, 6.90)
            )
            "ord_8837" -> listOf(
                com.example.model.OrderItem("item_8837_1", order.id, "prod_3", "Caricatore GaN 65W Fast Charge", "GAN-65W-USB", 2, 27.90, 55.80),
                com.example.model.OrderItem("item_8837_2", order.id, "prod_4", "Power Bank 20.000mAh Magnetico", "PB-MAG-20K", 1, 49.00, 49.00),
                com.example.model.OrderItem("item_8837_3", order.id, "prod_c", "Custodia Protettiva Anti-urto", "CASE-MAG-SLIM", 1, 7.70, 7.70)
            )
            else -> listOf(
                com.example.model.OrderItem("item_${order.id}_1", order.id, "prod_custom", "Articolo OpenCart Standard", "OC-ART-GEN", order.itemsCount.coerceAtLeast(1), order.total / order.itemsCount.coerceAtLeast(1), order.total)
            )
        }

        val addresses = when (order.id) {
            "ord_8842" -> Pair("Alessandro Conti\nVia Montenapoleone 18, 20121 Milano (MI)\nItalia", "+39 348 765 4321")
            "ord_8841" -> Pair("Giulia Bianchi\nCorso Vittorio Emanuele II 45, 10123 Torino (TO)\nItalia", "+39 333 912 8841")
            "ord_8840" -> Pair("Matteo Ferrari\nVia dell'Indipendenza 12, 40121 Bologna (BO)\nItalia", "+39 340 551 2390")
            "ord_8839" -> Pair("Elena Ricci\nVia Toledo 156, 80134 Napoli (NA)\nItalia", "+39 328 440 9182")
            "ord_8838" -> Pair("Davide Esposito\nVia del Corso 28, 00186 Roma (RM)\nItalia", "+39 347 119 2834")
            "ord_8837" -> Pair("Sara Moretti\nVia Garibaldi 89, 50123 Firenze (FI)\nItalia", "+39 339 678 1209")
            else -> Pair("${order.customerName}\nVia Roma 42, 20121 Milano (MI)\nItalia", "+39 347 889 1234")
        }

        val sub = (order.total - 6.90).coerceAtLeast(10.0)
        return OrderDetail(
            order = order,
            items = items,
            customerPhone = addresses.second,
            shippingAddress = addresses.first,
            paymentAddress = addresses.first,
            subtotal = sub,
            shippingCost = 6.90,
            taxAmount = sub * 0.22,
            discountAmount = 0.0,
            grandTotal = order.total,
            customerNotes = order.notes,
            isFromLocalCache = true,
            cachedTimestamp = "Locale (Room SQLite)"
        )
    }

    fun getAllDetailedOrders(): List<OrderDetail> {
        return _orders.value.map { getDetailedOrder(it) }
    }

    private val _visitorStats = MutableStateFlow(
        VisitorRealtimeStats(
            activeVisitorsNow = 48,
            pageViewsPerMin = 142,
            activeCartsCount = 9,
            activeCheckoutsCount = 3,
            avgDurationSeconds = 215,
            bounceRate = 26.8,
            trafficHistory = listOf(
                com.example.model.LiveVisitorPoint("15:02", 34, 98),
                com.example.model.LiveVisitorPoint("15:06", 38, 110),
                com.example.model.LiveVisitorPoint("15:10", 41, 122),
                com.example.model.LiveVisitorPoint("15:14", 39, 115),
                com.example.model.LiveVisitorPoint("15:18", 45, 130),
                com.example.model.LiveVisitorPoint("15:22", 52, 155),
                com.example.model.LiveVisitorPoint("15:26", 47, 138),
                com.example.model.LiveVisitorPoint("Ora", 48, 142)
            ),
            topPages = listOf(
                com.example.model.ActivePageVisit("/product/smartwatch-ultra-pro", "Smartwatch Ultra Pro GPS", 15, 31.2, "Elettronica"),
                com.example.model.ActivePageVisit("/checkout/cart", "Carrello Acquisti (OpenCart)", 9, 18.7, "Checkout"),
                com.example.model.ActivePageVisit("/category/audio-cuffie", "Cuffie & Auricolari Wireless", 8, 16.6, "Audio"),
                com.example.model.ActivePageVisit("/product/cuffie-wireless-anc", "Cuffie Wireless ANC Noise Cancelling", 7, 14.5, "Audio"),
                com.example.model.ActivePageVisit("/checkout/onepage", "Cassa Veloce - Pagamento", 3, 6.2, "Checkout"),
                com.example.model.ActivePageVisit("/index.php?route=common/home", "Home Page Negozio", 6, 12.8, "Generale")
            ),
            topCountries = listOf(
                com.example.model.GeoVisitor("Italia", "IT", "🇮🇹", "Milano, Roma, Torino, Napoli", 37, 77.0),
                com.example.model.GeoVisitor("Germania", "DE", "🇩🇪", "Berlino, Monaco", 5, 10.4),
                com.example.model.GeoVisitor("Francia", "FR", "🇫🇷", "Parigi, Lione", 3, 6.2),
                com.example.model.GeoVisitor("Spagna", "ES", "🇪🇸", "Madrid, Barcellona", 2, 4.2),
                com.example.model.GeoVisitor("Regno Unito", "GB", "🇬🇧", "Londra", 1, 2.2)
            ),
            trafficSources = listOf(
                com.example.model.TrafficSource("Google Search (Organico)", "Ricerca", 20, 41.6, 4.2),
                com.example.model.TrafficSource("Instagram Ads / Meta", "Social", 14, 29.1, 3.8),
                com.example.model.TrafficSource("Accesso Diretto (URL)", "Diretto", 9, 18.7, 5.1),
                com.example.model.TrafficSource("TikTok Shop / Influencer", "Social", 3, 6.3, 2.9),
                com.example.model.TrafficSource("Newsletter Promo Weekend", "Email", 2, 4.3, 6.7)
            ),
            deviceStats = listOf(
                com.example.model.DeviceBreakdown("Smartphone Mobile", 35, 73.0, "phone"),
                com.example.model.DeviceBreakdown("Computer Desktop", 11, 23.0, "desktop"),
                com.example.model.DeviceBreakdown("Tablet iPad/Android", 2, 4.0, "tablet")
            ),
            liveEvents = listOf(
                com.example.model.LiveVisitorEvent("evt_1", "10 sec fa", "checkout_start", "Iniziato checkout da Milano (IT)", "Milano, IT", "checkout"),
                com.example.model.LiveVisitorEvent("evt_2", "24 sec fa", "cart_add", "Aggiunto 'Smartwatch Ultra Pro' al carrello", "Roma, IT", "cart"),
                com.example.model.LiveVisitorEvent("evt_3", "45 sec fa", "page_view", "Visualizzata scheda 'Cuffie Wireless ANC'", "Torino, IT", "view"),
                com.example.model.LiveVisitorEvent("evt_4", "1 min fa", "search", "Cercato 'custodia impermeabile'", "Bologna, IT", "search"),
                com.example.model.LiveVisitorEvent("evt_5", "2 min fa", "purchase", "Ordine completato €149.00 da Alessandro C.", "Milano, IT", "order"),
                com.example.model.LiveVisitorEvent("evt_6", "3 min fa", "new_session", "Nuovo visitatore da Monaco (DE) via Google", "Monaco, DE", "session")
            )
        )
    )
    val visitorStats: StateFlow<VisitorRealtimeStats> = _visitorStats.asStateFlow()

    fun updateLiveVisitorCount(delta: Int) {
        _visitorStats.update { current ->
            val newCount = (current.activeVisitorsNow + delta).coerceIn(12, 120)
            val newPvm = (newCount * 2.9 + (1..6).random()).toInt()
            current.copy(
                activeVisitorsNow = newCount,
                pageViewsPerMin = newPvm
            )
        }
    }

    fun addLiveVisitorEvent(event: com.example.model.LiveVisitorEvent) {
        _visitorStats.update { current ->
            current.copy(liveEvents = listOf(event) + current.liveEvents.take(15))
        }
    }

    fun addStore(name: String, url: String, version: String, username: String = "api_user", key: String = "api_key_secret") {
        val newStore = Store(
            id = "store_${System.currentTimeMillis()}",
            name = name,
            url = url,
            version = version,
            apiUsername = username,
            apiKey = key,
            todayRevenue = 1250.0,
            revenueGrowthPercent = 5.0,
            pendingOrdersCount = 8,
            stockAlertsCount = 2,
            lastSyncTime = "Adesso"
        )
        _stores.update { it + newStore }
        _currentStoreId.value = newStore.id
    }

    fun clearDummyData() {
        _orders.value = emptyList()
        _products.value = emptyList()
        _categories.value = emptyList()
        _activities.value = emptyList()
        _stores.update { currentList ->
            currentList.map { store ->
                store.copy(
                    todayRevenue = 0.0,
                    revenueGrowthPercent = 0.0,
                    pendingOrdersCount = 0,
                    stockAlertsCount = 0,
                    lastSyncTime = "Nessun dato sincronizzato"
                )
            }
        }
    }
}
