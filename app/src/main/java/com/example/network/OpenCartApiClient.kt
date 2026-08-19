package com.example.network

import com.example.model.Order
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
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
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
                message = "URL non valido. Assicurati di inserire un URL corretto (es. https://negozio.it)",
                details = "Formato URL errato"
            )
        }

        // 1. Check CartAdmin Bridge Module
        val bridgeUrl = "$cleanUrl/cartadmin_api.php?action=status&api_key=$apiKey"
        val bridgeRequest = Request.Builder()
            .url(bridgeUrl)
            .get()
            .header("X-CartAdmin-Key", apiKey)
            .header("User-Agent", "CartAdmin-Android/1.0")
            .build()

        try {
            client.newCall(bridgeRequest).execute().use { response ->
                val duration = System.currentTimeMillis() - startTime
                val body = response.body?.string() ?: ""

                if (response.isSuccessful && body.contains("\"success\":true")) {
                    val json = try { JSONObject(body) } catch (e: Exception) { null }
                    val ocVer = json?.optString("opencart_version", "3.x/4.x") ?: "3.x/4.x"
                    val storeName = json?.optString("store_name", "OpenCart Store") ?: "OpenCart Store"
                    val ordersCount = json?.optInt("total_orders", 0) ?: 0

                    return@withContext OpenCartConnectionResult(
                        isSuccess = true,
                        statusCode = response.code,
                        responseTimeMs = duration,
                        message = "Modulo CartAdmin Bridge attivo e connesso a $storeName!",
                        apiToken = apiKey,
                        details = "OpenCart v$ocVer • Ordini nel DB: $ordersCount • Risposta in ${duration}ms.",
                        isBridgeDetected = true
                    )
                } else if (response.code == 401) {
                    return@withContext OpenCartConnectionResult(
                        isSuccess = false,
                        statusCode = 401,
                        responseTimeMs = duration,
                        message = "Modulo trovato su $cleanUrl ma la chiave API non corrisponde.",
                        details = "Verifica che la chiave CARTADMIN_SECRET_KEY nel file cartadmin_api.php sia identica alla chiave nell'app."
                    )
                }
            }
        } catch (e: Exception) {
            // Bridge might not be uploaded yet, fallback to check OpenCart native
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
            .header("User-Agent", "CartAdmin-Android/1.0")
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
                            message = "OpenCart errore: $errorMsg",
                            details = "Consiglio: Per evitare problemi di IP dinamico dello smartphone, carica il modulo PHP incluso nell'app (cartadmin_api.php)."
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
                        message = "Server ha risposto HTTP $code: ${response.message}",
                        details = "Carica il file cartadmin_api.php nella cartella root del tuo OpenCart per abilitare la sincronizzazione istantanea."
                    )
                }
            }
        } catch (e: IOException) {
            val duration = System.currentTimeMillis() - startTime
            return@withContext OpenCartConnectionResult(
                isSuccess = false,
                statusCode = 0,
                responseTimeMs = duration,
                message = "Impossibile raggiungere lo store: ${e.localizedMessage ?: "Timeout"}",
                details = "Verifica che il sito $cleanUrl sia online e raggiungibile."
            )
        }
    }

    /**
     * Update order status on OpenCart via CartAdmin Bridge or native API
     */
    suspend fun updateOrderStatusOnOpenCart(
        store: Store,
        orderId: String,
        status: OrderStatus,
        comment: String = "Aggiornato da CartAdmin App"
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanUrl = store.url.trim().removeSuffix("/")
        
        // 1. Try Bridge first
        val bridgeUrl = "$cleanUrl/cartadmin_api.php?action=order_status"
        val payload = JSONObject().apply {
            put("order_id", orderId)
            put("status", status.name)
            put("comment", comment)
            put("notify", 1)
        }
        val bridgeReq = Request.Builder()
            .url(bridgeUrl)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .header("X-CartAdmin-Key", store.apiKey)
            .build()

        try {
            client.newCall(bridgeReq).execute().use { res ->
                if (res.isSuccessful && res.body?.string()?.contains("\"success\":true") == true) {
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            // fallback to native
        }

        // 2. Native fallback
        val nativeUrl = "$cleanUrl/index.php?route=api/order/history&api_token=${store.apiToken ?: ""}"
        val statusId = when (status) {
            OrderStatus.PENDING -> "1"
            OrderStatus.PROCESSING -> "2"
            OrderStatus.SHIPPED -> "3"
            OrderStatus.COMPLETE, OrderStatus.DELIVERED -> "5"
            OrderStatus.CANCELLED -> "7"
            OrderStatus.CONFIRMED -> "15"
        }
        val formBody = FormBody.Builder()
            .add("order_id", orderId.replace("ord_", ""))
            .add("order_status_id", statusId)
            .add("comment", comment)
            .add("notify", "1")
            .build()

        val nativeReq = Request.Builder()
            .url(nativeUrl)
            .post(formBody)
            .build()

        try {
            client.newCall(nativeReq).execute().use { res ->
                res.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update product stock quantity on OpenCart
     */
    suspend fun updateProductQuantityOnOpenCart(
        store: Store,
        productId: String,
        newQuantity: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanUrl = store.url.trim().removeSuffix("/")

        // Try Bridge
        val bridgeUrl = "$cleanUrl/cartadmin_api.php?action=update_stock"
        val payload = JSONObject().apply {
            put("product_id", productId)
            put("quantity", newQuantity)
        }
        val bridgeReq = Request.Builder()
            .url(bridgeUrl)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .header("X-CartAdmin-Key", store.apiKey)
            .build()

        try {
            client.newCall(bridgeReq).execute().use { res ->
                if (res.isSuccessful && res.body?.string()?.contains("\"success\":true") == true) {
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        true
    }

    /**
     * Synchronize audit log action directly to the remote OpenCart store database (cartadmin_audit table)
     */
    suspend fun sendAuditLogToOpenCart(
        store: Store,
        actionType: String,
        description: String,
        details: String?,
        operator: String,
        device: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanUrl = store.url.trim().removeSuffix("/")
        if (cleanUrl.isBlank() || store.apiKey.isBlank()) return@withContext false

        val bridgeUrl = "$cleanUrl/cartadmin_api.php?action=log_audit"
        val payload = JSONObject().apply {
            put("action_type", actionType)
            put("description", description)
            put("details", details ?: "")
            put("operator", operator)
            put("device", device)
        }

        val request = Request.Builder()
            .url(bridgeUrl)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .header("X-CartAdmin-Key", store.apiKey)
            .header("User-Agent", "CartAdmin-Android/1.1.1")
            .build()

        try {
            client.newCall(request).execute().use { res ->
                res.isSuccessful && res.body?.string()?.contains("\"success\":true") == true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Fetch remote audit logs registered on the OpenCart server
     */
    suspend fun fetchOpenCartAuditLogs(store: Store): List<com.example.model.AuditLog> = withContext(Dispatchers.IO) {
        val cleanUrl = store.url.trim().removeSuffix("/")
        if (cleanUrl.isBlank() || store.apiKey.isBlank()) return@withContext emptyList()

        val bridgeUrl = "$cleanUrl/cartadmin_api.php?action=audit_logs&limit=100"
        val request = Request.Builder()
            .url(bridgeUrl)
            .get()
            .header("X-CartAdmin-Key", store.apiKey)
            .header("User-Agent", "CartAdmin-Android/1.1.1")
            .build()

        try {
            client.newCall(request).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string() ?: ""
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false)) {
                        val logsArr = json.optJSONArray("logs") ?: return@withContext emptyList()
                        val result = mutableListOf<com.example.model.AuditLog>()
                        for (i in 0 until logsArr.length()) {
                            val item = logsArr.getJSONObject(i)
                            val actType = try {
                                com.example.model.AuditActionType.valueOf(item.optString("actionType", "SYSTEM_LOGIN"))
                            } catch (_: Exception) {
                                com.example.model.AuditActionType.SYSTEM_LOGIN
                            }
                            result.add(
                                com.example.model.AuditLog(
                                    id = item.optString("id", "oc_$i"),
                                    timestamp = item.optString("timestamp", "Recente"),
                                    timestampMillis = System.currentTimeMillis(),
                                    operatorUsername = item.optString("operatorUsername", "admin"),
                                    actionType = actType,
                                    description = item.optString("description", ""),
                                    details = item.optString("details", null),
                                    deviceModel = item.optString("deviceModel", "Android"),
                                    androidVersion = "OpenCart Server Log",
                                    appVersion = "1.1.2",
                                    storeName = store.name,
                                    apiProfileUsed = "OpenCart Database (cartadmin_audit)"
                                )
                            )
                        }
                        return@withContext result
                    }
                }
            }
        } catch (_: Exception) {}
        emptyList()
    }
}
