package com.strobingn.wildlifefieldops.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ReminderType {
    FOLLOW_UP, INSPECTION_DUE, PAYMENT_DUE, WARRANTY_EXPIRING, PERMIT_EXPIRING,
    SCHEDULE_MAINTENANCE, CUSTOMER_CALLBACK, EQUIPMENT_SERVICE, OTHER
}

enum class ReminderPriority {
    LOW, MEDIUM, HIGH, URGENT
}

enum class ReminderStatus {
    PENDING, COMPLETED, SNOOZED, DISMISSED, OVERDUE
}

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val jobId: String? = null,
    val customerId: String? = null,
    val customerName: String = "",
    val reminderType: ReminderType = ReminderType.OTHER,
    val priority: ReminderPriority = ReminderPriority.MEDIUM,
    val status: ReminderStatus = ReminderStatus.PENDING,
    val dueDate: Long = System.currentTimeMillis(),
    val completedDate: Long? = null,
    val completedBy: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
