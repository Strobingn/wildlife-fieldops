package com.strobingn.wildlifefieldops.ml

import com.strobingn.wildlifefieldops.data.model.VisionPrediction
import com.strobingn.wildlifefieldops.ml.fusion.DefaultMultimodalFusionEngine
import com.strobingn.wildlifefieldops.ml.model.FieldProvenance
import com.strobingn.wildlifefieldops.ml.model.GpsFix
import com.strobingn.wildlifefieldops.ml.model.MultimodalDraftSnapshot
import com.strobingn.wildlifefieldops.ml.model.PredictionTarget
import com.strobingn.wildlifefieldops.ml.model.ScoredLabel
import com.strobingn.wildlifefieldops.ml.model.VoiceParseResult
import com.strobingn.wildlifefieldops.ml.vision.TaxonomyMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultMultimodalFusionEngineTest {

    private val engine = DefaultMultimodalFusionEngine(TaxonomyMapper.default())

    private fun vision(species: String, conf: Float, photoId: String = "p1") = VisionPrediction(
        photoId = photoId,
        target = PredictionTarget.SPECIES,
        labelId = species,
        displayLabel = species.replaceFirstChar { it.uppercase() },
        confidence = conf
    )

    private fun visionDamage(id: String, conf: Float, photoId: String = "p1") = VisionPrediction(
        photoId = photoId,
        target = PredictionTarget.DAMAGE,
        labelId = id,
        displayLabel = id,
        confidence = conf
    )

    @Test
    fun agree_voiceAndVision_speciesFused() {
        val voice = VoiceParseResult(
            transcript = "raccoon in attic",
            speciesLabelIds = listOf(
                ScoredLabel("raccoon", "Raccoon", 0.9f, FieldProvenance.VOICE_NLU, true)
            )
        )
        val draft = engine.fuse(
            voice = voice,
            visions = listOf(vision("raccoon", 0.86f)),
            gps = null
        )
        assertTrue(draft.speciesLabelIds.any { it.labelId == "raccoon" })
        assertEquals(FieldProvenance.FUSION, draft.speciesLabelIds.first { it.labelId == "raccoon" }.provenance)
        assertTrue(draft.needsReviewFields.none { it == "species" })
        assertTrue(draft.title.contains("Raccoon", ignoreCase = true))
    }

    @Test
    fun disagree_marksNeedsReview() {
        val voice = VoiceParseResult(
            transcript = "squirrel",
            speciesLabelIds = listOf(
                ScoredLabel("squirrel", "Squirrel", 0.9f, FieldProvenance.VOICE_NLU, true)
            )
        )
        val draft = engine.fuse(
            voice = voice,
            visions = listOf(vision("raccoon", 0.88f)),
            gps = null
        )
        assertTrue(draft.speciesLabelIds.any { it.labelId == "squirrel" })
        assertTrue(draft.speciesLabelIds.any { it.labelId == "raccoon" })
        assertTrue(draft.needsReviewFields.contains("species"))
        assertTrue(draft.fusionWarnings.any { it.contains("disagree", ignoreCase = true) })
    }

    @Test
    fun userLock_preservesSpecies() {
        val existing = MultimodalDraftSnapshot(
            speciesLabelIds = listOf(
                ScoredLabel("bat", "Bat", 1f, FieldProvenance.USER, true)
            ),
            fieldProvenance = mapOf("species" to FieldProvenance.USER)
        )
        val voice = VoiceParseResult(
            speciesLabelIds = listOf(
                ScoredLabel("raccoon", "Raccoon", 0.95f, FieldProvenance.VOICE_NLU, true)
            )
        )
        val draft = engine.fuse(
            voice = voice,
            visions = listOf(vision("squirrel", 0.9f)),
            gps = null,
            existingDraft = existing,
            lockUserFields = true
        )
        assertEquals(1, draft.speciesLabelIds.size)
        assertEquals("bat", draft.speciesLabelIds.first().labelId)
    }

    @Test
    fun damageUnion_andSeverityFromWire() {
        val voice = VoiceParseResult(
            damageLabelIds = listOf(
                ScoredLabel("entry_hole", "Entry hole / gap", 0.8f, FieldProvenance.VOICE_NLU, true)
            ),
            urgencyKeywords = listOf("urgent"),
            transcript = "urgent chewed wires at vent"
        )
        val draft = engine.fuse(
            voice = voice,
            visions = listOf(visionDamage("wire_damage", 0.9f)),
            gps = null
        )
        assertTrue(draft.damageLabelIds.any { it.labelId == "entry_hole" })
        assertTrue(draft.damageLabelIds.any { it.labelId == "wire_damage" })
        assertTrue(draft.severity >= 3)
        assertTrue(draft.estimatedPriceLow > 0)
    }

    @Test
    fun gpsAddressUsedWhenVoiceEmpty() {
        val draft = engine.fuse(
            voice = null,
            visions = listOf(vision("bat", 0.9f)),
            gps = GpsFix(40.0, -74.0, 12f, addressGuess = "5 River Rd")
        )
        assertEquals("5 River Rd", draft.address)
        assertTrue(draft.photoIds.contains("p1"))
    }

    @Test
    fun visionOnly_noVoice() {
        val draft = engine.fuse(
            voice = null,
            visions = listOf(vision("skunk", 0.92f), visionDamage("droppings", 0.7f)),
            gps = null
        )
        assertTrue(draft.speciesLabelIds.any { it.labelId == "skunk" })
        assertTrue(draft.damageLabelIds.any { it.labelId == "droppings" })
        assertTrue(draft.serviceType.isNotBlank())
    }
}
