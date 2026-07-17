package com.strobingn.wildlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "trap_logs")
data class TrapLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val jobId: String = "",
    val trapId: String = "",
    val trapLocation: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val technicianName: String = "",
    val checkDate: Long = System.currentTimeMillis(),
    val status: TrapStatus = TrapStatus.SET,
    val catchType: CatchType = CatchType.NONE,
    val catchCount: Int = 0,
    val baitType: String = "",
    val baitCondition: String = "",
    val conditionNotes: String = "",
    val actionTaken: String = "",
    val photoPath: String = "",
    val nextCheckDate: Long? = null,
    val weatherConditions: String = "",
    val temperature: Float? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
