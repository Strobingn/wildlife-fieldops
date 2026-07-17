package com.strobingn.wildlifefieldops.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "repairs")
data class Repair(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val jobId: String = "",
    val description: String = "",
    val repairType: String = "",
    val location: String = "",
    val materialsUsed: String = "",
    val laborHours: Double = 0.0,
    val cost: Double = 0.0,
    val beforePhotoPath: String = "",
    val afterPhotoPath: String = "",
    val completedDate: Long? = null,
    val completedBy: String = "",
    val warrantyMonths: Int = 0,
    val warrantyExpires: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
