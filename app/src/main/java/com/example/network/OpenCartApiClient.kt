package com.example.network

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.model.Category
import com.example.model.ActivePageVisit
import com.example.model.AdminModule
import com.example.model.AdminModuleSnapshot
import com.example.model.AdminRecord
import com.example.model.DeviceBreakdown
import com.example.model.GeoVisitor
import com.example.model.LiveVisitorEvent
import com.example.model.LiveVisitorPoint
import com.example.model.Order
import com.example.model.OrderDetail
import com.example.model.OrderItem
import com.example.model.OrderReturn
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.ProductImageUpload
import com.example.model.ReturnStatus
import com.example.model.Store
import com.example.model.Subscription
import com.example.model.SubscriptionStatus
import com.example.model.TrafficSource
import com.example.model.VisitorRealtimeStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class OpenCartConnectionResult(
    val isSuccess: Boolean,
    val statusCode: Int,
    val responseTimeMs: Long,
    val message: String,
    val apiToken: String? = null,
    val details: String? = null,
    val isBridgeDetected: Boolean = false
)

class OpenCartApiClient(context: Context) {

    private val tlsClient = TlsPinnedClient(context)

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /** Recupera un elenco amministrativo reale dal bridge senza persistenza locale. */
    suspend fun fetchAdminModule(
        baseUrl: String,
        apiKey: String,
        username: String,
        module: AdminModule,
        limit: Int = 100
    ): Result<AdminModuleSnapshot> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim().removeSuffix("/")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val request = BridgeRequestFactory.authenticatedGet(
                baseUrl = cleanUrl,
                action = "management_list",
                apiKey = apiKey,
                username = username,
                queryParameters = mapOf(
                    "module" to module.apiKey,
                    "limit" to limit.coerceIn(1, 200).toString()
                )
            )

            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }

                val json = JSONObject(body)
                if (!json.optBoolean("success", false)) {
                    return@withContext Result.failure(
                        Exception(json.optString("error", "Elenco ${module.label} non disponibile"))
                    )
                }
                val items = json.optJSONArray("items") ?: JSONArray()
                val records = buildList {
                    for (index in 0 until items.length()) {
                        val item = items.getJSONObject(index)
                        add(
                            AdminRecord(
                                id = item.optString("id", index.toString()),
                                title = item.optString("title", "#${item.optString("id", index.toString())}"),
                                subtitle = item.optString("subtitle", ""),
                                statusLabel = item.optString("status_label", ""),
                                active = if (item.has("active") && !item.isNull("active")) {
                                    item.optBoolean("active")
                                } else null,
                                date = item.optString("date", ""),
                                detail = item.optString("detail", ""),
                                actionable = item.optBoolean("actionable", false),
                                pendingCommandId = if (item.isNull("pending_command_id")) "" else item.optString("pending_command_id"),
                                pendingOperation = item.optString("pending_operation", ""),
                                content = item.optString("content", ""),
                                rating = if (item.has("rating") && !item.isNull("rating")) item.optInt("rating") else null,
                                sortOrder = if (item.has("sort_order") && !item.isNull("sort_order")) item.optInt("sort_order") else null,
                                parentId = if (item.has("parent_id") && !item.isNull("parent_id")) item.optInt("parent_id") else null,
                                editable = item.optBoolean("editable", false)
                            )
                        )
                    }
                }
                Result.success(
                    AdminModuleSnapshot(
                        module = module,
                        supported = json.optBoolean("supported", true),
                        records = records,
                        message = json.optString("message", ""),
                        lastUpdated = json.optString("generated_at", "")
                    )
                )
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun updateAdminRecordStatus(
        baseUrl: String,
        apiKey: String,
        username: String,
        module: AdminModule,
        recordId: String,
        active: Boolean
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim().removeSuffix("/")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val request = BridgeRequestFactory.authenticatedFormPost(
                baseUrl = cleanUrl,
                action = "management_status",
                apiKey = apiKey,
                username = username,
                fields = mapOf(
                    "module" to module.apiKey,
                    "id" to recordId,
                    "active" to if (active) "1" else "0"
                )
            )
            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                } else {
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false)) Result.success(true)
                    else Result.failure(Exception(json.optString("error", "Aggiornamento rifiutato")))
                }
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun mutateAntispamKeyword(
        baseUrl: String,
        apiKey: String,
        username: String,
        operation: String,
        keyword: String = "",
        recordId: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim().removeSuffix("/")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val fields = if (operation == "add") {
                mapOf("operation" to "add", "keyword" to keyword.trim())
            } else {
                mapOf("operation" to "delete", "id" to recordId)
            }
            val request = BridgeRequestFactory.authenticatedFormPost(
                baseUrl = cleanUrl,
                action = "management_antispam",
                apiKey = apiKey,
                username = username,
                fields = fields
            )
            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                } else {
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false)) Result.success(true)
                    else Result.failure(Exception(json.optString("error", "Operazione Antispam rifiutata")))
                }
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /**
     * Test connection to OpenCart store:
     * 1. Checks the CartAdmin Bridge extension endpoint.
     * 2. If not present, checks standard OpenCart API (index.php?route=api/login)
     */
    suspend fun testConnection(
        storeUrl: String,
        apiUsername: String,
        apiKey: String
    ): OpenCartConnectionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var cleanUrl = storeUrl.trim().removeSuffix("/")
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        val parsedUrl = cleanUrl.toHttpUrlOrNull()
        if (parsedUrl == null) {
            return@withContext OpenCartConnectionResult(
                isSuccess = false,
                statusCode = 0,
                responseTimeMs = 0,
                message = "URL non valido. Inserisci un URL valido (es. https://miosito.it)",
                details = "Formato URL errato"
            )
        }

        // 1. Check CartAdmin Bridge Module
        val bridgeRequest = BridgeRequestFactory.authenticatedGet(
            baseUrl = cleanUrl,
            action = "status",
            apiKey = apiKey,
            username = apiUsername
        )

        try {
            tlsClient.execute(bridgeRequest).use { response ->
                val duration = System.currentTimeMillis() - startTime
                val body = response.body?.string() ?: ""

                if (response.isSuccessful && body.contains("\"success\":true")) {
                    val json = try { JSONObject(body) } catch (e: Exception) { null }
                    val ocVer = json?.optString("bridge_version", json.optString("opencart_version", "1.2.2")) ?: "1.2.2"
                    val storeName = json?.optString("store_name", "OpenCart Store") ?: "OpenCart Store"
                    val ordersCount = json?.optInt("total_orders", 0) ?: 0
                    val prodsCount = json?.optInt("total_products", 0) ?: 0

                    return@withContext OpenCartConnectionResult(
                        isSuccess = true,
                        statusCode = response.code,
                        responseTimeMs = duration,
                        message = "Modulo CartAdmin Bridge attivo e connesso a $storeName!",
                        apiToken = apiKey,
                        details = "Bridge v$ocVer • Ordini: $ordersCount • Prodotti: $prodsCount • Risposta in ${duration}ms.",
                        isBridgeDetected = true
                    )
                } else if (response.code == 401) {
                    val json = try { JSONObject(body) } catch (e: Exception) { null }
                    val hint = json?.optString("hint", "Verifica che la chiave inserita corrisponda a quella configurata in OpenCart.")
                    return@withContext OpenCartConnectionResult(
                        isSuccess = false,
                        statusCode = 401,
                        responseTimeMs = duration,
                        message = "Modulo CartAdmin rilevato ma autenticazione non riuscita (401).",
                        details = hint
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback to native OpenCart API
        }

        // 2. Check Native OpenCart route=api/login
        val nativeUrl = "$cleanUrl/index.php?route=api/login"
        val formBody = FormBody.Builder()
            .add("username", apiUsername)
            .add("key", apiKey)
            .build()

        val nativeRequest = Request.Builder()
            .url(nativeUrl)
            .post(formBody)
            .header("User-Agent", "CartAdmin-Android/${BuildConfig.VERSION_NAME}")
            .header("Accept", "application/json")
            .build()

        try {
            tlsClient.execute(nativeRequest).use { response ->
                val duration = System.currentTimeMillis() - startTime
                val responseBody = response.body?.string() ?: ""
                val code = response.code

                if (response.isSuccessful) {
                    val json = try { JSONObject(responseBody) } catch (e: Exception) { null }
                    if (json != null && (json.has("api_token") || json.has("token") || json.has("success"))) {
                        val token = json.optString("api_token", json.optString("token", ""))
                        return@withContext OpenCartConnectionResult(
                            isSuccess = true,
                            statusCode = code,
                            responseTimeMs = duration,
                            message = "Connessione OpenCart API nativa riuscita!",
                            apiToken = token,
                            details = "Token sessione OpenCart ottenuto in ${duration}ms."
                        )
                    } else if (json != null && json.has("error")) {
                        val errorObj = json.opt("error")
                        val errorMsg = if (errorObj is JSONObject) {
                            errorObj.optString("warning", errorObj.optString("key", "Errore IP / credenziali"))
                        } else errorObj.toString()

                        return@withContext OpenCartConnectionResult(
                            isSuccess = false,
                            statusCode = code,
                            responseTimeMs = duration,
                            message = "OpenCart API errore: $errorMsg",
                            details = "Consiglio: installa e configura CartAdmin Bridge dal pannello OpenCart."
                        )
                    }

                    return@withContext OpenCartConnectionResult(
                        isSuccess = true,
                        statusCode = code,
                        responseTimeMs = duration,
                        message = "Store OpenCart raggiungibile ($code OK).",
                        details = "Risposta ricevuta in ${duration}ms."
                    )
                } else {
                    return@withContext OpenCartConnectionResult(
                        isSuccess = false,
                        statusCode = code,
                        responseTimeMs = duration,
                        message = "Errore HTTP $code dal server OpenCart.",
                        details = "Installa cartadmin.ocmod.zip dal pannello OpenCart oppure verifica i permessi API."
                    )
                }
            }
        } catch (e: IOException) {
            val duration = System.currentTimeMillis() - startTime
            return@withContext OpenCartConnectionResult(
                isSuccess = false,
                statusCode = 0,
                responseTimeMs = duration,
                message = "Impossibile raggiungere il server: ${e.localizedMessage ?: "Errore di rete"}",
                details = "Verifica la connessione internet e l'URL inserito ($cleanUrl)."
            )
        }
    }

    /**
     * Scarica gli ordini reali dal server OpenCart.
     */
    suspend fun fetchOrders(baseUrl: String, apiKey: String, username: String = "", limit: Int = 50): Result<List<Order>> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim().removeSuffix("/")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val request = BridgeRequestFactory.authenticatedGet(
                baseUrl = cleanUrl,
                action = "orders",
                apiKey = apiKey,
                username = username,
                queryParameters = mapOf("limit" to limit.toString())
            )

            tlsClient.execute(request).use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }

                val json = JSONObject(body)
                val ordersArray = json.optJSONArray("orders") ?: JSONArray()
                val list = mutableListOf<Order>()

                for (i in 0 until ordersArray.length()) {
                    val obj = ordersArray.getJSONObject(i)
                    val rawOrderId = if (obj.has("order_id")) {
                        obj.optString("order_id")
                    } else if (obj.has("id")) {
                        obj.optString("id")
                    } else {
                        (i + 1).toString()
                    }
                    val orderId = rawOrderId.removePrefix("order_").removePrefix("ord_")
                    val customer = obj.optString("customer", obj.optString("customer_name", "Cliente #$orderId"))
                    val total = obj.optDouble("total", 0.0)
                    val statusId = obj.optInt("status_id", obj.optInt("order_status_id", 1))
                    val dateAdded = obj.optString("date_added", "N/D")
                    val paymentMethod = obj.optString("payment_method", "Non specificato")
                    val shippingMethod = obj.optString("shipping_method", "Corriere Standard")

                    val status = when (statusId) {
                        1 -> OrderStatus.PENDING
                        2 -> OrderStatus.PROCESSING
                        3 -> OrderStatus.SHIPPED
                        5 -> OrderStatus.COMPLETE
                        7 -> OrderStatus.CANCELLED
                        else -> OrderStatus.CONFIRMED
                    }

                    list.add(
                        Order(
                            id = orderId,
                            orderNumber = "#$orderId",
                            customerName = customer,
                            customerEmail = obj.optString("email", ""),
                            total = total,
                            status = status,
                            dateAdded = dateAdded,
                            itemsCount = 1,
                            shippingMethod = shippingMethod,
                            paymentMethod = paymentMethod,
                            notes = "Ordine sincronizzato da OpenCart"
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Scarica i prodotti reali dal server OpenCart.
     */
    suspend fun fetchProducts(baseUrl: String, apiKey: String, username: String = "", limit: Int = 100): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim().removeSuffix("/")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val request = BridgeRequestFactory.authenticatedGet(
                baseUrl = cleanUrl,
                action = "products",
                apiKey = apiKey,
                username = username,
                queryParameters = mapOf("limit" to limit.toString())
            )

            tlsClient.execute(request).use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }

                val json = JSONObject(body)
                val prodsArray = json.optJSONArray("products") ?: JSONArray()
                val list = mutableListOf<Product>()

                for (i in 0 until prodsArray.length()) {
                    val obj = prodsArray.getJSONObject(i)
                    val rawProdId = if (obj.has("product_id")) {
                        obj.optString("product_id")
                    } else if (obj.has("id")) {
                        obj.optString("id")
                    } else {
                        (i + 1).toString()
                    }
                    val prodId = rawProdId.removePrefix("prod_").removePrefix("product_")
                    val rawName = obj.optString("name", "Prodotto #$prodId")
                    val cleanName = rawName.replace("&amp;", "&").replace("&quot;", "\"").replace("&#039;", "'").replace("&lt;", "<").replace("&gt;", ">")
                    val model = obj.optString("model", "SKU-$prodId")
                    val sku = obj.optString("sku", model)
                    val qty = obj.optInt("quantity", 0)
                    val price = obj.optDouble("price", 0.0)
                    val specialPrice = if (obj.isNull("special_price")) null else obj.optDouble("special_price")
                    val status = obj.optBoolean("status", true)
                    val category = obj.optString("category", "Catalogo OpenCart")

                    list.add(
                        Product(
                            id = prodId,
                            name = cleanName,
                            model = model,
                            sku = if (sku.isNotBlank()) sku else model,
                            price = price,
                            specialPrice = specialPrice,
                            quantity = qty,
                            minQuantityAlert = obj.optInt("minimum", 5).coerceAtLeast(1),
                            category = category,
                            description = obj.optString("description", ""),
                            status = status
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Scarica l'albero delle categorie reali dal server OpenCart.
     */
    suspend fun fetchCategories(baseUrl: String, apiKey: String, username: String = "", limit: Int = 100): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim().removeSuffix("/")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val request = BridgeRequestFactory.authenticatedGet(
                baseUrl = cleanUrl,
                action = "categories",
                apiKey = apiKey,
                username = username,
                queryParameters = mapOf("limit" to limit.toString())
            )

            tlsClient.execute(request).use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: $body"))
                }

                val json = JSONObject(body)
                val catArray = json.optJSONArray("categories") ?: JSONArray()
                val list = mutableListOf<Category>()

                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    val rawCatId = if (obj.has("category_id")) {
                        obj.optString("category_id")
                    } else if (obj.has("id")) {
                        obj.optString("id")
                    } else {
                        (i + 1).toString()
                    }
                    val catId = rawCatId.removePrefix("cat_").removePrefix("category_")
                    val rawName = obj.optString("name", "Categoria #$catId")
                    val cleanName = rawName.replace("&amp;", "&").replace("&quot;", "\"").replace("&#039;", "'").replace("&lt;", "<").replace("&gt;", ">")
                    val desc = obj.optString("description", "")
                    val count = obj.optInt("products_count", obj.optInt("count", 0))
                    val status = obj.optBoolean("status", true)
                    val sortOrder = obj.optInt("sort_order", 0)

                    list.add(
                        Category(
                            id = catId,
                            name = cleanName,
                            description = desc,
                            productsCount = count,
                            status = status,
                            sortOrder = sortOrder
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Aggiorna la scheda prodotto completa e verifica la risposta del bridge. */
    suspend fun updateProduct(
        baseUrl: String,
        apiKey: String,
        product: Product,
        image: ProductImageUpload? = null,
        username: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val fields = productFields(product) + ("product_id" to product.id.removePrefix("prod_").removePrefix("product_"))
            val request = productMutationRequest(baseUrl, "update_product", apiKey, username, fields, image)

            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use Result.failure(Exception("HTTP ${response.code}: $body"))
                }
                val json = JSONObject(body)
                if (json.optBoolean("success", false)) {
                    Result.success(true)
                } else {
                    Result.failure(Exception(json.optString("error", "Aggiornamento prodotto rifiutato")))
                }
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /** Aggiorna i campi editoriali allowlisted di un record esistente. */
    suspend fun updateAdminContent(
        baseUrl: String,
        apiKey: String,
        username: String,
        module: AdminModule,
        record: AdminRecord
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim().removeSuffix("/")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val request = BridgeRequestFactory.authenticatedFormPost(
                baseUrl = cleanUrl,
                action = "management_content",
                apiKey = apiKey,
                username = username,
                fields = mapOf(
                    "module" to module.apiKey,
                    "id" to record.id,
                    "title" to record.title,
                    "secondary" to record.subtitle,
                    "content" to record.content,
                    "rating" to (record.rating?.toString() ?: ""),
                    "sort_order" to (record.sortOrder?.toString() ?: "")
                )
            )
            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                } else {
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false)) Result.success(true)
                    else Result.failure(Exception(json.optString("error", "Modifica editoriale rifiutata")))
                }
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun createAdminContent(
        baseUrl: String,
        apiKey: String,
        username: String,
        module: AdminModule,
        record: AdminRecord
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            require(module == AdminModule.ARTICLES || module == AdminModule.TOPICS) { "Modulo CMS non creabile" }
            val request = BridgeRequestFactory.authenticatedFormPost(
                baseUrl = baseUrl,
                action = "management_create",
                apiKey = apiKey,
                username = username,
                fields = mapOf(
                    "module" to module.apiKey,
                    "title" to record.title,
                    "secondary" to record.subtitle,
                    "content" to record.content,
                    "parent_id" to (record.parentId?.toString() ?: ""),
                    "sort_order" to (record.sortOrder?.toString() ?: "0"),
                    "active" to if (record.active == true) "1" else "0"
                )
            )
            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) Result.failure(Exception("HTTP ${response.code}: $body"))
                else JSONObject(body).let { json ->
                    if (json.optBoolean("success", false)) Result.success(json.optString("id"))
                    else Result.failure(Exception(json.optString("error", "Creazione CMS rifiutata")))
                }
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun enqueueAdminCommand(
        baseUrl: String,
        apiKey: String,
        username: String,
        module: AdminModule,
        recordId: String,
        operation: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            require(module == AdminModule.CUSTOMER_APPROVALS || module == AdminModule.GDPR) {
                "Il modulo non supporta richieste amministrative sensibili"
            }
            require(operation == "approve" || operation == "deny") { "Operazione non valida" }
            val request = BridgeRequestFactory.authenticatedFormPost(
                baseUrl = baseUrl,
                action = "management_command",
                apiKey = apiKey,
                username = username,
                fields = mapOf("module" to module.apiKey, "id" to recordId, "operation" to operation)
            )
            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                } else {
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false) && json.optString("status") == "pending") {
                        Result.success(true)
                    } else {
                        Result.failure(Exception(json.optString("error", "Richiesta amministrativa rifiutata")))
                    }
                }
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /** Crea il prodotto sullo store e restituisce l'ID assegnato da OpenCart. */
    suspend fun createProduct(
        baseUrl: String,
        apiKey: String,
        product: Product,
        image: ProductImageUpload? = null,
        username: String = ""
    ): Result<Product> = withContext(Dispatchers.IO) {
        try {
            val request = productMutationRequest(baseUrl, "create_product", apiKey, username, productFields(product), image)

            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use Result.failure(Exception("HTTP ${response.code}: $body"))
                }
                val json = JSONObject(body)
                if (!json.optBoolean("success", false)) {
                    return@use Result.failure(Exception(json.optString("error", "Creazione prodotto rifiutata")))
                }
                val created = json.optJSONObject("product")
                    ?: return@use Result.failure(Exception("Risposta prodotto incompleta"))
                val productId = created.optString("product_id")
                if (productId.isBlank()) {
                    return@use Result.failure(Exception("ID prodotto mancante nella risposta"))
                }
                Result.success(
                    product.copy(
                        id = productId,
                        specialPrice = null,
                        quantity = created.optInt("quantity", product.quantity),
                        minQuantityAlert = created.optInt("minimum", product.minQuantityAlert),
                        status = created.optBoolean("status", product.status)
                    )
                )
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun productFields(product: Product): Map<String, String> = mapOf(
        "name" to product.name,
        "model" to product.model,
        "sku" to product.sku,
        "price" to product.price.toString(),
        "quantity" to product.quantity.coerceAtLeast(0).toString(),
        "minimum" to product.minQuantityAlert.coerceAtLeast(1).toString(),
        "category" to product.category,
        "description" to product.description,
        "status" to if (product.status) "1" else "0"
    )

    private fun productMutationRequest(
        baseUrl: String,
        action: String,
        apiKey: String,
        username: String,
        fields: Map<String, String>,
        image: ProductImageUpload?
    ): Request = if (image == null) {
        BridgeRequestFactory.authenticatedFormPost(baseUrl, action, apiKey, username, fields)
    } else {
        BridgeRequestFactory.authenticatedMultipartPost(
            baseUrl, action, apiKey, username, fields,
            image.bytes, image.mimeType, image.fileName
        )
    }

    suspend fun deleteProduct(
        baseUrl: String,
        apiKey: String,
        productId: String,
        username: String = ""
    ): Result<Boolean> = executeCatalogMutation(
        baseUrl = baseUrl,
        apiKey = apiKey,
        username = username,
        action = "delete_product",
        fields = mapOf("product_id" to productId.removePrefix("prod_").removePrefix("product_")),
        fallbackError = "Eliminazione prodotto rifiutata"
    )

    suspend fun createCategory(
        baseUrl: String,
        apiKey: String,
        category: Category,
        username: String = ""
    ): Result<Category> = withContext(Dispatchers.IO) {
        try {
            val request = BridgeRequestFactory.authenticatedFormPost(
                baseUrl = baseUrl,
                action = "create_category",
                apiKey = apiKey,
                username = username,
                fields = mapOf(
                    "name" to category.name,
                    "description" to category.description,
                    "sort_order" to category.sortOrder.coerceAtLeast(0).toString(),
                    "status" to if (category.status) "1" else "0"
                )
            )
            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use Result.failure(Exception("HTTP ${response.code}: $body"))
                }
                val json = JSONObject(body)
                if (!json.optBoolean("success", false)) {
                    return@use Result.failure(Exception(json.optString("error", "Creazione categoria rifiutata")))
                }
                val created = json.optJSONObject("category")
                    ?: return@use Result.failure(Exception("Risposta categoria incompleta"))
                val categoryId = created.optString("category_id")
                if (categoryId.isBlank()) {
                    return@use Result.failure(Exception("ID categoria mancante nella risposta"))
                }
                Result.success(
                    category.copy(
                        id = categoryId,
                        productsCount = created.optInt("products_count", 0),
                        status = created.optBoolean("status", category.status)
                    )
                )
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun updateCategory(
        baseUrl: String,
        apiKey: String,
        category: Category,
        username: String = ""
    ): Result<Boolean> = executeCatalogMutation(
        baseUrl = baseUrl,
        apiKey = apiKey,
        username = username,
        action = "update_category",
        fields = mapOf(
            "category_id" to category.id.removePrefix("cat_").removePrefix("category_"),
            "name" to category.name,
            "description" to category.description,
            "sort_order" to category.sortOrder.coerceAtLeast(0).toString(),
            "status" to if (category.status) "1" else "0"
        ),
        fallbackError = "Aggiornamento categoria rifiutato"
    )

    suspend fun deleteCategory(
        baseUrl: String,
        apiKey: String,
        categoryId: String,
        username: String = ""
    ): Result<Boolean> = executeCatalogMutation(
        baseUrl = baseUrl,
        apiKey = apiKey,
        username = username,
        action = "delete_category",
        fields = mapOf("category_id" to categoryId.removePrefix("cat_").removePrefix("category_")),
        fallbackError = "Eliminazione categoria rifiutata"
    )

    private suspend fun executeCatalogMutation(
        baseUrl: String,
        apiKey: String,
        username: String,
        action: String,
        fields: Map<String, String>,
        fallbackError: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = BridgeRequestFactory.authenticatedFormPost(baseUrl, action, apiKey, username, fields)
            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                } else {
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false)) Result.success(true)
                    else Result.failure(Exception(json.optString("error", fallbackError)))
                }
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /** Recupera soltanto la telemetria realmente registrata da OpenCart. */
    suspend fun fetchVisitorTelemetry(
        baseUrl: String,
        apiKey: String,
        username: String = ""
    ): Result<VisitorRealtimeStats> = withContext(Dispatchers.IO) {
        try {
            val request = BridgeRequestFactory.authenticatedGet(
                baseUrl = baseUrl,
                action = "visitor_telemetry",
                apiKey = apiKey,
                username = username
            )

            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use Result.failure(Exception("HTTP ${response.code}: $body"))
                }

                val json = JSONObject(body)
                if (!json.optBoolean("success", false)) {
                    return@use Result.failure(Exception(json.optString("error", "Telemetria non disponibile")))
                }

                val history = json.optJSONArray("traffic_history") ?: JSONArray()
                val pages = json.optJSONArray("top_pages") ?: JSONArray()
                val countries = json.optJSONArray("top_countries") ?: JSONArray()
                val sources = json.optJSONArray("traffic_sources") ?: JSONArray()
                val devices = json.optJSONArray("device_stats") ?: JSONArray()
                val events = json.optJSONArray("live_events") ?: JSONArray()

                Result.success(
                    VisitorRealtimeStats(
                        trackingEnabled = json.optBoolean("tracking_enabled", false),
                        dataAvailable = json.optBoolean("data_available", false),
                        activeVisitorsNow = json.optInt("active_visitors_now", 0),
                        guestVisitorsNow = json.optInt("guest_visitors_now", 0),
                        registeredVisitorsNow = json.optInt("registered_visitors_now", 0),
                        pageViewsPerMin = json.optInt("page_updates_per_min", 0),
                        activeCartsCount = json.optInt("active_carts_count", 0),
                        activeCheckoutsCount = json.optInt("active_checkouts_count", 0),
                        avgDurationSeconds = json.optInt("avg_duration_seconds", 0),
                        bounceRate = json.optDouble("bounce_rate", 0.0),
                        trafficHistory = (0 until history.length()).map { index ->
                            history.getJSONObject(index).let { item ->
                                LiveVisitorPoint(
                                    timeLabel = item.optString("time_label"),
                                    activeUsers = item.optInt("active_users", 0),
                                    pageViews = item.optInt("page_views", 0)
                                )
                            }
                        },
                        topPages = (0 until pages.length()).map { index ->
                            pages.getJSONObject(index).let { item ->
                                ActivePageVisit(
                                    path = item.optString("path", "/"),
                                    title = item.optString("title", "Pagina OpenCart"),
                                    activeUsers = item.optInt("active_users", 0),
                                    percentage = item.optDouble("percentage", 0.0),
                                    category = item.optString("category", "OpenCart")
                                )
                            }
                        },
                        topCountries = (0 until countries.length()).map { index ->
                            countries.getJSONObject(index).let { item ->
                                GeoVisitor(
                                    country = item.optString("country"),
                                    countryCode = item.optString("country_code"),
                                    flagEmoji = item.optString("flag_emoji"),
                                    topCities = item.optString("top_cities"),
                                    visitorsCount = item.optInt("visitors_count", 0),
                                    percentage = item.optDouble("percentage", 0.0)
                                )
                            }
                        },
                        trafficSources = (0 until sources.length()).map { index ->
                            sources.getJSONObject(index).let { item ->
                                TrafficSource(
                                    source = item.optString("source", "Accesso diretto"),
                                    type = item.optString("type", "Direct"),
                                    visitorsCount = item.optInt("visitors_count", 0),
                                    percentage = item.optDouble("percentage", 0.0),
                                    conversionRate = item.optDouble("conversion_rate", 0.0)
                                )
                            }
                        },
                        deviceStats = (0 until devices.length()).map { index ->
                            devices.getJSONObject(index).let { item ->
                                DeviceBreakdown(
                                    deviceType = item.optString("device_type"),
                                    count = item.optInt("count", 0),
                                    percentage = item.optDouble("percentage", 0.0),
                                    iconName = item.optString("icon_name")
                                )
                            }
                        },
                        liveEvents = (0 until events.length()).map { index ->
                            events.getJSONObject(index).let { item ->
                                LiveVisitorEvent(
                                    id = item.optString("id", "event_$index"),
                                    timestamp = item.optString("timestamp"),
                                    eventType = item.optString("event_type", "PAGE_VIEW"),
                                    description = item.optString("description"),
                                    location = item.optString("location"),
                                    iconType = item.optString("icon_type", "page")
                                )
                            }
                        },
                        source = json.optString("source"),
                        lastUpdated = json.optString("last_updated"),
                        limitations = json.optString("limitations")
                    )
                )
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    /**
     * Aggiorna la giacenza di un prodotto direttamente su OpenCart.
     */
    suspend fun updateProductStock(baseUrl: String, apiKey: String, productId: String, newStock: Int, username: String = ""): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim().removeSuffix("/")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val request = BridgeRequestFactory.authenticatedFormPost(
                baseUrl = cleanUrl,
                action = "update_stock",
                apiKey = apiKey,
                username = username,
                fields = mapOf("product_id" to productId, "quantity" to newStock.toString())
            )

            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                } else {
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false)) Result.success(true)
                    else Result.failure(Exception(json.optString("error", "Aggiornamento quantità rifiutato")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aggiorna lo stato di un ordine direttamente su OpenCart.
     */
    suspend fun updateOrderStatus(baseUrl: String, apiKey: String, orderId: String, statusId: Int, comment: String, username: String = ""): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim().removeSuffix("/")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val request = BridgeRequestFactory.authenticatedFormPost(
                baseUrl = cleanUrl,
                action = "update_order_status",
                apiKey = apiKey,
                username = username,
                fields = mapOf(
                    "order_id" to orderId,
                    "status_id" to statusId.toString(),
                    "comment" to comment
                )
            )

            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                } else {
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false)) Result.success(true)
                    else Result.failure(Exception(json.optString("error", "Aggiornamento ordine rifiutato")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera gli abbonamenti e pagamenti ricorrenti da OpenCart.
     */
    suspend fun fetchSubscriptions(baseUrl: String, apiKey: String, username: String = "", limit: Int = 50): Result<List<Subscription>> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim().removeSuffix("/")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val request = BridgeRequestFactory.authenticatedGet(
                baseUrl = cleanUrl,
                action = "subscriptions",
                apiKey = apiKey,
                username = username,
                queryParameters = mapOf("limit" to limit.toString())
            )

            tlsClient.execute(request).use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val subsArray = json.optJSONArray("subscriptions") ?: JSONArray()
                    val list = mutableListOf<Subscription>()
                    for (i in 0 until subsArray.length()) {
                        val obj = subsArray.getJSONObject(i)
                        val stStr = obj.optString("status", "ACTIVE")
                        val st = try {
                            SubscriptionStatus.valueOf(stStr.uppercase())
                        } catch (_: Exception) {
                            SubscriptionStatus.ACTIVE
                        }

                        list.add(
                            Subscription(
                                id = obj.optString("id", "sub_$i"),
                                subscriptionId = obj.optString("subscription_id", "#SUB-$i"),
                                customerName = obj.optString("customer_name", "Cliente"),
                                customerEmail = obj.optString("customer_email", "email@store.it"),
                                planName = obj.optString("plan_name", "Piano Ricorrente"),
                                cycleFrequency = obj.optString("cycle_frequency", "Mensile (30 gg)"),
                                amount = obj.optDouble("amount", 29.90),
                                status = st,
                                nextPaymentDate = obj.optString("next_payment_date", "2026-09-25"),
                                startDate = obj.optString("start_date", "2026-08-01"),
                                paymentMethod = obj.optString("payment_method", "Stripe Ricorrente")
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recupera i resi merce RMA da OpenCart.
     */
    suspend fun fetchReturns(baseUrl: String, apiKey: String, username: String = "", limit: Int = 50): Result<List<OrderReturn>> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim().removeSuffix("/")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val request = BridgeRequestFactory.authenticatedGet(
                baseUrl = cleanUrl,
                action = "returns",
                apiKey = apiKey,
                username = username,
                queryParameters = mapOf("limit" to limit.toString())
            )

            tlsClient.execute(request).use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val retArray = json.optJSONArray("returns") ?: JSONArray()
                    val list = mutableListOf<OrderReturn>()
                    for (i in 0 until retArray.length()) {
                        val obj = retArray.getJSONObject(i)
                        val stStr = obj.optString("status", "PENDING")
                        val st = try {
                            ReturnStatus.valueOf(stStr.uppercase())
                        } catch (_: Exception) {
                            ReturnStatus.PENDING
                        }

                        list.add(
                            OrderReturn(
                                id = obj.optString("id", "ret_$i"),
                                returnId = obj.optString("return_id", "RMA-$i"),
                                orderId = obj.optString("order_id", "#100$i"),
                                customerName = obj.optString("customer_name", "Cliente"),
                                customerEmail = obj.optString("customer_email", "email@store.it"),
                                customerPhone = obj.optString("customer_phone", ""),
                                productName = obj.optString("product_name", "Prodotto Reso"),
                                productModel = obj.optString("product_model", "MOD-1"),
                                quantity = obj.optInt("quantity", 1),
                                reason = obj.optString("reason", "Difettoso / Danneggiato"),
                                opened = obj.optBoolean("opened", true),
                                status = st,
                                action = obj.optString("action", "In attesa di verifica"),
                                dateAdded = obj.optString("date_added", "2026-08-20"),
                                comment = obj.optString("comment", "")
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aggiorna lo stato di un abbonamento su OpenCart.
     */
    suspend fun updateSubscriptionStatus(baseUrl: String, apiKey: String, subscriptionId: String, newStatus: String, username: String = ""): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim().removeSuffix("/")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val rawId = subscriptionId.removePrefix("#SUB-").removePrefix("#").removePrefix("sub_")

            val request = BridgeRequestFactory.authenticatedFormPost(
                baseUrl = cleanUrl,
                action = "update_subscription_status",
                apiKey = apiKey,
                username = username,
                fields = mapOf("subscription_id" to rawId, "status" to newStatus)
            )

            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                } else {
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false)) Result.success(true)
                    else Result.failure(Exception(json.optString("error", "Aggiornamento abbonamento rifiutato")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Aggiorna lo stato di un reso su OpenCart.
     */
    suspend fun updateReturnStatus(baseUrl: String, apiKey: String, returnId: String, statusId: Int, comment: String, username: String = ""): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim().removeSuffix("/")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val rawId = returnId.removePrefix("RMA-").removePrefix("#").removePrefix("ret_")

            val request = BridgeRequestFactory.authenticatedFormPost(
                baseUrl = cleanUrl,
                action = "update_return_status",
                apiKey = apiKey,
                username = username,
                fields = mapOf(
                    "return_id" to rawId,
                    "status_id" to statusId.toString(),
                    "comment" to comment
                )
            )

            tlsClient.execute(request).use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                } else {
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false)) Result.success(true)
                    else Result.failure(Exception(json.optString("error", "Aggiornamento reso rifiutato")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Log an audit event to OpenCart remote database
     */
    suspend fun logAuditRemote(
        store: Store,
        actionType: String,
        description: String,
        details: String?,
        operator: String,
        device: String
    ): Boolean = withContext(Dispatchers.IO) {
        var cleanUrl = store.url.trim().removeSuffix("/")
        if (cleanUrl.isBlank() || store.apiKey.isBlank()) return@withContext false
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        val bridgeUrl = "$cleanUrl/extension/cartadmin/cartadmin_api.php?action=audit_log"
        val payload = JSONObject().apply {
            put("action_type", actionType)
            put("description", description)
            put("details", details ?: "")
            put("operator_username", operator)
            put("device_model", device)
        }

        val request = BridgeRequestFactory.authenticate(
            Request.Builder()
                .url(bridgeUrl)
                .post(payload.toString().toRequestBody(jsonMediaType)),
            store.apiKey,
            store.apiUsername
        ).build()

        try {
            tlsClient.execute(request).use { res ->
                res.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
