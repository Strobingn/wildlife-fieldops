package com.strobingn.wildlifefieldops.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["status", "nextAttemptAt"]),
        Index(value = ["entityType", "entityId", "operation"], unique = true)
    ]
)
data class SyncQueueItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val entityType: SyncEntityType,
    val entityId: String,
    val operation: SyncOperation = SyncOperation.UPSERT,
    val status: SyncQueueStatus = SyncQueueStatus.PENDING,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val nextAttemptAt: Long = 0L
)

enum class SyncEntityType {
    JOB,
    CUSTOMER,
    INSPECTION,
    INVOICE
}

enum class SyncOperation {
    UPSERT,
    DELETE
}

enum class SyncQueueStatus {
    PENDING,
    IN_PROGRESS,
    FAILED,
    COMPLETED
}
