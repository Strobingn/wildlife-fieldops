package com.strobingn.wildlifefieldops.ml.model

import kotlinx.serialization.Serializable

@Serializable
data class DraftHints(
    val knownCustomerName: String = "",
    val knownAddress: String = "",
    val jobId: String? = null
)

/**
 * Structured extract from a field voice transcript (regex offline and/or Grok JSON).
 * All taxonomy fields use stable label IDs from LABEL-TAXONOMY.
 */
@Serializable
data class VoiceParseResult(
    val transcript: String = "",
    val speciesLabelIds: List<ScoredLabel> = emptyList(),
    val damageLabelIds: List<ScoredLabel> = emptyList(),
    val severity: Int = 0,
    val severityConfidence: Float = 0f,
    val customerName: String = "",
    val address: String = "",
    val findings: String = "",
    val recommendations: String = "",
    val entryPoints: String = "",
    val notes: String = "",
    val serviceType: String = "",
    val priority: String = "",
    val urgencyKeywords: List<String> = emptyList(),
    val source: String = "regex",
    val errorMessage: String = ""
) {
    val ok: Boolean get() = errorMessage.isBlank()
}

/** Serializable DTO matching Grok voice JSON (strict schema). */
@Serializable
data class GrokVoiceJsonDto(
    val speciesLabelIds: List<String> = emptyList(),
    val damageLabelIds: List<String> = emptyList(),
    val severity: Int = 0,
    val customerName: String = "",
    val address: String = "",
    val findings: String = "",
    val recommendations: String = "",
    val entryPoints: String = "",
    val notes: String = "",
    val serviceType: String = "",
    val priority: String = "MEDIUM"
)
