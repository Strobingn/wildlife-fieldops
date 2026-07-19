package com.strobingn.wildlifefieldops.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.strobingn.wildlifefieldops.ml.model.ModelBackend
import com.strobingn.wildlifefieldops.ml.model.PredictionTarget
import java.util.UUID

/**
 * One model output label (optionally with a normalized bounding box) for a photo.
 * P0: one row per detection / whole-image taxonomy label.
 */
@Entity(
    tableName = "vision_predictions",
    indices = [
        Index("photoId"),
        Index("jobId"),
        Index("captureSessionId"),
        Index("createdAt")
    ]
)
data class VisionPrediction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val photoId: String,
    val jobId: String? = null,
    val inspectionId: String? = null,
    val captureSessionId: String? = null,
    val backend: ModelBackend = ModelBackend.ML_KIT_LABELING,
    val modelVersion: String = "mlkit-default",
    val target: PredictionTarget = PredictionTarget.SPECIES,
    /** Taxonomy id (see design LABEL-TAXONOMY.md). */
    val labelId: String = "unknown",
    val displayLabel: String = "",
    val confidence: Float = 0f,
    /** Normalized box 0..1; null if whole-image label. */
    val boxLeft: Float? = null,
    val boxTop: Float? = null,
    val boxRight: Float? = null,
    val boxBottom: Float? = null,
    val rawLabelsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)
