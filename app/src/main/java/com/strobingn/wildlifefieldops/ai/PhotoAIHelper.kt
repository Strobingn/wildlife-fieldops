package com.strobingn.wildlifefieldops.ai

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.tasks.await

// Heavy AI result for form filling, analysis, measurements
 data class AiAnalysisResult(
    val species: List<String> = emptyList(),
    val damageTypes: List<String> = emptyList(),
    val confidence: Float = 0f,
    val suggestedServiceType: String = "",
    val suggestedPriority: String = "MEDIUM",
    val suggestedNotes: String = "",
    val estimatedPriceRange: String = "",
    val objectDetections: List<String> = emptyList() // for measurement hints
)

object PhotoAIHelper {
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    suspend fun analyzePhotoForFormFilling(context: Context, imageUri: Uri): AiAnalysisResult {
        return try {
            val image = InputImage.fromFilePath(context, imageUri)
            
            // Species + Damage labeling
            val labels = labeler.process(image).await()
            val wildlifeLabels = labels.filter { it.confidence > 0.55f }
                .map { it.text.lowercase() }
                .filter { it in listOf("raccoon", "bat", "squirrel", "opossum", "snake", "bird", "rodent", "damage", "hole", "entry point", "chew marks", "nesting", "droppings", "scratching") }

            val species = wildlifeLabels.filter { it in listOf("raccoon", "bat", "squirrel", "opossum", "snake", "bird", "rodent") }
            val damage = wildlifeLabels.filter { it in listOf("damage", "hole", "entry point", "chew marks", "nesting", "droppings", "scratching") }

            // Object detection for measurement hints (size estimation)
            val objects = objectDetector.process(image).await()
            val objectNames = objects.mapNotNull { obj ->
                obj.labels.firstOrNull()?.text?.lowercase()
            }.distinct()

            // Smart form filling logic (heavy AI)
            val suggestedService = when {
                species.any { it.contains("bat") } -> "Bat Exclusion & Removal"
                species.any { it.contains("raccoon") } -> "Raccoon Removal & Exclusion"
                species.any { it.contains("squirrel") } -> "Squirrel Removal & Exclusion"
                damage.any { it.contains("entry") || it.contains("hole") } -> "Entry Point Sealing & Repair"
                else -> "Wildlife Inspection & Removal"
            }

            val priority = if (species.isNotEmpty() || damage.isNotEmpty()) "HIGH" else "MEDIUM"

            val notes = buildString {
                if (species.isNotEmpty()) append("Species observed: ${species.joinToString()}. ")
                if (damage.isNotEmpty()) append("Damage noted: ${damage.joinToString()}. ")
                append("AI confidence: ${(labels.maxOfOrNull { it.confidence } ?: 0f) * 100}%. ")
                append("Recommend on-site inspection + photos of all entry points.")
            }

            val priceRange = when {
                suggestedService.contains("Bat") -> "$450 - $1,200 (exclusion + one-way doors)"
                suggestedService.contains("Raccoon") -> "$350 - $950 (trapping + exclusion)"
                suggestedService.contains("Squirrel") -> "$275 - $750 (exclusion + repairs)"
                else -> "$200 - $600 (inspection + remediation)"
            }

            AiAnalysisResult(
                species = species,
                damageTypes = damage,
                confidence = labels.maxOfOrNull { it.confidence } ?: 0f,
                suggestedServiceType = suggestedService,
                suggestedPriority = priority,
                suggestedNotes = notes,
                estimatedPriceRange = priceRange,
                objectDetections = objectNames
            )
        } catch (e: Exception) {
            AiAnalysisResult(suggestedNotes = "AI analysis failed: ${e.message}. Manual entry required.")
        }
    }
}