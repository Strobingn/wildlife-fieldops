package com.strobingn.wildlifefieldops.ml.vision

import com.strobingn.wildlifefieldops.data.model.DefaultServiceTypes

/**
 * Deterministic service-type + priority suggestions from taxonomy label IDs.
 * Uses [DefaultServiceTypes] labels already in the app.
 */
object ServiceTypeRules {

    fun serviceTypeFor(
        speciesLabelIds: List<String>,
        damageLabelIds: List<String> = emptyList()
    ): String {
        val species = speciesLabelIds.map { it.lowercase() }.toSet()
        val damage = damageLabelIds.map { it.lowercase() }.toSet()

        return when {
            "bat" in species -> "Bat Exclusion"
            "raccoon" in species -> "Raccoon Removal"
            "squirrel" in species -> "Squirrel Removal"
            "skunk" in species -> "Skunk Removal"
            "snake" in species -> "Snake Removal"
            "bird" in species -> "Bird Control"
            "rodent" in species -> "Removal"
            "opossum" in species -> "Removal"
            "coyote" in species -> "Removal"
            "wire_damage" in damage -> "Repair"
            "entry_hole" in damage || "structural_damage" in damage -> "Exclusion"
            "insulation_damage" in damage -> "Insulation Remediation"
            "droppings" in damage || "latrine" in damage || "urine_stain" in damage ->
                "Sanitation / Disinfection"
            species.any { it !in setOf("unknown", "none", "") } -> "Removal"
            damage.any { it !in setOf("unknown", "none", "") } -> "Inspection"
            else -> "Inspection"
        }.let { DefaultServiceTypes.display(it) }
    }

    fun priorityFor(
        speciesLabelIds: List<String>,
        damageLabelIds: List<String>,
        severityScore: Int
    ): String {
        if (severityScore >= 4) return "URGENT"
        if (severityScore >= 3) return "HIGH"
        if ("wire_damage" in damageLabelIds || "structural_damage" in damageLabelIds) return "HIGH"
        if ("bat" in speciesLabelIds) return "HIGH"
        if (speciesLabelIds.any { it !in setOf("unknown", "none", "") } ||
            damageLabelIds.any { it !in setOf("unknown", "none", "") }
        ) {
            return "HIGH"
        }
        return "MEDIUM"
    }

    fun notesFor(
        speciesDisplay: List<String>,
        damageDisplay: List<String>,
        confidencePercent: Int
    ): String = buildString {
        if (speciesDisplay.isNotEmpty()) {
            append("Species observed: ${speciesDisplay.joinToString()}. ")
        }
        if (damageDisplay.isNotEmpty()) {
            append("Damage noted: ${damageDisplay.joinToString()}. ")
        }
        append("On-device confidence: $confidencePercent%. ")
        append("Verify on site and photograph all entry points.")
    }
}
