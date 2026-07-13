package com.strobingn.wildlifefieldops.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-device AI service using ML Kit GenAI / Gemini Nano when available.
 * Falls back to rules-based or existing cloud AiService.
 * Focused on wildlife ops: species ID, estimate generation, compliance check, form structuring from voice.
 */
object OnDeviceAIService {

    suspend fun generateEstimateFromPhotoAndNotes(
        context: Context,
        species: String?,
        damageNotes: String,
        photosCount: Int
    ): String = withContext(Dispatchers.IO) {
        // TODO: Replace with real ML Kit GenAI Prompt API call
        // Example structured prompt to Gemini Nano:
        // "You are a wildlife removal expert. Based on species=$species and notes=$damageNotes, generate a tiered Good/Better/Best estimate with 3-5 line items and realistic pricing for New York area. Return as clean JSON."
        """
        {
          "tiers": [
            {"name": "Good", "total": 450, "items": ["Trap & remove animal", "Seal entry point"]},
            {"name": "Better", "total": 750, "items": ["Full exclusion + one-way door", "Warranty 1 year"]},
            {"name": "Best", "total": 1250, "items": ["Complete exclusion system", "Follow-up inspection", "Lifetime warranty"]}
          ]
        }
        """
    }

    suspend fun analyzeVoiceToStructuredJob(
        transcribedText: String
    ): Map<String, String> = withContext(Dispatchers.IO) {
        // TODO: Gemini Nano structured output
        mapOf(
            "species" to if (transcribedText.contains("raccoon", true)) "Raccoon" else "Unknown",
            "action" to if (transcribedText.contains("trap", true)) "Trapped and removed" else "Inspected",
            "priority" to if (transcribedText.contains("urgent") || transcribedText.contains("baby")) "HIGH" else "NORMAL"
        )
    }

    // Add more: complianceCheck(formText), suggestRoutePriority(trapHistory), etc.
}