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

    @Test
    fun telemetryRequestUsesAuthenticatedHttpsEndpoint() {
        val request = BridgeRequestFactory.authenticatedGet(
            baseUrl = "https://shop.example",
            action = "visitor_telemetry",
            apiKey = "telemetry-secret"
        )

        assertEquals("visitor_telemetry", request.url.queryParameter("action"))
        assertEquals("telemetry-secret", request.header("X-CartAdmin-Key"))
        assertFalse(request.url.toString().contains("telemetry-secret"))
        assertTrue(request.url.isHttps)
    }

    @Test
    fun productUpdateKeepsCredentialsOutOfFormData() {
        val request = BridgeRequestFactory.authenticatedFormPost(
            baseUrl = "https://shop.example",
            action = "update_product",
            apiKey = "product-secret",
            fields = mapOf("product_id" to "42", "name" to "Prodotto", "quantity" to "8")
        )
        val body = request.body as FormBody
        val names = (0 until body.size).map(body::name)

        assertEquals(listOf("action", "product_id", "name", "quantity"), names)
        assertFalse(names.contains("api_key"))
        assertFalse(names.contains("username"))
        assertEquals("product-secret", request.header("X-CartAdmin-Key"))
    }

    @Test
    fun catalogCrudMutationsKeepCredentialsInHeadersOnly() {
        val actions = listOf("create_product", "delete_product", "create_category", "update_category", "delete_category")

        actions.forEach { action ->
            val request = BridgeRequestFactory.authenticatedFormPost(
                baseUrl = "https://shop.example",
                action = action,
                apiKey = "catalog-secret",
                username = "operator",
                fields = mapOf("id" to "42")
            )
            val body = request.body as FormBody
            val names = (0 until body.size).map(body::name)

            assertEquals(action, body.value(0))
            assertFalse(names.contains("api_key"))
            assertFalse(names.contains("username"))
            assertEquals("catalog-secret", request.header("X-CartAdmin-Key"))
            assertEquals("operator", request.header("X-CartAdmin-User"))
        }
    }

    @Test
    fun managementListUsesAllowlistedModuleQueryAndHeaderOnlyCredentials() {
        val request = BridgeRequestFactory.authenticatedGet(
            baseUrl = "https://shop.example",
            action = "management_list",
            apiKey = "management-secret",
            queryParameters = mapOf("module" to "customers", "limit" to "100")
        )

        assertEquals("management_list", request.url.queryParameter("action"))
        assertEquals("customers", request.url.queryParameter("module"))
        assertEquals("management-secret", request.header("X-CartAdmin-Key"))
        assertFalse(request.url.toString().contains("management-secret"))
    }

    @Test
    fun managementStatusKeepsCredentialsOutOfMutationBody() {
        val request = BridgeRequestFactory.authenticatedFormPost(
            baseUrl = "https://shop.example",
            action = "management_status",
            apiKey = "management-secret",
            fields = mapOf("module" to "reviews", "id" to "12", "active" to "1")
        )
        val body = request.body as FormBody
        val names = (0 until body.size).map(body::name)

        assertEquals(listOf("action", "module", "id", "active"), names)
        assertFalse(names.contains("api_key"))
        assertEquals("management-secret", request.header("X-CartAdmin-Key"))
    }

    @Test
    fun sensitiveCommandUsesHeaderAuthenticationAndContainsOnlyQueueFields() {
        val request = BridgeRequestFactory.authenticatedFormPost(
            baseUrl = "https://shop.example",
            action = "management_command",
            apiKey = "queue-secret",
            username = "mobile-admin",
            fields = mapOf("module" to "gdpr", "id" to "7", "operation" to "approve")
        )
        val body = request.body as FormBody
        val names = (0 until body.size).map(body::name)

        assertEquals(listOf("action", "module", "id", "operation"), names)
        assertFalse(names.contains("api_key"))
        assertFalse(names.contains("username"))
        assertEquals("queue-secret", request.header("X-CartAdmin-Key"))
        assertEquals("mobile-admin", request.header("X-CartAdmin-User"))
    }

    @Test
    fun antispamMutationUsesAuthenticatedFormWithoutCredentials() {
        val request = BridgeRequestFactory.authenticatedFormPost(
            baseUrl = "https://shop.example",
            action = "management_antispam",
            apiKey = "antispam-secret",
            fields = mapOf("operation" to "add", "keyword" to "spamword")
        )
        val body = request.body as FormBody
        val names = (0 until body.size).map(body::name)

        assertEquals(listOf("action", "operation", "keyword"), names)
        assertFalse(names.contains("api_key"))
        assertEquals("antispam-secret", request.header("X-CartAdmin-Key"))
    }
}
