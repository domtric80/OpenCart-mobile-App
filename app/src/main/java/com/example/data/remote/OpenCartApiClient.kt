package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenCartApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    data class ApiTestResult(
        val success: Boolean,
        val message: String,
        val httpStatusCode: Int = 0,
        val detectedVersion: String = "OpenCart 3.x/4.x",
        val responseTimeMs: Long = 0
    )

    suspend fun testConnection(url: String, username: String, apiKey: String): ApiTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        if (!cleanUrl.endsWith("/")) {
            cleanUrl = "$cleanUrl/"
        }

        try {
            // Attempt standard OpenCart API login endpoint
            val loginUrl = "${cleanUrl}index.php?route=api/login"
            val formBody = FormBody.Builder()
                .add("username", username)
                .add("key", apiKey)
                .build()

            val request = Request.Builder()
                .url(loginUrl)
                .post(formBody)
                .header("User-Agent", "OpenCartManager-Android/1.0")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val elapsed = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                try {
                    val json = JSONObject(responseBody)
                    if (json.has("api_token") || json.has("token") || json.has("success")) {
                        return@withContext ApiTestResult(
                            success = true,
                            message = "Connessione stabilita con successo! Token API valido.",
                            httpStatusCode = response.code,
                            detectedVersion = if (json.has("api_token")) "OpenCart 3.x/4.x" else "OpenCart 2.x",
                            responseTimeMs = elapsed
                        )
                    } else if (json.has("error")) {
                        val error = json.optJSONObject("error")?.toString() ?: json.optString("error", "Errore credenziali API")
                        return@withContext ApiTestResult(
                            success = false,
                            message = "Server raggiungibile, ma OpenCart ha risposto: $error. Verifica l'IP autorizzato nel pannello Admin OpenCart.",
                            httpStatusCode = response.code,
                            responseTimeMs = elapsed
                        )
                    }
                } catch (e: Exception) {
                    // Might be HTML or general response
                }
                return@withContext ApiTestResult(
                    success = true,
                    message = "Server OpenCart risponde con HTTP ${response.code} ($elapsed ms).",
                    httpStatusCode = response.code,
                    responseTimeMs = elapsed
                )
            } else {
                return@withContext ApiTestResult(
                    success = false,
                    message = "Errore server HTTP ${response.code}: ${response.message}",
                    httpStatusCode = response.code,
                    responseTimeMs = elapsed
                )
            }
        } catch (e: Exception) {
            Log.e("OpenCartApiClient", "Error connecting to $cleanUrl", e)
            val elapsed = System.currentTimeMillis() - startTime
            return@withContext ApiTestResult(
                success = false,
                message = "Impossibile connettersi all'indirizzo: ${e.localizedMessage ?: "Timeout di rete o host sconosciuto"}",
                responseTimeMs = elapsed
            )
        }
    }
}
