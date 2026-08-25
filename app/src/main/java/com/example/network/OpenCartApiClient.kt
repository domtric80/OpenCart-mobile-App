package com.example.network

import android.util.Log
import com.example.model.Order
import com.example.model.OrderDetail
import com.example.model.OrderItem
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class OpenCartConnectionResult(
    val isSuccess: Boolean,
    val statusCode: Int,
    val responseTimeMs: Long,
    val message: String,
    val apiToken: String? = null,
    val details: String? = null,
    val isBridgeDetected: Boolean = false
)

class OpenCartApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Test connection to OpenCart store:
     * 1. Checks for CartAdmin Bridge script (cartadmin_api.php?action=status)
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
        val encodedKey = java.net.URLEncoder.encode(apiKey, "UTF-8")
        val encodedUser = java.net.URLEncoder.encode(apiUsername, "UTF-8")
        val bridgeUrl = "$cleanUrl/cartadmin_api.php?action=status&api_key=$encodedKey&username=$encodedUser"
        val bridgeRequest = Request.Builder()
            .url(bridgeUrl)
            .get()
            .header("X-CartAdmin-Key", apiKey)
            .header("X-CartAdmin-User", apiUsername)
            .header("Authorization", "Bearer $apiKey")
            .header("User-Agent", "CartAdmin-Android/1.2.2")
            .build()

        try {
            client.newCall(bridgeRequest).execute().use { response ->
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
            .header("User-Agent", "CartAdmin-Android/1.2.2")
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(nativeRequest).execute().use { response ->
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
                            details = "Consiglio: Installa l'estensione cartadmin_api.php per bypassare il blocco IP dinamico di OpenCart."
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
                        details = "Carica il file cartadmin_api.php nella cartella principale del tuo OpenCart oppure verifica i permessi API."
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
            val encodedKey = java.net.URLEncoder.encode(apiKey, "UTF-8")
            val encodedUser = java.net.URLEncoder.encode(username, "UTF-8")
            val endpoint = "$cleanUrl/cartadmin_api.php?action=orders&limit=$limit&api_key=$encodedKey&username=$encodedUser"

            val request = Request.Builder()
                .url(endpoint)
                .get()
                .header("X-CartAdmin-Key", apiKey)
                .header("X-CartAdmin-User", username)
                .header("Authorization", "Bearer $apiKey")
                .header("User-Agent", "CartAdmin-Android/1.2.2")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
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
            val encodedKey = java.net.URLEncoder.encode(apiKey, "UTF-8")
            val encodedUser = java.net.URLEncoder.encode(username, "UTF-8")
            val endpoint = "$cleanUrl/cartadmin_api.php?action=products&limit=$limit&api_key=$encodedKey&username=$encodedUser"

            val request = Request.Builder()
                .url(endpoint)
                .get()
                .header("X-CartAdmin-Key", apiKey)
                .header("X-CartAdmin-User", username)
                .header("Authorization", "Bearer $apiKey")
                .header("User-Agent", "CartAdmin-Android/1.2.2")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
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
                    val status = obj.optBoolean("status", true)
                    val category = obj.optString("category", "Catalogo OpenCart")

                    list.add(
                        Product(
                            id = prodId,
                            name = cleanName,
                            model = model,
                            sku = if (sku.isNotBlank()) sku else model,
                            price = price,
                            specialPrice = null,
                            quantity = qty,
                            minQuantityAlert = 5,
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
     * Aggiorna la giacenza di un prodotto direttamente su OpenCart.
     */
    suspend fun updateProductStock(baseUrl: String, apiKey: String, productId: String, newStock: Int, username: String = ""): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            var cleanUrl = baseUrl.trim().removeSuffix("/")
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://$cleanUrl"
            }
            val endpoint = "$cleanUrl/cartadmin_api.php"

            val formBody = FormBody.Builder()
                .add("action", "update_stock")
                .add("product_id", productId)
                .add("quantity", newStock.toString())
                .add("api_key", apiKey)
                .add("username", username)
                .build()

            val request = Request.Builder()
                .url(endpoint)
                .post(formBody)
                .header("X-CartAdmin-Key", apiKey)
                .header("X-CartAdmin-User", username)
                .header("Authorization", "Bearer $apiKey")
                .header("User-Agent", "CartAdmin-Android/1.2.2")
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
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
            val endpoint = "$cleanUrl/cartadmin_api.php"

            val formBody = FormBody.Builder()
                .add("action", "update_order_status")
                .add("order_id", orderId)
                .add("status_id", statusId.toString())
                .add("comment", comment)
                .add("api_key", apiKey)
                .add("username", username)
                .build()

            val request = Request.Builder()
                .url(endpoint)
                .post(formBody)
                .header("X-CartAdmin-Key", apiKey)
                .header("X-CartAdmin-User", username)
                .header("Authorization", "Bearer $apiKey")
                .header("User-Agent", "CartAdmin-Android/1.2.2")
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
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

        val bridgeUrl = "$cleanUrl/cartadmin_api.php?action=audit_log"
        val payload = JSONObject().apply {
            put("action_type", actionType)
            put("description", description)
            put("details", details ?: "")
            put("operator_username", operator)
            put("device_model", device)
        }

        val request = Request.Builder()
            .url(bridgeUrl)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .header("X-CartAdmin-Key", store.apiKey)
            .header("User-Agent", "CartAdmin-Android/1.2.1")
            .build()

        try {
            client.newCall(request).execute().use { res ->
                res.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
