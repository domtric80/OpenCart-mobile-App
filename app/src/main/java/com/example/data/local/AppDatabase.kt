package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.OrderDao
import com.example.data.local.dao.OrderReturnDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.StoreProfileDao
import com.example.data.local.dao.SubscriptionDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.OrderItemEntity
import com.example.data.local.entity.OrderReturnEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StoreProfileEntity
import com.example.data.local.entity.SubscriptionEntity

/**
 * Main Room database instance for local caching of OpenCart stores, orders, subscriptions, returns, items, products, categories, and audit logs.
 */
@Database(
    entities = [
        StoreProfileEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        ProductEntity::class,
        CategoryEntity::class,
        AuditLogEntity::class,
        SubscriptionEntity::class,
        OrderReturnEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun storeProfileDao(): StoreProfileDao
    abstract fun orderDao(): OrderDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun orderReturnDao(): OrderReturnDao
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "opencart_admin_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
