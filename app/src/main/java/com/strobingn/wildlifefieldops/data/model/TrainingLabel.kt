package com.strobingn.wildlifefieldops.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.strobingn.wildlifefieldops.ml.model.LabelSource
import com.strobingn.wildlifefieldops.ml.model.PredictionTarget
import java.util.UUID

/**
 * Ground-truth label after technician review — training flywheel row.
 */
@Entity(
    tableName = "training_labels",
    indices = [
        Index("photoId"),
        Index("labelId"),
        Index("createdAt")
    ]
)
data class TrainingLabel(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val photoId: String,
    val visionPredictionId: String? = null,
    val target: PredictionTarget,
    val labelId: String,
    val source: LabelSource,
    val modelLabelId: String? = null,
    val modelConfidence: Float? = null,
    val boxLeft: Float? = null,
    val boxTop: Float? = null,
    val boxRight: Float? = null,
    val boxBottom: Float? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val exportedAt: Long? = null
)
