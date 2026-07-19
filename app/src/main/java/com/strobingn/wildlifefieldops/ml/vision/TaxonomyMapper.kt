package com.strobingn.wildlifefieldops.ml.vision

import com.strobingn.wildlifefieldops.ml.model.MlThresholds
import com.strobingn.wildlifefieldops.ml.model.PredictionTarget

/**
 * Maps free-form ML Kit / object-detector strings into stable taxonomy IDs.
 * Pure Kotlin — no Android APIs (unit-testable).
 */
class TaxonomyMapper(
    private val catalog: TaxonomyCatalog = TaxonomyCatalog.embedded()
) {
    data class RawLabel(
        val text: String,
        val confidence: Float
    )

    data class MappedLabel(
        val labelId: String,
        val displayLabel: String,
        val confidence: Float,
        val target: PredictionTarget,
        val rawText: String
    )

    fun catalog(): TaxonomyCatalog = catalog

    fun mapSpecies(raw: String, confidence: Float = 1f): MappedLabel {
        val text = raw.trim().lowercase()
        val id = matchSpeciesId(text)
        return MappedLabel(
            labelId = id,
            displayLabel = catalog.speciesDisplay(id),
            confidence = confidence.coerceIn(0f, 1f),
            target = PredictionTarget.SPECIES,
            rawText = raw
        )
    }

    fun mapDamage(raw: String, confidence: Float = 1f): MappedLabel {
        val text = raw.trim().lowercase()
        val id = matchDamageId(text)
        return MappedLabel(
            labelId = id,
            displayLabel = catalog.damageDisplay(id),
            confidence = confidence.coerceIn(0f, 1f),
            target = PredictionTarget.DAMAGE,
            rawText = raw
        )
    }

    /**
     * Classifies a single ML Kit label into species, damage, or neither.
     * Prefers species when both could match (e.g. unlikely dual matches).
     */
    fun mapAny(raw: String, confidence: Float): MappedLabel? {
        if (confidence < MlThresholds.HARD_REJECT) return null
        val text = raw.trim().lowercase()
        if (text.isBlank()) return null

        val speciesId = matchSpeciesId(text)
        if (speciesId != "unknown" && speciesId != "none") {
            return MappedLabel(
                labelId = speciesId,
                displayLabel = catalog.speciesDisplay(speciesId),
                confidence = confidence.coerceIn(0f, 1f),
                target = PredictionTarget.SPECIES,
                rawText = raw
            )
        }

        val damageId = matchDamageId(text)
        if (damageId != "unknown" && damageId != "none") {
            return MappedLabel(
                labelId = damageId,
                displayLabel = catalog.damageDisplay(damageId),
                confidence = confidence.coerceIn(0f, 1f),
                target = PredictionTarget.DAMAGE,
                rawText = raw
            )
        }

        // Keep low-value labels out of prediction tables unless above suggestion threshold.
        if (confidence >= MlThresholds.SHOW_SUGGESTION) {
            return MappedLabel(
                labelId = "unknown",
                displayLabel = catalog.speciesDisplay("unknown"),
                confidence = confidence.coerceIn(0f, 1f),
                target = PredictionTarget.SPECIES,
                rawText = raw
            )
        }
        return null
    }

    fun mapAll(rawLabels: List<RawLabel>): List<MappedLabel> {
        val out = linkedMapOf<String, MappedLabel>()
        for (raw in rawLabels) {
            val mapped = mapAny(raw.text, raw.confidence) ?: continue
            // Key by target+id so species raccoon and damage chew stay separate.
            val key = "${mapped.target.name}:${mapped.labelId}"
            val existing = out[key]
            if (existing == null || mapped.confidence > existing.confidence) {
                out[key] = mapped
            }
        }
        return out.values.sortedByDescending { it.confidence }
    }

    fun primarySpeciesId(mapped: List<MappedLabel>): String {
        return mapped
            .filter { it.target == PredictionTarget.SPECIES && it.labelId != "unknown" && it.labelId != "none" }
            .maxByOrNull { it.confidence }
            ?.labelId
            .orEmpty()
    }

    fun primaryDamageId(mapped: List<MappedLabel>): String {
        return mapped
            .filter { it.target == PredictionTarget.DAMAGE && it.labelId != "unknown" && it.labelId != "none" }
            .maxByOrNull { it.confidence }
            ?.labelId
            .orEmpty()
    }

    fun estimateSeverityScore(damageIds: List<String>): Int {
        if (damageIds.isEmpty() || damageIds.all { it == "none" }) return 0
        var score = 1
        if (damageIds.any { it in setOf("entry_hole", "chew_marks", "scratch_marks", "grease_rub") }) {
            score = maxOf(score, 2)
        }
        if (damageIds.any { it in setOf("nest", "droppings", "latrine", "insulation_damage", "urine_stain") }) {
            score = maxOf(score, 2)
        }
        if (damageIds.any { it in setOf("wire_damage", "structural_damage") }) {
            score = maxOf(score, 3)
        }
        if (damageIds.size >= 3) score = maxOf(score, 3)
        return score.coerceIn(0, 4)
    }

    private fun matchSpeciesId(text: String): String {
        return when {
            text.contains("raccoon") -> "raccoon"
            text.contains("squirrel") -> "squirrel"
            text.contains("bat") -> "bat"
            text.contains("pigeon") || text.contains("crow") || text.contains("sparrow") ||
                text.contains("bird") || text.contains("avian") -> "bird"
            text.contains("rat") || text.contains("mouse") || text.contains("rodent") -> "rodent"
            text.contains("opossum") || text.contains("possum") -> "opossum"
            text.contains("skunk") -> "skunk"
            text.contains("snake") || text.contains("serpent") -> "snake"
            // Policy v1: map common mammals that are not primary attic pests to unknown
            // (not coyote) to avoid over-confident wrong species.
            text.contains("coyote") -> "coyote"
            text.contains("fox") || text.contains("dog") || text.contains("cat") ||
                text.contains("canine") || text.contains("feline") -> "unknown"
            text.contains("insect") || text.contains("bee") || text.contains("wasp") ||
                text.contains("hornet") || text.contains("ant") -> "insect_other"
            text.contains("no animal") || text == "none" -> "none"
            else -> "unknown"
        }
    }

    private fun matchDamageId(text: String): String {
        return when {
            text.contains("latrine") -> "latrine"
            text.contains("wire") || text.contains("cable") || text.contains("electrical") -> "wire_damage"
            text.contains("insulation") -> "insulation_damage"
            text.contains("urine") || (text.contains("stain") && !text.contains("grease")) -> "urine_stain"
            text.contains("grease") || text.contains("rub mark") -> "grease_rub"
            text.contains("dropping") || text.contains("scat") || text.contains("feces") ||
                text.contains("guano") || text.contains("poop") -> "droppings"
            text.contains("nest") || text.contains("bedding") || text.contains("nesting") -> "nest"
            text.contains("scratch") -> "scratch_marks"
            text.contains("chew") || text.contains("gnaw") -> "chew_marks"
            text.contains("hole") || text.contains("gap") || text.contains("opening") ||
                text.contains("vent") || text.contains("entry") -> "entry_hole"
            text.contains("structural") || text.contains("collapse") || text.contains("broken") ->
                "structural_damage"
            text.contains("odor") || text.contains("smell") || text.contains("ammonia") -> "odor_only"
            text.contains("no damage") || text == "none" -> "none"
            text.contains("damage") -> "unknown"
            else -> "unknown"
        }
    }

    companion object {
        fun default(): TaxonomyMapper = TaxonomyMapper(TaxonomyCatalog.embedded())
    }
}
