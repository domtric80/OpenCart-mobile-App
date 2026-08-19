package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey
    val id: String,
    val timestamp: String,
    val timestampMillis: Long,
    val operatorUsername: String,
    val actionType: String,
    val description: String,
    val details: String?,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
    val storeName: String,
    val apiProfileUsed: String
)
