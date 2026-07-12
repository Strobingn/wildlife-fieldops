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
        label.trim().replace(Regex("\\s+"), " ")

    fun display(label: String): String =
        normalize(label).ifBlank { "Inspection" }
}

/**
 * Legacy enum kept for Room converters / older rows.
 * New code should prefer free-form service type strings via [DefaultServiceTypes].
 */
enum class JobType {
    INSPECTION, REMOVAL, REPAIR, PREVENTION, CLEANUP, CONSULTATION, EMERGENCY,
    EXCLUSION, TRAPPING, ONE_WAY_DOOR, ATTIC_CLEANOUT, CRAWLSPACE_CLEANUP,
    CHIMNEY_CAP, DEAD_ANIMAL_REMOVAL, BAT_EXCLUSION, BIRD_CONTROL,
    SQUIRREL_REMOVAL, RACCOON_REMOVAL, SKUNK_REMOVAL, SNAKE_REMOVAL,
    INSULATION_REMEDIATION, SANITATION, FOLLOW_UP, OTHER;

    val label: String
        get() = when (this) {
            INSPECTION -> "Inspection"
            REMOVAL -> "Removal"
            REPAIR -> "Repair"
            PREVENTION -> "Prevention"
            CLEANUP -> "Cleanup"
            CONSULTATION -> "Consultation"
            EMERGENCY -> "Emergency"
            EXCLUSION -> "Exclusion"
            TRAPPING -> "Trapping"
            ONE_WAY_DOOR -> "One-Way Door"
            ATTIC_CLEANOUT -> "Attic Cleanout"
            CRAWLSPACE_CLEANUP -> "Crawlspace Cleanup"
            CHIMNEY_CAP -> "Chimney Cap"
            DEAD_ANIMAL_REMOVAL -> "Dead Animal Removal"
            BAT_EXCLUSION -> "Bat Exclusion"
            BIRD_CONTROL -> "Bird Control"
            SQUIRREL_REMOVAL -> "Squirrel Removal"
            RACCOON_REMOVAL -> "Raccoon Removal"
            SKUNK_REMOVAL -> "Skunk Removal"
            SNAKE_REMOVAL -> "Snake Removal"
            INSULATION_REMEDIATION -> "Insulation Remediation"
            SANITATION -> "Sanitation / Disinfection"
            FOLLOW_UP -> "Follow-Up Visit"
            OTHER -> "Other"
        }

    companion object {
        fun fromLabel(label: String): JobType {
            val n = DefaultServiceTypes.normalize(label)
            return entries.firstOrNull { it.label.equals(n, ignoreCase = true) || it.name.equals(n, ignoreCase = true) }
                ?: OTHER
        }
    }
}
