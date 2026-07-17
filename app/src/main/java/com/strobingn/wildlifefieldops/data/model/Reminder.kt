package com.strobingn.wildlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

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
