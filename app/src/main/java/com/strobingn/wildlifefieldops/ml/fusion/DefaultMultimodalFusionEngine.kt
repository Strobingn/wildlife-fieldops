package com.strobingn.wildlifefieldops.ml.fusion

import com.strobingn.wildlifefieldops.data.model.VisionPrediction
import com.strobingn.wildlifefieldops.ml.domain.MultimodalFusionEngine
import com.strobingn.wildlifefieldops.ml.model.FieldProvenance
import com.strobingn.wildlifefieldops.ml.model.GpsFix
import com.strobingn.wildlifefieldops.ml.model.MlThresholds
import com.strobingn.wildlifefieldops.ml.model.MultimodalDraftSnapshot
import com.strobingn.wildlifefieldops.ml.model.PredictionTarget
import com.strobingn.wildlifefieldops.ml.model.ScoredLabel
import com.strobingn.wildlifefieldops.ml.model.VoiceParseResult
import com.strobingn.wildlifefieldops.ml.vision.OfflinePriceBands
import com.strobingn.wildlifefieldops.ml.vision.ServiceTypeRules
import com.strobingn.wildlifefieldops.ml.vision.TaxonomyMapper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic fusion rules from DESIGN.md §4.4 — pure Kotlin, unit-tested.
 */
@Singleton
class DefaultMultimodalFusionEngine @Inject constructor(
    private val mapper: TaxonomyMapper
) : MultimodalFusionEngine {

    override fun fuse(
        voice: VoiceParseResult?,
        visions: List<VisionPrediction>,
        gps: GpsFix?,
        existingDraft: MultimodalDraftSnapshot,
        lockUserFields: Boolean
    ): MultimodalDraftSnapshot {
        val provenance = existingDraft.fieldProvenance.toMutableMap()
        val warnings = mutableListOf<String>()
        val needsReview = linkedSetOf<String>()

        fun userLocked(field: String): Boolean =
            lockUserFields && provenance[field] == FieldProvenance.USER

        val visionSpecies = visions
            .filter { it.target == PredictionTarget.SPECIES }
            .filter { it.labelId !in IGNORE }
            .filter { it.confidence >= MlThresholds.SHOW_SUGGESTION }
            .groupBy { it.labelId }
            .map { (id, rows) ->
                val best = rows.maxBy { it.confidence }
                ScoredLabel(
                    labelId = id,
                    displayLabel = best.displayLabel.ifBlank { mapper.catalog().speciesDisplay(id) },
                    confidence = best.confidence,
                    provenance = FieldProvenance.VISION,
                    accepted = best.confidence >= MlThresholds.AUTO_ACCEPT
                )
            }

        val visionDamage = visions
            .filter { it.target == PredictionTarget.DAMAGE }
            .filter { it.labelId !in IGNORE }
            .filter { it.confidence >= MlThresholds.SHOW_SUGGESTION }
            .groupBy { it.labelId }
            .map { (id, rows) ->
                val best = rows.maxBy { it.confidence }
                ScoredLabel(
                    labelId = id,
                    displayLabel = best.displayLabel.ifBlank { mapper.catalog().damageDisplay(id) },
                    confidence = best.confidence,
                    provenance = FieldProvenance.VISION,
                    accepted = best.confidence >= MlThresholds.AUTO_ACCEPT
                )
            }

        val voiceSpecies = voice?.speciesLabelIds
            ?.filter { it.labelId !in IGNORE && it.confidence >= MlThresholds.SHOW_SUGGESTION }
            .orEmpty()
        val voiceDamage = voice?.damageLabelIds
            ?.filter { it.labelId !in IGNORE && it.confidence >= MlThresholds.SHOW_SUGGESTION }
            .orEmpty()

        // --- Species fusion ---
        val fusedSpecies = if (userLocked("species") && existingDraft.speciesLabelIds.isNotEmpty()) {
            existingDraft.speciesLabelIds
        } else {
            fuseSpeciesLists(voiceSpecies, visionSpecies, warnings, needsReview)
        }
        if (!userLocked("species") && fusedSpecies.isNotEmpty()) {
            provenance["species"] = fusedSpecies.first().provenance.let {
                if (fusedSpecies.any { s -> s.provenance == FieldProvenance.FUSION }) FieldProvenance.FUSION
                else it
            }
        }

        // --- Damage union ---
        val fusedDamage = if (userLocked("damage") && existingDraft.damageLabelIds.isNotEmpty()) {
            existingDraft.damageLabelIds
        } else {
            unionById(voiceDamage + visionDamage)
                .sortedByDescending { it.confidence }
        }
        if (!userLocked("damage") && fusedDamage.isNotEmpty()) {
            provenance["damage"] = FieldProvenance.FUSION
        }

        // --- Severity ---
        var severity = if (userLocked("severity")) {
            existingDraft.severity
        } else {
            val fromDamage = mapper.estimateSeverityScore(fusedDamage.map { it.labelId })
            val fromVoice = voice?.severity ?: 0
            maxOf(fromDamage, fromVoice)
        }
        val urgencyBoost = voice?.urgencyKeywords.orEmpty().isNotEmpty() ||
            voice?.transcript?.let {
                Regex("""\b(emergency|urgent|chewed\s+wire|fire|sparks?)\b""", RegexOption.IGNORE_CASE)
                    .containsMatchIn(it)
            } == true
        if (!userLocked("severity") && urgencyBoost) {
            severity = maxOf(severity, 3)
        }
        val severityConf = when {
            userLocked("severity") -> existingDraft.severityConfidence
            urgencyBoost -> 0.85f
            fusedDamage.isNotEmpty() -> fusedDamage.maxOf { it.confidence }
            else -> voice?.severityConfidence ?: 0f
        }
        if (!userLocked("severity")) {
            provenance["severity"] = if (urgencyBoost || voice != null) FieldProvenance.VOICE_NLU else FieldProvenance.VISION
        }

        val speciesIds = fusedSpecies.map { it.labelId }
        val damageIds = fusedDamage.map { it.labelId }

        val serviceType = when {
            userLocked("serviceType") && existingDraft.serviceType.isNotBlank() -> existingDraft.serviceType
            voice?.serviceType?.isNotBlank() == true -> voice.serviceType
            else -> ServiceTypeRules.serviceTypeFor(speciesIds, damageIds)
        }
        if (!userLocked("serviceType")) {
            provenance["serviceType"] = FieldProvenance.FUSION
        }

        val priority = when {
            userLocked("priority") && existingDraft.priority.isNotBlank() -> existingDraft.priority
            voice?.priority?.isNotBlank() == true -> voice.priority
            else -> ServiceTypeRules.priorityFor(speciesIds, damageIds, severity)
        }
        if (!userLocked("priority")) provenance["priority"] = FieldProvenance.FUSION

        val band = OfflinePriceBands.forServiceType(serviceType)
        val priceLow = if (userLocked("price") && existingDraft.estimatedPriceLow > 0) {
            existingDraft.estimatedPriceLow
        } else band.low
        val priceHigh = if (userLocked("price") && existingDraft.estimatedPriceHigh > 0) {
            existingDraft.estimatedPriceHigh
        } else band.high
        if (!userLocked("price")) provenance["price"] = FieldProvenance.SYSTEM_DEFAULT

        val address = when {
            userLocked("address") && existingDraft.address.isNotBlank() -> existingDraft.address
            voice?.address?.isNotBlank() == true -> voice.address
            gps?.addressGuess?.isNotBlank() == true -> gps.addressGuess
            else -> existingDraft.address
        }
        if (!userLocked("address") && address.isNotBlank()) {
            provenance["address"] = when {
                voice?.address?.isNotBlank() == true -> FieldProvenance.VOICE_NLU
                gps?.addressGuess?.isNotBlank() == true -> FieldProvenance.GPS
                else -> FieldProvenance.SYSTEM_DEFAULT
            }
        }

        val customerName = when {
            userLocked("customerName") && existingDraft.customerName.isNotBlank() -> existingDraft.customerName
            voice?.customerName?.isNotBlank() == true -> voice.customerName
            else -> existingDraft.customerName
        }
        if (!userLocked("customerName") && customerName.isNotBlank()) {
            provenance["customerName"] = FieldProvenance.VOICE_NLU
        }

        val primarySpeciesName = fusedSpecies.maxByOrNull { it.confidence }?.displayLabel
        val title = when {
            userLocked("title") && existingDraft.title.isNotBlank() -> existingDraft.title
            !primarySpeciesName.isNullOrBlank() && address.isNotBlank() ->
                "$primarySpeciesName — ${shortAddress(address)}"
            !primarySpeciesName.isNullOrBlank() ->
                "$primarySpeciesName — field capture"
            else ->
                "Wildlife inspection — ${SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())}"
        }
        if (!userLocked("title")) provenance["title"] = FieldProvenance.FUSION

        val findings = when {
            userLocked("findings") && existingDraft.findings.isNotBlank() -> existingDraft.findings
            voice?.findings?.isNotBlank() == true -> voice.findings
            else -> existingDraft.findings
        }
        val recommendations = when {
            userLocked("recommendations") && existingDraft.recommendations.isNotBlank() ->
                existingDraft.recommendations
            voice?.recommendations?.isNotBlank() == true -> voice.recommendations
            else -> existingDraft.recommendations
        }
        val entryPoints = when {
            userLocked("entryPoints") && existingDraft.entryPoints.isNotBlank() -> existingDraft.entryPoints
            voice?.entryPoints?.isNotBlank() == true -> voice.entryPoints
            fusedDamage.any { it.labelId == "entry_hole" } -> "Entry points noted — verify on site"
            else -> existingDraft.entryPoints
        }
        val notes = when {
            userLocked("notes") && existingDraft.notes.isNotBlank() -> existingDraft.notes
            voice?.notes?.isNotBlank() == true -> voice.notes
            voice?.transcript?.isNotBlank() == true -> voice.transcript
            else -> existingDraft.notes
        }

        val photoIds = (existingDraft.photoIds + visions.map { it.photoId }).distinct()

        if (fusedSpecies.isEmpty() && fusedDamage.isEmpty() && findings.isBlank() && notes.isBlank() && photoIds.isEmpty()) {
            needsReview.add("findings")
            warnings.add("No species, damage, notes, or photos fused — review before commit.")
        }

        return MultimodalDraftSnapshot(
            schemaVersion = 1,
            title = title,
            customerName = customerName,
            address = address,
            speciesLabelIds = fusedSpecies,
            damageLabelIds = fusedDamage,
            severity = severity.coerceIn(0, 4),
            severityConfidence = severityConf,
            serviceType = serviceType,
            priority = priority,
            findings = findings,
            recommendations = recommendations,
            entryPoints = entryPoints,
            notes = notes,
            estimatedPriceLow = priceLow,
            estimatedPriceHigh = priceHigh,
            fieldProvenance = provenance,
            photoIds = photoIds,
            needsReviewFields = needsReview.toList(),
            fusionWarnings = (existingDraft.fusionWarnings + warnings).distinct()
        )
    }

    private fun fuseSpeciesLists(
        voice: List<ScoredLabel>,
        vision: List<ScoredLabel>,
        warnings: MutableList<String>,
        needsReview: MutableSet<String>
    ): List<ScoredLabel> {
        if (voice.isEmpty()) return vision.sortedByDescending { it.confidence }
        if (vision.isEmpty()) return voice.sortedByDescending { it.confidence }

        val voiceIds = voice.map { it.labelId }.toSet()
        val visionIds = vision.map { it.labelId }.toSet()
        val agree = voiceIds.intersect(visionIds)

        if (agree.isNotEmpty()) {
            return agree.map { id ->
                val v = voice.first { it.labelId == id }
                val i = vision.first { it.labelId == id }
                ScoredLabel(
                    labelId = id,
                    displayLabel = v.displayLabel.ifBlank { i.displayLabel },
                    confidence = maxOf(v.confidence, i.confidence),
                    provenance = FieldProvenance.FUSION,
                    accepted = maxOf(v.confidence, i.confidence) >= MlThresholds.AUTO_ACCEPT
                )
            } + (voice + vision).filter { it.labelId !in agree }
                .let { unionById(it) }
                .sortedByDescending { it.confidence }
        }

        // Disagree: keep both if above suggestion threshold; force review
        val both = unionById(voice + vision).sortedByDescending { it.confidence }
        if (voice.isNotEmpty() && vision.isNotEmpty()) {
            warnings.add(
                "Voice and vision disagree on species " +
                    "(voice=${voice.joinToString { it.labelId }}, " +
                    "vision=${vision.joinToString { it.labelId }}). Confirm on review."
            )
            needsReview.add("species")
        }
        return both
    }

    private fun unionById(labels: List<ScoredLabel>): List<ScoredLabel> {
        val map = linkedMapOf<String, ScoredLabel>()
        for (label in labels) {
            val existing = map[label.labelId]
            if (existing == null || label.confidence > existing.confidence) {
                map[label.labelId] = label
            }
        }
        return map.values.toList()
    }

    private fun shortAddress(address: String): String {
        val first = address.split(",").firstOrNull()?.trim().orEmpty()
        return first.ifBlank { address.take(40) }
    }

    companion object {
        private val IGNORE = setOf("unknown", "none", "")
    }
}
