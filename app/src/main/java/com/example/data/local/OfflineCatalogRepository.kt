package com.example.data.local

import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.model.Category
import com.example.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineCatalogRepository(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao
) {
    fun getProductsFlow(storeId: String = "store_1"): Flow<List<Product>> {
        return productDao.getProductsByStoreFlow(storeId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getCategoriesFlow(storeId: String = "store_1"): Flow<List<Category>> {
        return categoryDao.getCategoriesByStoreFlow(storeId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun cacheProducts(products: List<Product>, storeId: String = "store_1") {
        val entities = products.map { ProductEntity.fromDomainModel(it, storeId) }
        productDao.insertProducts(entities)
    }

    suspend fun saveProduct(product: Product, storeId: String = "store_1") {
        productDao.insertProduct(ProductEntity.fromDomainModel(product, storeId))
    }

    suspend fun deleteProduct(productId: String) {
        productDao.deleteProductById(productId)
    }

    suspend fun updateProductStock(productId: String, quantity: Int) {
        productDao.updateStock(productId, quantity)
    }

    suspend fun updateProductStatus(productId: String, status: Boolean) {
        productDao.updateStatus(productId, status)
    }

    suspend fun cacheCategories(categories: List<Category>, storeId: String = "store_1") {
        val entities = categories.map { CategoryEntity.fromDomainModel(it, storeId) }
        categoryDao.insertCategories(entities)
    }

    suspend fun saveCategory(category: Category, storeId: String = "store_1") {
        categoryDao.insertCategory(CategoryEntity.fromDomainModel(category, storeId))
    }

    suspend fun deleteCategory(categoryId: String) {
        categoryDao.deleteCategoryById(categoryId)
    }

    suspend fun updateCategoryStatus(categoryId: String, status: Boolean) {
        categoryDao.updateCategoryStatus(categoryId, status)
    }
}
