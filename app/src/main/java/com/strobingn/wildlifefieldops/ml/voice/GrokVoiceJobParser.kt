package com.strobingn.wildlifefieldops.ml.voice

import com.google.gson.Gson
import com.strobingn.wildlifefieldops.BuildConfig
import com.strobingn.wildlifefieldops.ai.GrokPrompts
import com.strobingn.wildlifefieldops.ml.domain.VoiceJobParser
import com.strobingn.wildlifefieldops.ml.model.DraftHints
import com.strobingn.wildlifefieldops.ml.model.FieldProvenance
import com.strobingn.wildlifefieldops.ml.model.GrokVoiceJsonDto
import com.strobingn.wildlifefieldops.ml.model.MlThresholds
import com.strobingn.wildlifefieldops.ml.model.ScoredLabel
import com.strobingn.wildlifefieldops.ml.model.VoiceParseResult
import com.strobingn.wildlifefieldops.ml.vision.TaxonomyMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional cloud voice parse. On missing key or any failure → [RegexVoiceJobParser].
 * Never blocks capture offline.
 */
@Singleton
class GrokVoiceJobParser @Inject constructor(
    private val mapper: TaxonomyMapper,
    private val regexFallback: RegexVoiceJobParser
) : VoiceJobParser {

    private val client = HttpClient()
    private val gson = Gson()

    private data class ChatEnvelope(val choices: List<Choice> = emptyList())
    private data class Choice(val message: Message = Message())
    private data class Message(val content: String = "")

    override suspend fun parse(transcript: String, hints: DraftHints): VoiceParseResult {
        val offline = regexFallback.parse(transcript, hints)
        if (transcript.isBlank()) return offline
        if (!hasDirectKey()) return offline.copy(source = RegexVoiceJobParser.SOURCE)

        return runCatching {
            val redacted = redactSensitive(transcript)
            val raw = callGrokJson(GrokPrompts.voiceToMultimodalJson(redacted, hints.knownAddress))
            val dto = gson.fromJson(raw, GrokVoiceJsonDto::class.java)
                ?: return@runCatching offline.copy(
                    errorMessage = "Empty Grok voice JSON",
                    source = RegexVoiceJobParser.SOURCE
                )

            val species = dto.speciesLabelIds.mapNotNull { id ->
                val mapped = mapper.mapSpecies(id, 0.88f)
                if (mapped.labelId == "unknown" && id.isNotBlank()) {
                    // Accept only known taxonomy ids from model
                    null
                } else if (mapped.labelId in setOf("unknown", "none") && id.isBlank()) {
                    null
                } else {
                    ScoredLabel(
                        labelId = mapped.labelId,
                        displayLabel = mapped.displayLabel,
                        confidence = 0.88f,
                        provenance = FieldProvenance.LLM,
                        accepted = 0.88f >= MlThresholds.AUTO_ACCEPT
                    )
                }
            }.ifEmpty { offline.speciesLabelIds }

            val damage = dto.damageLabelIds.mapNotNull { id ->
                val mapped = mapper.mapDamage(id, 0.88f)
                if (mapped.labelId in setOf("unknown", "none") && id.isBlank()) null
                else ScoredLabel(
                    labelId = mapped.labelId,
                    displayLabel = mapped.displayLabel,
                    confidence = 0.88f,
                    provenance = FieldProvenance.LLM,
                    accepted = 0.88f >= MlThresholds.AUTO_ACCEPT
                )
            }.ifEmpty { offline.damageLabelIds }

            VoiceParseResult(
                transcript = transcript,
                speciesLabelIds = species,
                damageLabelIds = damage,
                severity = dto.severity.coerceIn(0, 4).takeIf { it > 0 } ?: offline.severity,
                severityConfidence = 0.8f,
                customerName = dto.customerName.ifBlank { offline.customerName },
                address = dto.address.ifBlank { offline.address },
                findings = dto.findings.ifBlank { offline.findings },
                recommendations = dto.recommendations.ifBlank { offline.recommendations },
                entryPoints = dto.entryPoints.ifBlank { offline.entryPoints },
                notes = dto.notes.ifBlank { offline.notes },
                serviceType = dto.serviceType.ifBlank { offline.serviceType },
                priority = dto.priority.ifBlank { offline.priority },
                urgencyKeywords = offline.urgencyKeywords,
                source = SOURCE
            )
        }.getOrElse { e ->
            offline.copy(
                source = RegexVoiceJobParser.SOURCE,
                errorMessage = "Grok voice parse fell back to regex: ${e.message ?: e.javaClass.simpleName}"
            )
        }
    }

    private fun hasDirectKey(): Boolean = BuildConfig.LLM_API_KEY.trim().length >= 10

    /** Strip phone-like tokens before cloud send (design privacy rule). */
    internal fun redactSensitive(text: String): String {
        return text
            .replace(Regex("""\b\d{3}[-.\s]?\d{3}[-.\s]?\d{4}\b"""), "[phone]")
            .replace(Regex("""\b\d{3}-\d{2}-\d{4}\b"""), "[id]")
    }

    private suspend fun callGrokJson(prompt: String): String = withContext(Dispatchers.IO) {
        val body = mapOf(
            "model" to BuildConfig.LLM_MODEL,
            "messages" to listOf(
                mapOf("role" to "system", "content" to GrokPrompts.SYSTEM),
                mapOf("role" to "user", "content" to prompt)
            ),
            "temperature" to 0.2,
            "max_tokens" to 900,
            "response_format" to mapOf("type" to "json_object")
        )
        val response: String = client.post("${BuildConfig.LLM_BASE_URL.trimEnd('/')}/chat/completions") {
            contentType(ContentType.Application.Json)
            headers { append("Authorization", "Bearer ${BuildConfig.LLM_API_KEY.trim()}") }
            setBody(gson.toJson(body))
        }.body()
        val envelope = gson.fromJson(response, ChatEnvelope::class.java)
        val content = envelope.choices.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
            ?: error("Empty Grok response")
        content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    }

    companion object {
        const val SOURCE = "grok"
    }
}
