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

// AiEdgeRequest is defined in RemoteDtos.kt (same package) — do not redeclare

@Singleton
class AiService @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val isConfigured: Boolean
        get() = BuildConfig.LLM_API_KEY.isNotBlank()

    val providerLabel: String
        get() {
            val base = BuildConfig.LLM_BASE_URL.lowercase()
            return when {
                base.contains("x.ai") -> "SpaceXAI (Grok)"
                base.contains("openai") -> "OpenAI"
                else -> "LLM"
            }
        }

    /**
     * SpaceXAI (xAI Grok) by default — OpenAI-compatible chat completions.
     * Env: XAI_API_KEY (preferred) or LLM_API_KEY; base https://api.x.ai/v1; model grok-4.5.
     */
    suspend fun ask(userMessage: String, species: String = ""): String = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext buildString {
                append("⚠️ AI not connected.\n\n")
                append("This app defaults to SpaceXAI (xAI Grok).\n\n")
                append("1. Get a key: https://console.x.ai\n")
                append("2. Set GitHub secret XAI_API_KEY (or LLM_API_KEY)\n")
                append("3. Rebuild the APK\n\n")
                append("Defaults: base https://api.x.ai/v1 · model grok-4.5\n\n")
                append("Offline field tips still work — try a species name.")
            }
        }

        val systemPrompt = WILDLIFE_SYSTEM_PROMPT

        val userPrompt = buildString {
            if (species.isNotBlank()) append("Species context: $species\n")
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
                ),
                max_tokens = 900,
                temperature = 0.35
            )
        )

        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 90_000
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
                return@withContext when (code) {
                    429 -> "⚠️ AI rate limit (HTTP 429). Wait a minute and retry.\n\n" + localFieldKnowledge(userMessage)
                    401 -> "⚠️ Invalid API key (HTTP 401).\n\nCheck XAI_API_KEY / LLM_API_KEY secret and rebuild."
                    404 -> "⚠️ Endpoint not found (HTTP 404).\n\nExpected base: https://api.x.ai/v1 (chat/completions)."
                    else -> "⚠️ AI error (HTTP $code).\n\n" + localFieldKnowledge(userMessage)
                }
            }

            parseLlmResponse(body) ?: "No response from AI. Try again."
        } catch (e: Exception) {
            android.util.Log.e("AiService", "LLM request failed", e)
            "Network error: ${e.message}. Check internet, then retry.\n\n" + localFieldKnowledge(userMessage)
        } finally {
            connection.disconnect()
        }
    }

    private fun detectModel(): String {
        val configured = BuildConfig.LLM_MODEL.trim()
        if (configured.isNotBlank()) return configured
        val base = BuildConfig.LLM_BASE_URL.lowercase()
        return when {
            base.contains("x.ai") || base.contains("grok") -> "grok-4.5"
            base.contains("openai") -> "gpt-4o-mini"
            else -> "grok-4.5"
        }
    }

    companion object {
        /**
         * Full-stack wildlife ops copilot — field + office, not just species tips.
         */
        val WILDLIFE_SYSTEM_PROMPT: String = """
You are FieldOps AI for a professional wildlife removal / nuisance wildlife control business
(e.g. inspections, trapping, exclusion, cleanup, repairs, follow-ups, invoicing).

You help with EVERY operational area a tech or owner needs:
1) Field work — species ID (signs, sounds, damage, droppings), trapping/bait, exclusion, one-way doors, cleanup, PPE
2) Safety & compliance — rabies vectors, histoplasmosis, MBTA, state rules (call out when laws vary by state)
3) Job management — how to structure jobs, statuses, follow-ups, multi-visit plans, scheduling routes
4) Inspections — what to document, photos, entry points, severity, recommendations
5) Estimates & pricing — ballpark ranges, line items (labor, materials, exclusion, sanitation), upsells
6) Customer communication — clear, professional scripts without scare tactics
7) Equipment & inventory — traps, sealants, PPE, when to restock
8) Business ops — daily workflow, SOPs, quality checks before leaving a site

Style:
- Concise, bullet-first, field-readable on a phone
- Actionable next steps (1–5 bullets)
- Flag safety risks clearly
- If info is missing, ask 1–2 clarifying questions max
- Prefer practical US nuisance wildlife practice; note regional variation when relevant
- Never invent licenses or claim illegal methods; prefer legal exclusion/live-trap approaches

You are powered by SpaceXAI (xAI Grok) when the API key is configured.
""".trimIndent()
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

    /**
     * Built-in field knowledge — used as fallback when AI API is rate-limited or unavailable.
     * Provides practical wildlife removal guidance based on keywords in the user's message.
     */
    private fun localFieldKnowledge(userMessage: String): String {
        val msg = userMessage.lowercase()
        return when {
            msg.contains("raccoon") || msg.contains("coon") -> buildString {
                append("Raccoon Removal Tips:\n")
                append("• Use live traps (12x12x32) with sardines or marshmallows as bait\n")
                append("• Check attic entry points — raccoons tear fascia boards\n")
                append("• Rabies vector species — wear gloves, never handle bare-handed\n")
                append("• Typical job: $300-600 (trap + exclusion)\n")
                append("• Babies present Apr-Jun — delay eviction or use eviction fluid")
            }
            msg.contains("squirrel") || msg.contains("squirrels") -> buildString {
                append("Squirrel Removal Tips:\n")
                append("• Grey squirrels: 5x5x18 single-door live traps, peanut butter bait\n")
                append("• Flying squirrels: Multiple small traps, entry at dusk\n")
                append("• Check gable vents, chimney gaps, soffit edges\n")
                append("• One-way doors work well if no babies present\n")
                append("• Typical job: $250-450 (trap + seal entry)")
            }
            msg.contains("bat") || msg.contains("bats") -> buildString {
                append("Bat Removal Tips:\n")
                append("• Federally protected — NEVER kill, use exclusion only\n")
                append("• Install one-way bat valves at active entry points\n")
                append("• Active at dusk/dawn — observe flight paths\n")
                append("• Guano = histoplasmosis risk — wear respirator (N95 min)\n")
                append("• Typical job: $500-1500 (exclusion + cleanup)\n")
                append("• Exclusion window: Sept-May (avoid baby season Jun-Aug)")
            }
            msg.contains("skunk") || msg.contains("skunks") -> buildString {
                append("Skunk Removal Tips:\n")
                append("• Use covered live traps — draped tarp prevents spray\n")
                append("• Bait: sardines, cat food, or marshmallows\n")
                append("• Approach slowly, no sudden movements\n")
                append("• Rabies vector — wear full PPE, gloves required\n")
                append("• Typical job: $200-400 (trap + relocation)\n")
                append("• If sprayed: 1qt 3% H2O2 + 1/4c baking soda + 1tsp dish soap")
            }
            msg.contains("groundhog") || msg.contains("woodchuck") -> buildString {
                append("Groundhog Removal Tips:\n")
                append("• Large live trap (12x12x32), bait with fresh veggies/fruits\n")
                append("• Check for burrows under sheds, decks, porches\n")
                append("• Can excavate 50+ ft burrows — check foundation integrity\n")
                append("• Typical job: $250-500 (trap + burrow fill)")
            }
            msg.contains("snake") || msg.contains("snakes") -> buildString {
                append("Snake Handling Tips:\n")
                append("• NY native species are protected — check ID before removal\n")
                append("• Common nuisance: garter snakes, milk snakes, rat snakes\n")
                append("• Venomous species (timber rattler, copperhead) — call DEC\n")
                append("• Use snake tongs or pillowcase for transport\n")
                append("• Typical job: $150-300 (ID + relocation)")
            }
            msg.contains("bird") || msg.contains("birds") || msg.contains("pigeon") -> buildString {
                append("Bird Removal Tips:\n")
                append("• Federally protected (MBTA) — exclusion only, no kill\n")
                append("• Pigeons/starlings/sparrows: nets, spikes, exclusion\n")
                append("• Check vents, chimney caps, roof ledges\n")
                append("• Typical job: $300-800 (exclusion + cleanup)")
            }
            msg.contains("safety") || msg.contains("rabies") || msg.contains("ppe") -> buildString {
                append("Field Safety Protocols:\n")
                append("• Rabies vectors: raccoons, bats, skunks, foxes, coyotes\n")
                append("• Minimum PPE: leather gloves, long sleeves, safety glasses\n")
                append("• For bats: N95+ respirator (histoplasmosis)\n")
                append("• Bite protocol: wash 15 min, seek immediate medical care\n")
                append("• Vaccine: pre-exposure rabies vaccine recommended\n")
                append("• NEVER handle wildlife bare-handed — always use tools")
            }
            msg.contains("trap") || msg.contains("bait") || msg.contains("equipment") -> buildString {
                append("Trapping Equipment:\n")
                append("• Live traps: Havahart, Tomahawk, or Safeguard\n")
                append("• Small (5x5x18): squirrels, chipmunks\n")
                append("• Medium (10x12x30): raccoons, opossums, cats\n")
                append("• Large (15x15x42): groundhogs, foxes\n")
                append("• Baits: peanut butter (universal), sardines (raccoons/skunks),\n")
                append("  apples (deer), marshmallows (raccoons)\n")
                append("• Always check traps every 24hrs (NY law)")
            }
            msg.contains("estimate") || msg.contains("price") || msg.contains("cost") || msg.contains("charge") -> buildString {
                append("Typical Wildlife Removal Pricing (NY):\n")
                append("• Inspection only: $75-150\n")
                append("• Squirrel removal: $250-450\n")
                append("• Raccoon removal: $300-600\n")
                append("• Bat exclusion: $500-1500\n")
                append("• Skunk removal: $200-400\n")
                append("• Bird exclusion: $300-800\n")
                append("• Groundhog: $250-500\n")
                append("• Dead animal removal: $150-350\n")
                append("• Cleanup/sanitizing: $200-500 additional")
            }
            msg.contains("online") || msg.contains("there") || msg.contains("hello") || msg.contains("hi") -> {
                "I'm your Wildlife FieldOps assistant. Ask me about:\n" +
                "• Species ID and removal techniques\n" +
                "• Safety protocols and PPE\n" +
                "• Equipment and trapping strategies\n" +
                "• Pricing and estimates\n" +
                "• Exclusion and repair methods"
            }
            else -> buildString {
                append("I'm your Wildlife FieldOps assistant. I can help with:\n")
                append("• Species-specific removal techniques\n")
                append("• Safety protocols for rabies-vector species\n")
                append("• Trap selection and bait recommendations\n")
                append("• Pricing guidance for estimates\n")
                append("• Exclusion methods and entry point sealing\n\n")
                append("Try asking about a specific species (raccoon, squirrel, bat, skunk) ")
                append("or a topic like safety, equipment, or pricing.")
            }
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
