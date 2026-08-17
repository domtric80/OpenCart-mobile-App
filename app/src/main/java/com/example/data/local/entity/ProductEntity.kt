package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Product

@Entity(tableName = "products_cache")
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val storeId: String = "store_1",
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
) {
    fun toDomainModel(): Product = Product(
        id = id,
        name = name,
        model = model,
        sku = sku,
        price = price,
        specialPrice = specialPrice,
        quantity = quantity,
        minQuantityAlert = minQuantityAlert,
        category = category,
        description = description,
        status = status
    )

    companion object {
        fun fromDomainModel(product: Product, storeId: String = "store_1"): ProductEntity = ProductEntity(
            id = product.id,
            storeId = storeId,
            name = product.name,
            model = product.model,
            sku = if (product.sku.isNotBlank()) product.sku else "OC-${product.id.takeLast(4)}",
            price = product.price,
            specialPrice = product.specialPrice,
            quantity = product.quantity,
            minQuantityAlert = product.minQuantityAlert,
            category = product.category,
            description = product.description,
            status = product.status
        )
    }
}
