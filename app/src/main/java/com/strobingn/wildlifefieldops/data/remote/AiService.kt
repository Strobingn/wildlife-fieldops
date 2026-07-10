package com.strobingn.wildlifefieldops.data.remote

import com.strobingn.wildlifefieldops.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val isConfigured: Boolean
        get() {
            val url = BuildConfig.SUPABASE_URL
            val key = BuildConfig.SUPABASE_ANON_KEY
            return url.isNotBlank() &&
                !url.contains("your-project") &&
                key.isNotBlank() &&
                key != "your-anon-key"
        }

    /**
     * Calls Supabase Edge Function `ai-assistant` (uses server-side LLM secrets when set).
     * Falls back to on-device field knowledge if the function is unavailable.
     */
    suspend fun ask(userMessage: String, species: String = ""): String = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext localFieldKnowledge(userMessage)
        }
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
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
                android.util.Log.w("AiService", "AI edge HTTP $code: ${body.take(300)}")
                return@withContext localFieldKnowledge(userMessage)
            }
            formatEdgeResponse(body) ?: localFieldKnowledge(userMessage)
        } catch (e: Exception) {
            android.util.Log.e("AiService", "AI request failed", e)
            localFieldKnowledge(userMessage)
        } finally {
            connection.disconnect()
        }
    }

    private fun formatEdgeResponse(body: String): String? {
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            // Function may return { data: {...} } or the object directly
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
            android.util.Log.w("AiService", "Parse AI JSON failed", e)
            null
        }
    }

    fun localFieldKnowledge(userMessage: String): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("species") || lower.contains("identif") ||
                lower.contains("raccoon") || lower.contains("squirrel") || lower.contains("skunk") ||
                lower.contains("bat") || lower.contains("bird") ->
                "Identification tips:\n\n" +
                    "• Raccoons: black mask, ringed tail, nocturnal, 10–30 lbs.\n" +
                    "• Gray squirrels: bushy tail, chewed entry points, dawn/dusk active.\n" +
                    "• Skunks: white stripes/spots — can spray 10–15 ft.\n" +
                    "• Bats: often attics; many protections — verify season rules before exclusion.\n" +
                    "• Birds: Migratory Bird Treaty Act may apply — check before nest removal."

            lower.contains("safety") || lower.contains("protocol") || lower.contains("ppe") ->
                "Safety protocols:\n\n" +
                    "1. PPE: heavy gloves, eye protection, long sleeves, respirator in attics.\n" +
                    "2. Rabies-vector species (raccoon, bat, skunk, fox): minimize contact.\n" +
                    "3. Ladder work: use a spotter.\n" +
                    "4. Secure transfer cages with solid dividers.\n" +
                    "5. Photo entry points before and after work.\n" +
                    "6. Keep rabies pre-exposure vaccination current."

            lower.contains("equipment") || lower.contains("trap") || lower.contains("tool") ->
                "Core equipment:\n\n" +
                    "• Live traps sized for target species + bait.\n" +
                    "• Exclusion: hardware cloth, chimney caps, vent covers, sealant.\n" +
                    "• Inspection: borescope, headlamp, moisture meter.\n" +
                    "• Docs: camera, tape measure, GPS."

            lower.contains("price") || lower.contains("estimate") || lower.contains("cost") ->
                "Typical pricing guidance (verify locally):\n\n" +
                    "• Inspection: \$150–\$300\n" +
                    "• Squirrel: \$300–\$600\n" +
                    "• Raccoon: \$400–\$800\n" +
                    "• Bat exclusion: \$500–\$2,000+\n" +
                    "Factors: access, entry points, repairs, warranty length."

            else ->
                "Field guidance:\n\n" +
                    "1. Document evidence and entry points with photos.\n" +
                    "2. Prefer humane live trapping when allowed.\n" +
                    "3. Seal all access after removal (full exclusion).\n" +
                    "4. Remove attractants (food, shelter).\n" +
                    "5. Confirm local/state wildlife rules before relocation.\n\n" +
                    "Ask about a species, safety, equipment, or estimates for more detail."
        }
    }
}
