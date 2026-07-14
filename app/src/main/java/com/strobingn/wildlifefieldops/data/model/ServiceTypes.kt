package com.strobingn.wildlifefieldops.data.model

/**
 * Built-in wildlife field service types.
 * Users can add custom types in Settings; those are stored in DataStore.
 */
object DefaultServiceTypes {
    val all: List<String> = listOf(
        "Inspection",
        "Removal",
        "Repair",
        "Prevention",
        "Cleanup",
        "Consultation",
        "Emergency",
        "Exclusion",
        "Trapping",
        "One-Way Door",
        "Attic Cleanout",
        "Crawlspace Cleanup",
        "Chimney Cap",
        "Dead Animal Removal",
        "Bat Exclusion",
        "Bird Control",
        "Squirrel Removal",
        "Raccoon Removal",
        "Skunk Removal",
        "Snake Removal",
        "Insulation Remediation",
        "Sanitation / Disinfection",
        "Follow-Up Visit",
        "Other"
    )

    fun normalize(label: String): String =
        label.trim().replace(Regex("\s+"), " ")

    fun display(label: String): String =
        normalize(label).ifBlank { "Inspection" }
}
