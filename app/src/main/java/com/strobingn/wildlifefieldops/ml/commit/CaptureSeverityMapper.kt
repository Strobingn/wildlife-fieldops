package com.strobingn.wildlifefieldops.ml.commit

import com.strobingn.wildlifefieldops.data.model.FindingSeverity
import com.strobingn.wildlifefieldops.data.model.JobPriority

/** Maps fusion severity score (0–4) and priority strings to domain enums. */
object CaptureSeverityMapper {

    fun toFindingSeverity(score: Int): FindingSeverity = when (score.coerceIn(0, 4)) {
        0 -> FindingSeverity.NONE
        1 -> FindingSeverity.LOW
        2 -> FindingSeverity.MODERATE
        3 -> FindingSeverity.HIGH
        else -> FindingSeverity.CRITICAL
    }

    fun toJobPriority(priority: String, severityScore: Int = 0): JobPriority {
        val normalized = priority.trim().uppercase()
        val fromString = when (normalized) {
            "LOW" -> JobPriority.LOW
            "MEDIUM" -> JobPriority.MEDIUM
            "HIGH" -> JobPriority.HIGH
            "URGENT" -> JobPriority.URGENT
            else -> null
        }
        if (fromString != null) return fromString
        return when {
            severityScore >= 4 -> JobPriority.URGENT
            severityScore >= 3 -> JobPriority.HIGH
            severityScore >= 1 -> JobPriority.MEDIUM
            else -> JobPriority.MEDIUM
        }
    }
}
