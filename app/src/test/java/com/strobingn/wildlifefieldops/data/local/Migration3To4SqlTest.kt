package com.strobingn.wildlifefieldops.data.local

import com.strobingn.wildlifefieldops.data.model.CaptureSession
import com.strobingn.wildlifefieldops.data.model.TrainingLabel
import com.strobingn.wildlifefieldops.data.model.VisionPrediction
import com.strobingn.wildlifefieldops.ml.model.CaptureSessionStatus
import com.strobingn.wildlifefieldops.ml.model.LabelSource
import com.strobingn.wildlifefieldops.ml.model.ModelBackend
import com.strobingn.wildlifefieldops.ml.model.MultimodalDraftSnapshot
import com.strobingn.wildlifefieldops.ml.model.PredictionTarget
import com.strobingn.wildlifefieldops.ml.model.ScoredLabel
import com.strobingn.wildlifefieldops.ml.model.FieldProvenance
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards migration version bounds and that P0 model types construct / serialize.
 * Full instrumented Room migration is out of scope for CI without device.
 */
class Migration3To4SqlTest {

    @Test
    fun migration_3_4_hasExpectedVersionBounds() {
        val migration = AppDatabase.MIGRATION_3_4
        assertEquals(3, migration.startVersion)
        assertEquals(4, migration.endVersion)
    }

    @Test
    fun migration_2_3_stillPresent() {
        val migration = AppDatabase.MIGRATION_2_3
        assertEquals(2, migration.startVersion)
        assertEquals(3, migration.endVersion)
    }

    @Test
    fun visionPrediction_defaultsAreStable() {
        val row = VisionPrediction(photoId = "photo-1", labelId = "raccoon", displayLabel = "Raccoon")
        assertEquals("photo-1", row.photoId)
        assertEquals(ModelBackend.ML_KIT_LABELING, row.backend)
        assertEquals(PredictionTarget.SPECIES, row.target)
        assertTrue(row.id.isNotBlank())
    }

    @Test
    fun trainingLabel_and_captureSession_construct() {
        val label = TrainingLabel(
            photoId = "photo-1",
            target = PredictionTarget.SPECIES,
            labelId = "raccoon",
            source = LabelSource.MODEL_ACCEPTED
        )
        val session = CaptureSession(status = CaptureSessionStatus.DRAFT)
        assertEquals(LabelSource.MODEL_ACCEPTED, label.source)
        assertEquals(CaptureSessionStatus.DRAFT, session.status)
        assertEquals("{}", session.draftJson)
    }

    @Test
    fun multimodalDraft_roundTripsJson() {
        val draft = MultimodalDraftSnapshot(
            title = "Raccoon — Main St",
            speciesLabelIds = listOf(
                ScoredLabel(
                    labelId = "raccoon",
                    displayLabel = "Raccoon",
                    confidence = 0.9f,
                    provenance = FieldProvenance.VISION,
                    accepted = true
                )
            ),
            severity = 2,
            needsReviewFields = listOf("address")
        )
        val json = Json.encodeToString(draft)
        val back = Json.decodeFromString<MultimodalDraftSnapshot>(json)
        assertEquals(draft.title, back.title)
        assertEquals(1, back.speciesLabelIds.size)
        assertEquals("raccoon", back.speciesLabelIds.first().labelId)
        assertEquals(FieldProvenance.VISION, back.speciesLabelIds.first().provenance)
    }
}
