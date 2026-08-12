package com.strobingn.wildlifefieldops.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "voice_notes")
data class VoiceNote(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val jobId: String? = null,
    val inspectionId: String? = null,
    val title: String = "",
    val localPath: String = "",
    val remoteUrl: String = "",
    val durationMs: Long = 0,
    val transcript: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isUploaded: Boolean = false
)
