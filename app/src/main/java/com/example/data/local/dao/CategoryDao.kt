package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories_cache WHERE storeId = :storeId ORDER BY sortOrder ASC, name ASC")
    fun getCategoriesByStoreFlow(storeId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories_cache WHERE storeId = :storeId ORDER BY sortOrder ASC, name ASC")
    suspend fun getCategoriesByStore(storeId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories_cache WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryById(categoryId: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories_cache WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: String)

    @Query("UPDATE categories_cache SET status = :status WHERE id = :categoryId")
    suspend fun updateCategoryStatus(categoryId: String, status: Boolean)

    @Query("DELETE FROM categories_cache")
    suspend fun clearAllCategories()
}
