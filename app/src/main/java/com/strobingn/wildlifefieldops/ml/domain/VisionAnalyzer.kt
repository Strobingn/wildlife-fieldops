package com.strobingn.wildlifefieldops.ml.domain

import android.content.Context
import android.net.Uri
import com.strobingn.wildlifefieldops.data.model.VisionPrediction
import com.strobingn.wildlifefieldops.ml.vision.TaxonomyMapper

/**
 * On-device (or hybrid) vision analysis that emits taxonomy-backed [VisionPrediction] rows.
 */
interface VisionAnalyzer {
    /**
     * @param photoId stable id for Room linkage (caller creates Photo row separately)
     * @param captureSessionId optional Field Capture session
     * @param jobId optional job when analyzing in job context
     */
    suspend fun analyze(
        context: Context,
        photoUri: Uri,
        photoId: String,
        captureSessionId: String? = null,
        jobId: String? = null,
        inspectionId: String? = null
    ): VisionAnalysisResult
}

data class VisionAnalysisResult(
    val predictions: List<VisionPrediction>,
    val mappedLabels: List<TaxonomyMapper.MappedLabel>,
    val primarySpeciesLabelId: String = "",
    val primaryDamageLabelId: String = "",
    val severityScore: Int = 0,
    val serviceType: String = "",
    val priority: String = "MEDIUM",
    val suggestedNotes: String = "",
    val estimatedPriceLow: Double = 0.0,
    val estimatedPriceHigh: Double = 0.0,
    val estimatedPriceRange: String = "",
    val rawLabelTexts: List<String> = emptyList(),
    val objectDetectionTexts: List<String> = emptyList(),
    val maxConfidence: Float = 0f,
    val errorMessage: String = ""
) {
    val ok: Boolean get() = errorMessage.isBlank()
}
