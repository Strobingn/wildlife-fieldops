package com.strobingn.wildlifefieldops.ml.commit

import com.strobingn.wildlifefieldops.data.model.TrainingLabel
import com.strobingn.wildlifefieldops.ml.model.LabelSource
import com.strobingn.wildlifefieldops.ml.model.PredictionTarget
import com.strobingn.wildlifefieldops.ml.model.ScoredLabel
import java.util.UUID

/**
 * Review-time ground-truth label before persist.
 * Built by Field Capture UI from accepted/corrected chips.
 */
data class TrainingLabelDraft(
    val photoId: String,
    val target: PredictionTarget,
    val labelId: String,
    val source: LabelSource,
    val visionPredictionId: String? = null,
    val modelLabelId: String? = null,
    val modelConfidence: Float? = null,
    val boxLeft: Float? = null,
    val boxTop: Float? = null,
    val boxRight: Float? = null,
    val boxBottom: Float? = null,
    val notes: String = "",
    val createdBy: String = ""
) {
    fun toEntity(now: Long = System.currentTimeMillis()): TrainingLabel = TrainingLabel(
        id = UUID.randomUUID().toString(),
        photoId = photoId,
        visionPredictionId = visionPredictionId,
        target = target,
        labelId = labelId,
        source = source,
        modelLabelId = modelLabelId,
        modelConfidence = modelConfidence,
        boxLeft = boxLeft,
        boxTop = boxTop,
        boxRight = boxRight,
        boxBottom = boxBottom,
        notes = notes,
        createdAt = now,
        createdBy = createdBy,
        exportedAt = null
    )

    companion object {
        /**
         * Derive training rows from draft chips when UI passes accepted/corrected scored labels.
         * [photoId] should be primary photo for session-level labels; multi-photo UIs pass explicit drafts.
         */
        fun fromScoredLabels(
            photoId: String,
            species: List<ScoredLabel>,
            damage: List<ScoredLabel>,
            createdBy: String = ""
        ): List<TrainingLabelDraft> {
            val out = mutableListOf<TrainingLabelDraft>()
            for (s in species.filter { it.labelId !in IGNORE }) {
                out += TrainingLabelDraft(
                    photoId = photoId,
                    target = PredictionTarget.SPECIES,
                    labelId = s.labelId,
                    source = if (s.provenance.name == "USER" && !s.accepted) {
                        LabelSource.USER_ONLY
                    } else if (s.accepted) {
                        LabelSource.MODEL_ACCEPTED
                    } else {
                        LabelSource.MODEL_CORRECTED
                    },
                    modelConfidence = s.confidence,
                    createdBy = createdBy
                )
            }
            for (d in damage.filter { it.labelId !in IGNORE }) {
                out += TrainingLabelDraft(
                    photoId = photoId,
                    target = PredictionTarget.DAMAGE,
                    labelId = d.labelId,
                    source = if (d.accepted) LabelSource.MODEL_ACCEPTED else LabelSource.MODEL_CORRECTED,
                    modelConfidence = d.confidence,
                    createdBy = createdBy
                )
            }
            return out
        }

        private val IGNORE = setOf("unknown", "none", "")
    }
}
