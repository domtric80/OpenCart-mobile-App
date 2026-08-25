package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_profiles")
data class StoreProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val apiKey: String = "",
    val adminUsername: String = "",
    val isPrimary: Boolean = false,
    val isActive: Boolean = true,
    val pushWebhookEnabled: Boolean = false,
    val fcmDeviceToken: String = "",
    val connectionType: String = "BRIDGE_PLUGIN",
    val lastSyncTimestamp: Long = 0L,
    val openCartVersion: String = "3.x/4.x",
    val sslPinned: Boolean = true
)
