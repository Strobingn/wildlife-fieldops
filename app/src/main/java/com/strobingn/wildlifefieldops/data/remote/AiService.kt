package com.strobingn.wildlifefieldops.data.remote

import com.strobingn.wildlifefieldops.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class LlmMessage(
    val role: String,
    val content: String
)

@Serializable
private data class LlmRequest(
    val model: String,
    val messages: List<LlmMessage>,
    val max_tokens: Int = 600,
    val temperature: Double = 0.4
)

@Serializable
private data class AiEdgeRequest(
    val mode: String,
    val observation: String,
    val species: String = ""
)

@Singleton
class AiService @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val isConfigured: Boolean
        get() = BuildConfig.LLM_API_KEY.isNotBlank()

    /**
     * Calls the configured LLM API directly (xAI, OpenAI, or compatible).
     * Falls back to a clear message if no API key is configured.
     */
    suspend fun ask(userMessage: String, species: String = ""): String = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext "⚠️ AI not connected.\n\nAdd your LLM_API_KEY to BuildConfig (via env var or local.properties) and rebuild.\n\nSupports: xAI (Grok), OpenAI (GPT), or any OpenAI-compatible API.\n\nSet LLM_API_KEY and optionally LLM_BASE_URL env vars before building."
        }

        val systemPrompt = """You are a professional wildlife removal field assistant with 20+ years of hands-on experience. You help technicians in the field with:
- Species identification from behavioral clues, droppings, damage patterns, and sounds
- Safety protocols for rabies-vector species (raccoons, bats, skunks, foxes)
- Equipment and trapping strategies for specific situations
- Pricing/estimate guidance for nuisance wildlife jobs
- Exclusion techniques and repair recommendations
- Legal compliance (state/federal wildlife regulations)

Keep responses concise, actionable, and field-ready. Use bullet points. If the user mentions a species, tailor advice to that species. Be direct and practical — technicians are reading this on job sites."""

        val userPrompt = buildString {
            if (species.isNotBlank()) append("Species: $species\n")
            append(userMessage)
        }

        val baseUrl = BuildConfig.LLM_BASE_URL.trimEnd('/')
        val endpoint = URL("$baseUrl/chat/completions")
        val payload = json.encodeToString(
            LlmRequest(
                model = detectModel(),
                messages = listOf(
                    LlmMessage(role = "system", content = systemPrompt),
                    LlmMessage(role = "user", content = userPrompt)
                )
            )
        )

        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${BuildConfig.LLM_API_KEY}")
        }

        try {
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }
                .orEmpty()

            if (code !in 200..299) {
                android.util.Log.w("AiService", "LLM HTTP $code: ${body.take(400)}")
                return@withContext "API error (HTTP $code). Check your LLM_API_KEY is valid and LLM_BASE_URL is correct."
            }

            parseLlmResponse(body) ?: "No response from AI. Try again."
        } catch (e: Exception) {
            android.util.Log.e("AiService", "LLM request failed", e)
            "Network error: ${e.message}. Check your internet connection."
        } finally {
            connection.disconnect()
        }
    }

    private fun detectModel(): String {
        val base = BuildConfig.LLM_BASE_URL
        return when {
            base.contains("x.ai") || base.contains("grok") -> "grok-2-latest"
            base.contains("openai") -> "gpt-4o-mini"
            base.contains("anthropic") || base.contains("claude") -> "claude-3-haiku-20240307"
            else -> "grok-2-latest"
        }
    }

    private fun parseLlmResponse(body: String): String? {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            root["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("message")
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.content
                ?.trim()
        } catch (e: Exception) {
            android.util.Log.w("AiService", "Parse LLM response failed", e)
            null
        }
    }

    /**
     * Legacy Supabase edge function path — kept for backward compatibility.
     * Only used if SUPABASE_URL is configured AND LLM_API_KEY is not.
     */
    suspend fun askViaSupabase(userMessage: String, species: String = ""): String = withContext(Dispatchers.IO) {
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        if (base.contains("your-project") || BuildConfig.SUPABASE_ANON_KEY == "your-anon-key") {
            return@withContext "⚠️ No AI backend configured. Add LLM_API_KEY to enable AI assistance."
        }
        val endpoint = URL("$base/functions/v1/ai-assistant")
        val payload = json.encodeToString(
            AiEdgeRequest(
                mode = "field_plan",
                observation = userMessage,
                species = species
            )
        )
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
        }
        try {
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }
                .orEmpty()
            if (code !in 200..299) {
                android.util.Log.w("AiService", "Supabase edge HTTP $code: ${body.take(300)}")
                return@withContext "Supabase edge function error (HTTP $code). Check your edge function is deployed."
            }
            formatEdgeResponse(body) ?: "Empty response from edge function."
        } catch (e: Exception) {
            android.util.Log.e("AiService", "Supabase request failed", e)
            "Network error: ${e.message}"
        } finally {
            connection.disconnect()
        }
    }

    private fun formatEdgeResponse(body: String): String? {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            val data = root["data"]?.jsonObject ?: root["result"]?.jsonObject ?: root
            val summary = data["summary"]?.jsonPrimitive?.content
            if (summary.isNullOrBlank()) return null
            val steps = data["recommended_next_steps"]?.jsonArray
                ?.mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull() }
                .orEmpty()
            val safety = data["safety_flags"]?.jsonArray
                ?.mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull() }
                .orEmpty()
            buildString {
                append(summary.trim())
                if (steps.isNotEmpty()) {
                    append("\n\nNext steps:\n")
                    steps.forEachIndexed { i, s -> append("${i + 1}. $s\n") }
                }
                if (safety.isNotEmpty()) {
                    append("\nSafety:\n")
                    safety.forEach { append("• $it\n") }
                }
            }.trim()
        } catch (e: Exception) {
            android.util.Log.w("AiService", "Parse edge response failed", e)
            null
        }
    }
}
