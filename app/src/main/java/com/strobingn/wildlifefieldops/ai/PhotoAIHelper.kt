package com.strobingn.wildlifefieldops.ai

import android.content.Context
import android.net.Uri
import com.strobingn.wildlifefieldops.ml.domain.VisionAnalysisResult
import com.strobingn.wildlifefieldops.ml.vision.MlKitTaxonomyVisionAnalyzer
import com.strobingn.wildlifefieldops.ml.vision.TaxonomyCatalog
import com.strobingn.wildlifefieldops.ml.vision.TaxonomyMapper
import java.util.UUID

/**
 * Backward-compatible entry for existing screens (Species ID, HybridAI).
 * All vision work delegates to [MlKitTaxonomyVisionAnalyzer] — no parallel heuristics.
 */
data class AiAnalysisResult(
    val species: List<String> = emptyList(),
    val damageTypes: List<String> = emptyList(),
    val confidence: Float = 0f,
    val suggestedServiceType: String = "",
    val suggestedPriority: String = "MEDIUM",
    val suggestedNotes: String = "",
    val estimatedPriceRange: String = "",
    val estimatedPriceLow: Double = 0.0,
    val estimatedPriceHigh: Double = 0.0,
    val objectDetections: List<String> = emptyList(),
    val source: String = "offline_ml",
    /** Taxonomy ids (stable); prefer these over display strings when wiring new UI. */
    val speciesLabelIds: List<String> = emptyList(),
    val damageLabelIds: List<String> = emptyList(),
    val severityScore: Int = 0,
    val visionAnalysis: VisionAnalysisResult? = null
) {
    val serviceType: String get() = suggestedServiceType
    val priority: String get() = suggestedPriority
    val notes: String get() = suggestedNotes
    val fromGrok: Boolean get() = source == "grok"
}

object PhotoAIHelper {

    @Volatile
    private var analyzer: MlKitTaxonomyVisionAnalyzer? = null

    private fun analyzer(context: Context): MlKitTaxonomyVisionAnalyzer {
        analyzer?.let { return it }
        return synchronized(this) {
            analyzer ?: MlKitTaxonomyVisionAnalyzer.createDefault(
                TaxonomyMapper(TaxonomyCatalog.load(context))
            ).also { analyzer = it }
        }
    }

    /**
     * Analyze a photo and return form-oriented suggestions.
     * Internally runs taxonomy ML Kit analysis and maps to legacy field names.
     */
    suspend fun analyzePhotoForFormFilling(context: Context, imageUri: Uri): AiAnalysisResult {
        val appContext = context.applicationContext
        val result = analyzer(appContext).analyze(
            context = appContext,
            photoUri = imageUri,
            photoId = UUID.randomUUID().toString()
        )
        return result.toAiAnalysisResult()
    }

    fun toAiAnalysisResult(result: VisionAnalysisResult): AiAnalysisResult =
        result.toAiAnalysisResult()
}

private fun VisionAnalysisResult.toAiAnalysisResult(): AiAnalysisResult {
    if (!ok && predictions.isEmpty()) {
        return AiAnalysisResult(
            suggestedNotes = suggestedNotes.ifBlank {
                "Photo analysis failed: $errorMessage. Manual entry required."
            },
            source = "offline_ml",
            visionAnalysis = this
        )
    }
    val speciesIds = mappedLabels
        .filter { it.target == com.strobingn.wildlifefieldops.ml.model.PredictionTarget.SPECIES }
        .map { it.labelId }
        .filter { it !in setOf("unknown", "none") }
        .distinct()
    val damageIds = mappedLabels
        .filter { it.target == com.strobingn.wildlifefieldops.ml.model.PredictionTarget.DAMAGE }
        .map { it.labelId }
        .filter { it !in setOf("unknown", "none") }
        .distinct()
    val speciesDisplay = mappedLabels
        .filter { it.target == com.strobingn.wildlifefieldops.ml.model.PredictionTarget.SPECIES }
        .filter { it.labelId !in setOf("unknown", "none") }
        .map { it.displayLabel }
        .distinct()
    val damageDisplay = mappedLabels
        .filter { it.target == com.strobingn.wildlifefieldops.ml.model.PredictionTarget.DAMAGE }
        .filter { it.labelId !in setOf("unknown", "none") }
        .map { it.displayLabel }
        .distinct()

    return AiAnalysisResult(
        species = speciesDisplay.ifEmpty { speciesIds },
        damageTypes = damageDisplay.ifEmpty { damageIds },
        confidence = maxConfidence,
        suggestedServiceType = serviceType,
        suggestedPriority = priority,
        suggestedNotes = suggestedNotes,
        estimatedPriceRange = estimatedPriceRange,
        estimatedPriceLow = estimatedPriceLow,
        estimatedPriceHigh = estimatedPriceHigh,
        objectDetections = objectDetectionTexts,
        source = "offline_ml",
        speciesLabelIds = speciesIds,
        damageLabelIds = damageIds,
        severityScore = severityScore,
        visionAnalysis = this
    )
}
