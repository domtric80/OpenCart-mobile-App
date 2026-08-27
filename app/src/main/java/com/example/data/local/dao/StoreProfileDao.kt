package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.StoreProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreProfileDao {

    @Query("SELECT * FROM store_profiles ORDER BY isPrimary DESC, name ASC")
    fun getAllStoresFlow(): Flow<List<StoreProfileEntity>>

    @Query("SELECT * FROM store_profiles ORDER BY isPrimary DESC, name ASC")
    suspend fun getAllStores(): List<StoreProfileEntity>

    @Query("SELECT * FROM store_profiles WHERE id = :storeId LIMIT 1")
    suspend fun getStoreById(storeId: String): StoreProfileEntity?

    @Query("SELECT * FROM store_profiles WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryStore(): StoreProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(store: StoreProfileEntity)

    @Update
    suspend fun update(store: StoreProfileEntity)

    @Query(
        "UPDATE store_profiles SET apiKey = :protectedApiKey, " +
            "adminUsername = :protectedUsername WHERE id = :storeId"
    )
    suspend fun updateProtectedCredentials(
        storeId: String,
        protectedUsername: String,
        protectedApiKey: String
    )

    @Query("UPDATE store_profiles SET isPrimary = CASE WHEN id = :storeId THEN 1 ELSE 0 END")
    suspend fun setPrimaryStore(storeId: String)

    @Query("DELETE FROM store_profiles WHERE id = :storeId")
    suspend fun deleteStoreById(storeId: String)

    @Query("DELETE FROM store_profiles")
    suspend fun deleteAllStores()
}
