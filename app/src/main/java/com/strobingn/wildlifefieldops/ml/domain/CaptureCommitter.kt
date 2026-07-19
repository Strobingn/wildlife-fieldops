package com.strobingn.wildlifefieldops.ml.domain

import com.strobingn.wildlifefieldops.data.model.VisionPrediction
import com.strobingn.wildlifefieldops.ml.commit.TrainingLabelDraft
import com.strobingn.wildlifefieldops.ml.model.MultimodalDraftSnapshot

/**
 * Atomically persists a reviewed multimodal capture into Job + Inspection + related ML rows.
 */
interface CaptureCommitter {
    suspend fun commit(request: CaptureCommitRequest): CaptureCommitResult
}

data class CaptureCommitRequest(
    val sessionId: String,
    val reviewedDraft: MultimodalDraftSnapshot,
    val labels: List<TrainingLabelDraft> = emptyList(),
    /** Vision rows to insert (skip ones already in DB if same id). */
    val predictions: List<VisionPrediction> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val technicianName: String = "",
    /** When true, derive training labels from accepted draft chips if [labels] is empty. */
    val autoLabelsFromDraft: Boolean = true
)

sealed class CaptureCommitResult {
    data class Success(
        val jobId: String,
        val inspectionId: String,
        val sessionId: String,
        val trainingLabelCount: Int
    ) : CaptureCommitResult()

    data class Failure(
        val message: String,
        val sessionId: String? = null
    ) : CaptureCommitResult()
}
