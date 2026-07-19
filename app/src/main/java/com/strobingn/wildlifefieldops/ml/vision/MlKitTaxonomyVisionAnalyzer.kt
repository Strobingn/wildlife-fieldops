package com.strobingn.wildlifefieldops.ml.vision

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.strobingn.wildlifefieldops.data.model.VisionPrediction
import com.strobingn.wildlifefieldops.ml.domain.VisionAnalysisResult
import com.strobingn.wildlifefieldops.ml.domain.VisionAnalyzer
import com.strobingn.wildlifefieldops.ml.model.ModelBackend
import com.strobingn.wildlifefieldops.ml.model.PredictionTarget
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * P0 vision path: ML Kit image labeling + object detection → [TaxonomyMapper] → [VisionPrediction].
 */
@Singleton
class MlKitTaxonomyVisionAnalyzer @Inject constructor(
    private val mapper: TaxonomyMapper
) : VisionAnalyzer {

    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    override suspend fun analyze(
        context: Context,
        photoUri: Uri,
        photoId: String,
        captureSessionId: String? ,
        jobId: String?,
        inspectionId: String?
    ): VisionAnalysisResult {
        return try {
            val image = InputImage.fromFilePath(context, photoUri)
            val labels = awaitTask(labeler.process(image))
            val objects = awaitTask(objectDetector.process(image))

            val rawFromLabels = labels.map { TaxonomyMapper.RawLabel(it.text, it.confidence) }
            val objectTexts = objects.mapNotNull { detected ->
                detected.labels.maxByOrNull { it.confidence }?.let { best ->
                    TaxonomyMapper.RawLabel(best.text, best.confidence)
                }
            }
            val objectNames = objectTexts.map { it.text.lowercase() }.distinct()
            val rawAll = rawFromLabels + objectTexts
            val rawTexts = rawAll.map { it.text }.distinct()

            val mapped = mapper.mapAll(rawAll)
            val primarySpecies = mapper.primarySpeciesId(mapped)
            val primaryDamage = mapper.primaryDamageId(mapped)
            val damageIds = mapped
                .filter { it.target == PredictionTarget.DAMAGE }
                .map { it.labelId }
            val speciesIds = mapped
                .filter { it.target == PredictionTarget.SPECIES }
                .map { it.labelId }
            val severity = mapper.estimateSeverityScore(damageIds)
            val service = ServiceTypeRules.serviceTypeFor(speciesIds, damageIds)
            val priority = ServiceTypeRules.priorityFor(speciesIds, damageIds, severity)
            val maxConf = mapped.maxOfOrNull { it.confidence }
                ?: labels.maxOfOrNull { it.confidence }
                ?: 0f
            val notes = ServiceTypeRules.notesFor(
                speciesDisplay = mapped
                    .filter { it.target == PredictionTarget.SPECIES && it.labelId !in IGNORE_IDS }
                    .map { it.displayLabel },
                damageDisplay = mapped
                    .filter { it.target == PredictionTarget.DAMAGE && it.labelId !in IGNORE_IDS }
                    .map { it.displayLabel },
                confidencePercent = (maxConf * 100).toInt()
            )
            val band = OfflinePriceBands.forServiceType(service)
            val rawJson = JSONArray(rawTexts).toString()

            val predictions = mapped.map { m ->
                val backend = when (m.target) {
                    PredictionTarget.SPECIES, PredictionTarget.DAMAGE -> ModelBackend.ML_KIT_LABELING
                    else -> ModelBackend.ML_KIT_LABELING
                }
                VisionPrediction(
                    photoId = photoId,
                    jobId = jobId,
                    inspectionId = inspectionId,
                    captureSessionId = captureSessionId,
                    backend = backend,
                    modelVersion = MODEL_VERSION,
                    target = m.target,
                    labelId = m.labelId,
                    displayLabel = m.displayLabel,
                    confidence = m.confidence,
                    rawLabelsJson = rawJson
                )
            }

            // Always emit at least a session-level unknown species row when ML Kit returns nothing useful,
            // so callers can still attach photoId + raw labels for the training flywheel.
            val finalPredictions = predictions.ifEmpty {
                listOf(
                    VisionPrediction(
                        photoId = photoId,
                        jobId = jobId,
                        inspectionId = inspectionId,
                        captureSessionId = captureSessionId,
                        backend = ModelBackend.ML_KIT_LABELING,
                        modelVersion = MODEL_VERSION,
                        target = PredictionTarget.SPECIES,
                        labelId = "unknown",
                        displayLabel = mapper.catalog().speciesDisplay("unknown"),
                        confidence = maxConf,
                        rawLabelsJson = rawJson
                    )
                )
            }

            VisionAnalysisResult(
                predictions = finalPredictions,
                mappedLabels = mapped,
                primarySpeciesLabelId = primarySpecies,
                primaryDamageLabelId = primaryDamage,
                severityScore = severity,
                serviceType = service,
                priority = priority,
                suggestedNotes = notes,
                estimatedPriceLow = band.low,
                estimatedPriceHigh = band.high,
                estimatedPriceRange = band.rangeLabel,
                rawLabelTexts = rawTexts,
                objectDetectionTexts = objectNames,
                maxConfidence = maxConf
            )
        } catch (e: Exception) {
            VisionAnalysisResult(
                predictions = emptyList(),
                mappedLabels = emptyList(),
                suggestedNotes = "Photo analysis failed: ${e.message}. Manual entry required.",
                errorMessage = e.message ?: e.javaClass.simpleName
            )
        }
    }

    companion object {
        const val MODEL_VERSION = "mlkit-taxonomy-v1"
        private val IGNORE_IDS = setOf("unknown", "none", "")

        /** Non-Hilt construction for [com.strobingn.wildlifefieldops.ai.PhotoAIHelper] and tests. */
        fun createDefault(mapper: TaxonomyMapper = TaxonomyMapper.default()): MlKitTaxonomyVisionAnalyzer =
            MlKitTaxonomyVisionAnalyzer(mapper)
    }
}

private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { cont ->
    task.addOnSuccessListener { result -> cont.resume(result) }
    task.addOnFailureListener { exception -> cont.resumeWithException(exception) }
}
