package com.strobingn.wildlifefieldops.ml.model

import kotlinx.serialization.Serializable

/**
 * JSON-serializable draft produced by multimodal fusion and edited on review.
 * Stored on [com.strobingn.wildlifefieldops.data.model.CaptureSession.draftJson].
 */
@Serializable
data class MultimodalDraftSnapshot(
    val schemaVersion: Int = 1,
    val title: String = "",
    val customerName: String = "",
    val address: String = "",
    val speciesLabelIds: List<ScoredLabel> = emptyList(),
    val damageLabelIds: List<ScoredLabel> = emptyList(),
    val severity: Int = 0,
    val severityConfidence: Float = 0f,
    val serviceType: String = "",
    val priority: String = "MEDIUM",
    val findings: String = "",
    val recommendations: String = "",
    val entryPoints: String = "",
    val notes: String = "",
    val estimatedPriceLow: Double = 0.0,
    val estimatedPriceHigh: Double = 0.0,
    val fieldProvenance: Map<String, FieldProvenance> = emptyMap(),
    val photoIds: List<String> = emptyList(),
    val needsReviewFields: List<String> = emptyList(),
    val fusionWarnings: List<String> = emptyList()
)

@Serializable
data class ScoredLabel(
    val labelId: String,
    val displayLabel: String,
    val confidence: Float,
    val provenance: FieldProvenance,
    val accepted: Boolean = false
)
