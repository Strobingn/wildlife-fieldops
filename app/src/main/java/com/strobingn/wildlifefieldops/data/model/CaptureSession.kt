package com.strobingn.wildlifefieldops.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.strobingn.wildlifefieldops.ml.model.CaptureSessionStatus
import java.util.UUID

/**
 * Orchestrates multimodal field capture before commit to Job + Inspection.
 * [draftJson] holds [com.strobingn.wildlifefieldops.ml.model.MultimodalDraftSnapshot] JSON.
 */
@Entity(
    tableName = "capture_sessions",
    indices = [
        Index("status"),
        Index("createdAt")
    ]
)
data class CaptureSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val status: CaptureSessionStatus = CaptureSessionStatus.DRAFT,
    val voiceTranscript: String = "",
    val voiceAudioPath: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val addressGuess: String = "",
    val draftJson: String = "{}",
    val fusedJobId: String? = null,
    val fusedInspectionId: String? = null,
    val errorMessage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val committedAt: Long? = null
)
