package com.example.data.local

import com.example.data.local.dao.AuditLogDao
import com.example.data.local.entity.AuditLogEntity
import com.example.model.AuditActionType
import com.example.model.AuditLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineAuditRepository(
    private val auditLogDao: AuditLogDao
) {
    fun getAllAuditLogs(): Flow<List<AuditLog>> {
        return auditLogDao.getAllAuditLogs().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun logAction(
        actionType: AuditActionType,
        description: String,
        details: String? = null,
        operatorUsername: String = "admin",
        deviceModel: String = "Android Device",
        androidVersion: String = "Android 14",
        storeName: String = "OpenCart Store",
        apiProfileUsed: String = "OpenCart Admin API (Direct Session)"
    ) {
        val now = System.currentTimeMillis()
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.ITALIAN)
        val formattedTimestamp = sdf.format(java.util.Date(now))

        val entity = AuditLogEntity(
            id = "audit_${now}_${(100..999).random()}",
            timestamp = formattedTimestamp,
            timestampMillis = now,
            operatorUsername = operatorUsername,
            actionType = actionType.name,
            description = description,
            details = details,
            deviceModel = deviceModel,
            androidVersion = androidVersion,
            appVersion = "1.0.0",
            storeName = storeName,
            apiProfileUsed = apiProfileUsed
        )
        auditLogDao.insertAuditLog(entity)
    }

    suspend fun clearAuditLogs() {
        auditLogDao.clearAllAuditLogs()
    }

    private fun AuditLogEntity.toDomainModel(): AuditLog {
        val type = try {
            AuditActionType.valueOf(actionType)
        } catch (_: Exception) {
            AuditActionType.ORDER_STATUS_UPDATE
        }
        return AuditLog(
            id = id,
            timestamp = timestamp,
            timestampMillis = timestampMillis,
            operatorUsername = operatorUsername,
            actionType = type,
            description = description,
            details = details,
            deviceModel = deviceModel,
            androidVersion = androidVersion,
            appVersion = appVersion,
            storeName = storeName,
            apiProfileUsed = apiProfileUsed
        )
    }
}
