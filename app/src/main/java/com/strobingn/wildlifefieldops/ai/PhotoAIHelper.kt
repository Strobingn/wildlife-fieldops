package com.strobingn.wildlifefieldops.ai

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await

/**
 * On-device photo AI for Wildlife Field Ops.
 * Uses ML Kit Image Labeling (no cloud, works offline after first download).
 * Tags common wildlife species and damage types from job/inspection photos.
 */
object PhotoAIHelper {

    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    // Wildlife + damage keywords we care about (expand as needed)
    private val wildlifeKeywords = setOf(
        "raccoon", "bat", "squirrel", "opossum", "snake", "bird", "skunk", "fox",
        "damage", "hole", "entry", "chew", "nest", "droppings", "scratch", "gnaw"
    )

    /**
     * Analyze a photo URI and return list of relevant tags with confidence.
     * Call from a coroutine (e.g. viewModelScope or LaunchedEffect).
     * Safe: returns empty list on any error (no crash in field).
     */
    suspend fun analyzePhoto(context: Context, imageUri: Uri): List<Pair<String, Float>> {
        return try {
            val image = InputImage.fromFilePath(context, imageUri)
            val labels = labeler.process(image).await()

            labels
                .filter { it.confidence >= 0.55f }
                .map { it.text.lowercase() to it.confidence }
                .filter { (text, _) ->
                    wildlifeKeywords.any { keyword -> text.contains(keyword) }
                }
                .sortedByDescending { it.second }
                .take(5) // top 5 most relevant
        } catch (e: Exception) {
            // Model download pending, permission issue, or bad image - fail gracefully
            emptyList()
        }
    }

    /**
     * Convenience: just the tag names (for quick display or auto-suggest)
     */
    suspend fun getTags(context: Context, imageUri: Uri): List<String> {
        return analyzePhoto(context, imageUri).map { it.first }
    }
}