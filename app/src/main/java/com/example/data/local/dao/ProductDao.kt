package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products_cache WHERE storeId = :storeId ORDER BY name ASC")
    fun getProductsByStoreFlow(storeId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products_cache WHERE storeId = :storeId ORDER BY name ASC")
    suspend fun getProductsByStore(storeId: String): List<ProductEntity>

    @Query("SELECT * FROM products_cache WHERE storeId = :storeId AND category = :category ORDER BY name ASC")
    fun getProductsByCategoryFlow(storeId: String, category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products_cache WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products_cache WHERE id = :productId")
    suspend fun deleteProductById(productId: String)

    @Query("UPDATE products_cache SET quantity = :quantity WHERE id = :productId")
    suspend fun updateStock(productId: String, quantity: Int)

    @Query("UPDATE products_cache SET status = :status WHERE id = :productId")
    suspend fun updateStatus(productId: String, status: Boolean)
}
