package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Category

@Entity(tableName = "categories_cache")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val storeId: String = "store_1",
    val name: String,
    val description: String = "",
    val sortOrder: Int = 0,
    val status: Boolean = true
) {
    fun toDomainModel(productsCount: Int = 0): Category = Category(
        id = id,
        name = name,
        description = description,
        productsCount = productsCount,
        status = status,
        sortOrder = sortOrder
    )

    companion object {
        fun fromDomainModel(category: Category, storeId: String = "store_1"): CategoryEntity = CategoryEntity(
            id = category.id,
            storeId = storeId,
            name = category.name,
            description = category.description,
            sortOrder = category.sortOrder,
            status = category.status
        )
    }
}
