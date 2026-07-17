package com.strobingn.wildlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val jobId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val technicianName: String = "",
    val visitDate: Long = System.currentTimeMillis(),
    val startTime: Long? = null,
    val endTime: Long? = null,
    val checkInLatitude: Double? = null,
    val checkInLongitude: Double? = null,
    val checkOutLatitude: Double? = null,
    val checkOutLongitude: Double? = null,
    val workPerformed: String = "",
    val materialsUsed: String = "",
    val notes: String = "",
    val photos: List<String> = emptyList(),
    val signaturePath: String = "",
    val customerSignaturePath: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) {
    val durationMinutes: Long?
        get() = if (startTime != null && endTime != null) {
            (endTime - startTime) / 60000
        } else null
}
