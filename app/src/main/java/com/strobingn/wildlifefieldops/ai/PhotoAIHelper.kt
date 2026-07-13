package com.strobingn.wildlifefieldops.ai

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await

/**
 * On-device AI helper for wildlife species and damage identification.
 * Uses ML Kit Image Labeling + Gemini Nano via ML Kit GenAI Prompt API (when available).
 * Fully offline after model download.
 */
object PhotoAIHelper {

    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    /**
     * Analyze photo for species and damage.
     * Returns list of detected labels with confidence.
     */
    suspend fun analyzeSpeciesAndDamage(context: Context, imageUri: Uri): List<DetectedLabel> {
        return try {
            val image = InputImage.fromFilePath(context, imageUri)
            val labels = labeler.process(image).await()
            labels.filter { it.confidence >= 0.6f }
                .map { DetectedLabel(it.text, it.confidence) }
                .filter { it.label.lowercase() in setOf("raccoon", "bat", "squirrel", "opossum", "snake", "bird", "damage", "hole", "entry", "nest") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    data class DetectedLabel(val label: String, val confidence: Float)

    // TODO: Add Gemini Nano Prompt API call for richer multimodal analysis + structured JSON output
    // Example prompt: "Identify wildlife species and damage type in this image. Return JSON: {species, damageType, confidence, suggestedService}"
}