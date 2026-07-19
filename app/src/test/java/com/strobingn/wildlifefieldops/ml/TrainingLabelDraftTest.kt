package com.strobingn.wildlifefieldops.ml

import com.strobingn.wildlifefieldops.ml.commit.TrainingLabelDraft
import com.strobingn.wildlifefieldops.ml.model.FieldProvenance
import com.strobingn.wildlifefieldops.ml.model.LabelSource
import com.strobingn.wildlifefieldops.ml.model.PredictionTarget
import com.strobingn.wildlifefieldops.ml.model.ScoredLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingLabelDraftTest {

    @Test
    fun fromScoredLabels_buildsSpeciesAndDamage() {
        val drafts = TrainingLabelDraft.fromScoredLabels(
            photoId = "photo-1",
            species = listOf(
                ScoredLabel("raccoon", "Raccoon", 0.9f, FieldProvenance.VISION, accepted = true),
                ScoredLabel("unknown", "Unknown", 0.2f, FieldProvenance.VISION, accepted = false)
            ),
            damage = listOf(
                ScoredLabel("entry_hole", "Entry hole / gap", 0.8f, FieldProvenance.VOICE_NLU, accepted = true)
            ),
            createdBy = "tech"
        )
        assertEquals(2, drafts.size)
        assertTrue(drafts.any { it.labelId == "raccoon" && it.target == PredictionTarget.SPECIES })
        assertTrue(drafts.any { it.labelId == "entry_hole" && it.target == PredictionTarget.DAMAGE })
        assertEquals(LabelSource.MODEL_ACCEPTED, drafts.first { it.labelId == "raccoon" }.source)
        val entity = drafts.first().toEntity()
        assertEquals("photo-1", entity.photoId)
        assertTrue(entity.id.isNotBlank())
    }
}
