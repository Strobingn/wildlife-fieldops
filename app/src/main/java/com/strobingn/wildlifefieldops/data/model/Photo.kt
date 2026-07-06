package com.strobingn.wildlifefieldops.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class PhotoCategory {
    INSPECTION, JOB_SITE, DAMAGE, REPAIR, WILDLIFE, EVIDENCE, BEFORE, AFTER, DOCUMENT, SIGNATURE
}

@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val filePath: String = "",
    val localPath: String = "",
    val remoteUrl: String = "",
    val thumbnailPath: String = "",
    val jobId: String? = null,
    val inspectionId: String? = null,
    val customerId: String? = null,
    val category: PhotoCategory = PhotoCategory.JOB_SITE,
    val description: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val takenAt: Long = System.currentTimeMillis(),
    val takenBy: String = "",
    val fileSize: Long = 0,
    val isUploaded: Boolean = false,
    val uploadError: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
