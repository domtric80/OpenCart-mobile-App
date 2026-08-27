package com.example.network

import com.example.BuildConfig
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

internal object BridgeRequestFactory {

    fun authenticatedGet(
        baseUrl: String,
        action: String,
        apiKey: String,
        username: String = "",
        queryParameters: Map<String, String> = emptyMap()
    ): Request {
        require(apiKey.isNotBlank()) { "La chiave API non può essere vuota" }
        require(action.matches(Regex("[a-z_]+"))) { "Azione bridge non valida" }
        require(queryParameters.keys.none { it.equals("api_key", true) || it.equals("username", true) }) {
            "Le credenziali non possono essere inserite nella URL"
        }

        val endpoint = bridgeEndpoint(baseUrl)

        val urlBuilder = endpoint.newBuilder().addQueryParameter("action", action)
        queryParameters.forEach { (name, value) -> urlBuilder.addQueryParameter(name, value) }

        return authenticate(
            builder = Request.Builder()
                .url(urlBuilder.build())
                .get(),
            apiKey = apiKey,
            username = username
        ).build()
    }

    fun authenticatedFormPost(
        baseUrl: String,
        action: String,
        apiKey: String,
        username: String = "",
        fields: Map<String, String>
    ): Request {
        require(action.matches(Regex("[a-z_]+"))) { "Azione bridge non valida" }
        require(fields.keys.none { it.equals("api_key", true) || it.equals("username", true) }) {
            "Le credenziali non possono essere inserite nel form body"
        }

        val formBody = FormBody.Builder()
            .add("action", action)
            .apply { fields.forEach { (name, value) -> add(name, value) } }
            .build()

        return authenticate(
            builder = Request.Builder().url(bridgeEndpoint(baseUrl)).post(formBody),
            apiKey = apiKey,
            username = username
        ).build()
    }

    fun authenticate(
        builder: Request.Builder,
        apiKey: String,
        username: String = ""
    ): Request.Builder {
        require(apiKey.isNotBlank()) { "La chiave API non può essere vuota" }
        return builder
            .header("X-CartAdmin-Key", apiKey)
            .apply {
                if (username.isNotBlank()) {
                    header("X-CartAdmin-User", username)
                }
            }
            .header("User-Agent", "CartAdmin-Android/${BuildConfig.VERSION_NAME}")
            .header("Accept", "application/json")
    }

    private fun bridgeEndpoint(baseUrl: String) = baseUrl.trim().removeSuffix("/").let {
        val normalized = if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
        val endpoint = "$normalized/cartadmin_api.php".toHttpUrlOrNull()
            ?: throw IllegalArgumentException("URL OpenCart non valida")
        require(endpoint.isHttps) { "CartAdmin richiede HTTPS" }
        require(endpoint.username.isEmpty() && endpoint.password.isEmpty()) {
            "La URL dello store non può contenere credenziali"
        }
        endpoint
    }
}
