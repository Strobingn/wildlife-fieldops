package com.strobingn.wildlifefieldops.ml.voice

import com.strobingn.wildlifefieldops.ml.domain.VoiceJobParser
import com.strobingn.wildlifefieldops.ml.model.DraftHints
import com.strobingn.wildlifefieldops.ml.model.FieldProvenance
import com.strobingn.wildlifefieldops.ml.model.MlThresholds
import com.strobingn.wildlifefieldops.ml.model.ScoredLabel
import com.strobingn.wildlifefieldops.ml.model.VoiceParseResult
import com.strobingn.wildlifefieldops.ml.vision.ServiceTypeRules
import com.strobingn.wildlifefieldops.ml.vision.TaxonomyMapper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline voice/NLU: keyword and phrase rules over the raw transcript.
 * Always available; never throws for empty input (returns empty structured result).
 */
@Singleton
class RegexVoiceJobParser @Inject constructor(
    private val mapper: TaxonomyMapper
) : VoiceJobParser {

    override suspend fun parse(transcript: String, hints: DraftHints): VoiceParseResult {
        val text = transcript.trim()
        if (text.isBlank()) {
            return VoiceParseResult(
                transcript = "",
                customerName = hints.knownCustomerName,
                address = hints.knownAddress,
                source = SOURCE
            )
        }

        val lower = text.lowercase()
        val species = extractSpecies(lower)
        val damage = extractDamage(lower)
        val urgency = extractUrgency(lower)
        var severity = mapper.estimateSeverityScore(damage.map { it.labelId })
        if (urgency.isNotEmpty()) {
            severity = maxOf(severity, 3)
        }
        if (Regex("""\b(fire|sparks?|collapse|rabies|bite|bitten)\b""").containsMatchIn(lower)) {
            severity = maxOf(severity, 4)
        }

        val address = extractAddress(text).ifBlank { hints.knownAddress }
        val customer = extractCustomerName(text).ifBlank { hints.knownCustomerName }
        val entry = extractEntryPoints(lower, text)
        val findings = text.take(2000)
        val service = ServiceTypeRules.serviceTypeFor(
            species.map { it.labelId },
            damage.map { it.labelId }
        )
        val priority = ServiceTypeRules.priorityFor(
            species.map { it.labelId },
            damage.map { it.labelId },
            severity
        )

        return VoiceParseResult(
            transcript = text,
            speciesLabelIds = species,
            damageLabelIds = damage,
            severity = severity,
            severityConfidence = if (severity > 0) 0.7f else 0.4f,
            customerName = customer,
            address = address,
            findings = findings,
            recommendations = if (urgency.isNotEmpty()) {
                "Prioritize safety; confirm urgency keywords: ${urgency.joinToString()}."
            } else {
                ""
            },
            entryPoints = entry,
            notes = text,
            serviceType = service,
            priority = priority,
            urgencyKeywords = urgency,
            source = SOURCE
        )
    }

    private fun extractSpecies(lower: String): List<ScoredLabel> {
        val found = linkedMapOf<String, Float>()
        val patterns = listOf(
            "raccoon" to "raccoon",
            "squirrel" to "squirrel",
            "bat" to "bat",
            "bats" to "bat",
            "opossum" to "opossum",
            "possum" to "opossum",
            "skunk" to "skunk",
            "snake" to "snake",
            "rat" to "rodent",
            "rats" to "rodent",
            "mouse" to "rodent",
            "mice" to "rodent",
            "rodent" to "rodent",
            "bird" to "bird",
            "pigeon" to "bird",
            "crow" to "bird",
            "coyote" to "coyote"
        )
        for ((needle, id) in patterns) {
            if (Regex("""\b${Regex.escape(needle)}\b""").containsMatchIn(lower)) {
                found[id] = maxOf(found[id] ?: 0f, 0.85f)
            }
        }
        return found.map { (id, conf) ->
            val m = mapper.mapSpecies(id, conf)
            ScoredLabel(
                labelId = m.labelId,
                displayLabel = m.displayLabel,
                confidence = conf,
                provenance = FieldProvenance.VOICE_NLU,
                accepted = conf >= MlThresholds.AUTO_ACCEPT
            )
        }
    }

    private fun extractDamage(lower: String): List<ScoredLabel> {
        val found = linkedMapOf<String, Float>()
        fun hit(id: String, conf: Float = 0.8f) {
            found[id] = maxOf(found[id] ?: 0f, conf)
        }
        if (Regex("""\b(wire|wiring|electrical|cable)s?\b""").containsMatchIn(lower) &&
            Regex("""\b(chew|chewed|gnaw|gnawed|damage|damaged)\b""").containsMatchIn(lower)
        ) {
            hit("wire_damage", 0.9f)
        } else if (Regex("""\b(wire|wiring)\b""").containsMatchIn(lower)) {
            hit("wire_damage", 0.75f)
        }
        if (Regex("""\b(hole|holes|gap|opening|vent|entry\s*point)\b""").containsMatchIn(lower)) {
            hit("entry_hole", 0.85f)
        }
        if (Regex("""\b(chew|chewed|gnaw|gnawed)\b""").containsMatchIn(lower)) {
            hit("chew_marks", 0.85f)
        }
        if (Regex("""\b(droppings?|scat|feces|guano|poop)\b""").containsMatchIn(lower)) {
            hit("droppings", 0.85f)
        }
        if (Regex("""\b(nest|nesting|bedding)\b""").containsMatchIn(lower)) {
            hit("nest", 0.8f)
        }
        if (Regex("""\b(scratch|scratches|scratching)\b""").containsMatchIn(lower)) {
            hit("scratch_marks", 0.8f)
        }
        if (Regex("""\binsulation\b""").containsMatchIn(lower)) {
            hit("insulation_damage", 0.8f)
        }
        if (Regex("""\b(urine|ammonia)\b""").containsMatchIn(lower)) {
            hit("urine_stain", 0.8f)
        }
        if (Regex("""\b(grease|rub\s*marks?)\b""").containsMatchIn(lower)) {
            hit("grease_rub", 0.75f)
        }
        if (Regex("""\blatrine\b""").containsMatchIn(lower)) {
            hit("latrine", 0.85f)
        }
        if (Regex("""\b(odor|smell|stink)\b""").containsMatchIn(lower)) {
            hit("odor_only", 0.7f)
        }
        if (Regex("""\b(structural|collapse|collapsed|broken\s+beam)\b""").containsMatchIn(lower)) {
            hit("structural_damage", 0.9f)
        }

        return found.map { (id, conf) ->
            val m = mapper.mapDamage(id, conf)
            ScoredLabel(
                labelId = m.labelId,
                displayLabel = m.displayLabel,
                confidence = conf,
                provenance = FieldProvenance.VOICE_NLU,
                accepted = conf >= MlThresholds.AUTO_ACCEPT
            )
        }
    }

    private fun extractUrgency(lower: String): List<String> {
        val keys = listOf(
            "emergency", "urgent", "asap", "right now", "immediately",
            "fire", "sparks", "collapse", "rabies", "bite", "bitten"
        )
        return keys.filter { lower.contains(it) }
    }

    private fun extractAddress(text: String): String {
        // Simple US-style street number + street name
        val street = Regex(
            """\b(\d{1,6}\s+[A-Za-z0-9.'\-]+(?:\s+[A-Za-z0-9.'\-]+){0,4}\s+(?:St|Street|Rd|Road|Ave|Avenue|Dr|Drive|Ln|Lane|Blvd|Boulevard|Ct|Court|Way|Hwy|Highway)\.?)\b""",
            RegexOption.IGNORE_CASE
        ).find(text)?.groupValues?.getOrNull(1)
        return street?.trim().orEmpty()
    }

    private fun extractCustomerName(text: String): String {
        val m = Regex(
            """\b(?:customer|client|homeowner|owner)\s+(?:is\s+|named\s+)?([A-Z][a-z]+(?:\s+[A-Z][a-z]+)?)\b"""
        ).find(text)
        return m?.groupValues?.getOrNull(1)?.trim().orEmpty()
    }

    private fun extractEntryPoints(lower: String, original: String): String {
        if (!Regex("""\b(vent|soffit|fascia|chimney|gable|ridge|crawl|attic|roof)\b""").containsMatchIn(lower)) {
            return ""
        }
        val hits = listOf("vent", "soffit", "fascia", "chimney", "gable", "ridge", "crawlspace", "crawl space", "attic", "roof")
            .filter { lower.contains(it) }
        return hits.joinToString(", ").ifBlank {
            // keep a short excerpt if keywords only matched partially
            original.lines().firstOrNull { line ->
                line.lowercase().let { l -> hits.any { l.contains(it) } }
            }.orEmpty()
        }
    }

    companion object {
        const val SOURCE = "regex"
    }
}
