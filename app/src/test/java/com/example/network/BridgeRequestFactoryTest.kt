package com.example.network

import okhttp3.FormBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeRequestFactoryTest {

    @Test
    fun credentialsAreHeadersOnly() {
        val request = BridgeRequestFactory.authenticatedGet(
            baseUrl = "https://shop.example/opencart",
            action = "orders",
            apiKey = "secret-key-value",
            username = "api-user",
            queryParameters = mapOf("limit" to "50")
        )

        assertEquals("secret-key-value", request.header("X-CartAdmin-Key"))
        assertEquals("api-user", request.header("X-CartAdmin-User"))
        assertNull(request.header("Authorization"))
        assertEquals("orders", request.url.queryParameter("action"))
        assertEquals("50", request.url.queryParameter("limit"))
        assertNull(request.url.queryParameter("api_key"))
        assertNull(request.url.queryParameter("username"))
        assertFalse(request.url.toString().contains("secret-key-value"))
        assertTrue(request.url.isHttps)
        assertEquals("/opencart/extension/cartadmin/cartadmin_api.php", request.url.encodedPath)
    }

    @Test
    fun cleartextBaseUrlIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BridgeRequestFactory.authenticatedGet(
                baseUrl = "http://shop.example",
                action = "status",
                apiKey = "secret-key-value"
            )
        }
    }

    @Test
    fun credentialQueryParametersAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BridgeRequestFactory.authenticatedGet(
                baseUrl = "https://shop.example",
                action = "orders",
                apiKey = "secret-key-value",
                queryParameters = mapOf("api_key" to "leaked")
            )
        }
    }

    @Test
    fun formPostKeepsCredentialsOutOfTheBody() {
        val request = BridgeRequestFactory.authenticatedFormPost(
            baseUrl = "https://shop.example",
            action = "update_stock",
            apiKey = "secret-key-value",
            username = "api-user",
            fields = mapOf("product_id" to "42", "quantity" to "5")
        )
        val body = request.body as FormBody
        val names = (0 until body.size).map(body::name)

        assertEquals(listOf("action", "product_id", "quantity"), names)
        assertFalse(names.contains("api_key"))
        assertFalse(names.contains("username"))
        assertEquals("secret-key-value", request.header("X-CartAdmin-Key"))
        assertEquals("api-user", request.header("X-CartAdmin-User"))
    }

    @Test
    fun credentialFormFieldsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            BridgeRequestFactory.authenticatedFormPost(
                baseUrl = "https://shop.example",
                action = "update_stock",
                apiKey = "secret-key-value",
                fields = mapOf("username" to "leaked")
            )
        }
    }
}
