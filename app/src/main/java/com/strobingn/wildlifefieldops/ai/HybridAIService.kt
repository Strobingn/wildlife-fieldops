package com.strobingn.wildlifefieldops.ai

import android.content.Context
import android.net.Uri
import com.strobingn.wildlifefieldops.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.google.gson.Gson

// Heavy hybrid AI: ML Kit instant tags + Grok for deep structured reasoning when online
// Uses your existing XAI_API_KEY from BuildConfig
object HybridAIService {

    private val client = HttpClient()
    private val gson = Gson()

    suspend fun analyzePhotoAndFillForm(context: Context, imageUri: Uri, jobContext: String = ""): AiAnalysisResult {
        // Step 1: Fast on-device ML Kit (always works offline)
        val mlkitResult = PhotoAIHelper.analyzePhotoForFormFilling(context, imageUri)

        // Step 2: If online and key exists, enhance with Grok for better reasoning
        if (BuildConfig.LLM_API_KEY.isNotBlank()) {
            return try {
                val prompt = GrokPrompts.photoToFormFill(
                    speciesTags = mlkitResult.species,
                    damageTags = mlkitResult.damageTypes,
                    location = jobContext
                )
                val grokResponse = callGrok(prompt)
                // Parse Grok JSON and merge with mlkitResult for best of both
                parseGrokFormResponse(grokResponse, mlkitResult)
            } catch (e: Exception) {
                mlkitResult // graceful fallback
            }
        }
        return mlkitResult
    }

    private suspend fun callGrok(prompt: String): String = withContext(Dispatchers.IO) {
        val body = mapOf(
            "model" to BuildConfig.LLM_MODEL,
            "messages" to listOf(
                mapOf("role" to "system", "content" to GrokPrompts.SYSTEM),
                mapOf("role" to "user", "content" to prompt)
            ),
            "response_format" to mapOf("type" to "json_object"),
            "temperature" to 0.3
        )
        val response: String = client.post("${BuildConfig.LLM_BASE_URL}/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(gson.toJson(body))
            headers.append("Authorization", "Bearer ${BuildConfig.LLM_API_KEY}")
        }.body()
        response
    }

    private fun parseGrokFormResponse(grokJson: String, fallback: AiAnalysisResult): AiAnalysisResult {
        // Simple parsing - in production use better JSON parser or Kotlinx
        return fallback // TODO: real parsing of Grok structured output into AiAnalysisResult
    }

    // Add similar hybrid methods for generateEstimate, complianceCheck, etc. using GrokPrompts
}
