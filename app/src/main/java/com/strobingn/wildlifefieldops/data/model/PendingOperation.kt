package com.strobingn.wildlifefieldops.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pending_operations")
data class PendingOperation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val operationType: OperationType = OperationType.CREATE,
    val entityType: EntityType = EntityType.JOB,
    val entityId: String = "",
    val payload: String = "", // JSON serialized data
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String = "",
    val lastAttempt: Long? = null,
    val isProcessing: Boolean = false
) {
    fun canRetry(): Boolean = retryCount < 5
    fun nextRetryDelay(): Long = kotlin.math.min(60000L * (retryCount + 1), 600000L) // Exponential backoff, max 10 min
}

enum class OperationType {
    CREATE, UPDATE, DELETE, SYNC
}

enum class EntityType {
    JOB, CUSTOMER, INSPECTION, PHOTO, INVOICE, EXPENSE, INVENTORY, REPAIR, TRAP_LOG
}
