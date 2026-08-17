package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class OrderItem(
    val productId: Long,
    val name: String,
    val model: String,
    val quantity: Int,
    val price: Double,
    val total: Double,
    val options: String = ""
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("productId", productId)
        json.put("name", name)
        json.put("model", model)
        json.put("quantity", quantity)
        json.put("price", price)
        json.put("total", total)
        json.put("options", options)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): OrderItem {
            return OrderItem(
                productId = json.optLong("productId", 0),
                name = json.optString("name", "Prodotto"),
                model = json.optString("model", ""),
                quantity = json.optInt("quantity", 1),
                price = json.optDouble("price", 0.0),
                total = json.optDouble("total", 0.0),
                options = json.optString("options", "")
            )
        }

        fun parseList(jsonString: String): List<OrderItem> {
            if (jsonString.isBlank()) return emptyList()
            val list = mutableListOf<OrderItem>()
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                // Fallback
            }
            return list
        }

        fun serializeList(items: List<OrderItem>): String {
            val array = JSONArray()
            items.forEach { array.put(it.toJson()) }
            return array.toString()
        }
    }
}

data class OrderHistoryItem(
    val status: String,
    val comment: String,
    val notified: Boolean,
    val dateAdded: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("status", status)
        json.put("comment", comment)
        json.put("notified", notified)
        json.put("dateAdded", dateAdded)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): OrderHistoryItem {
            return OrderHistoryItem(
                status = json.optString("status", "In attesa"),
                comment = json.optString("comment", ""),
                notified = json.optBoolean("notified", false),
                dateAdded = json.optLong("dateAdded", System.currentTimeMillis())
            )
        }

        fun parseList(jsonString: String): List<OrderHistoryItem> {
            if (jsonString.isBlank()) return emptyList()
            val list = mutableListOf<OrderHistoryItem>()
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                // Fallback
            }
            return list
        }

        fun serializeList(history: List<OrderHistoryItem>): String {
            val array = JSONArray()
            history.forEach { array.put(it.toJson()) }
            return array.toString()
        }
    }
}

data class SalesKpis(
    val todaySales: Double = 0.0,
    val weeklySales: Double = 0.0,
    val monthlySales: Double = 0.0,
    val yearlySales: Double = 0.0,
    val todayOrdersCount: Int = 0,
    val totalOrdersCount: Int = 0,
    val totalCustomersCount: Int = 0,
    val lowStockCount: Int = 0,
    val averageOrderValue: Double = 0.0,
    val pendingOrdersCount: Int = 0,
    val processingOrdersCount: Int = 0
)

data class DaySales(
    val dayLabel: String,
    val amount: Double,
    val orderCount: Int
)

enum class OrderStatusType(val label: String, val badgeColorHex: Long) {
    PENDING("In attesa", 0xFFF59E0B),
    PROCESSING("In lavorazione", 0xFF3B82F6),
    CONFIRMED("Confermato", 0xFF6366F1),
    SHIPPED("Spedito", 0xFF8B5CF6),
    DELIVERED("Consegnato", 0xFF10B981),
    CANCELLED("Annullato", 0xFFEF4444),
    REFUNDED("Rimborsato", 0xFF64748B)
}
