package com.strobingn.wildlifefieldops.ml

import com.strobingn.wildlifefieldops.ml.commit.CaptureCommitValidator
import com.strobingn.wildlifefieldops.ml.model.CaptureSessionStatus
import com.strobingn.wildlifefieldops.ml.model.FieldProvenance
import com.strobingn.wildlifefieldops.ml.model.MultimodalDraftSnapshot
import com.strobingn.wildlifefieldops.ml.model.ScoredLabel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureCommitValidatorTest {

    @Test
    fun draftAndReview_allowed() {
        assertTrue(CaptureCommitValidator.validateSessionStatus(CaptureSessionStatus.DRAFT).ok)
        assertTrue(CaptureCommitValidator.validateSessionStatus(CaptureSessionStatus.REVIEW).ok)
    }

    @Test
    fun committedAndDiscarded_blocked() {
        assertFalse(CaptureCommitValidator.validateSessionStatus(CaptureSessionStatus.COMMITTED).ok)
        assertFalse(CaptureCommitValidator.validateSessionStatus(CaptureSessionStatus.DISCARDED).ok)
    }

    @Test
    fun emptyDraft_fails() {
        val r = CaptureCommitValidator.validateDraftContent(MultimodalDraftSnapshot())
        assertFalse(r.ok)
        assertTrue(r.message.contains("photo", ignoreCase = true) || r.message.contains("species", ignoreCase = true))
    }

    @Test
    fun notesOnly_ok() {
        assertTrue(
            CaptureCommitValidator.validateDraftContent(
                MultimodalDraftSnapshot(notes = "Checked attic")
            ).ok
        )
    }

    @Test
    fun findingsOnly_ok() {
        assertTrue(
            CaptureCommitValidator.validateDraftContent(
                MultimodalDraftSnapshot(findings = "Droppings on joists")
            ).ok
        )
    }

    @Test
    fun photoOnly_ok() {
        assertTrue(
            CaptureCommitValidator.validateDraftContent(
                MultimodalDraftSnapshot(photoIds = listOf("p1"))
            ).ok
        )
    }

    @Test
    fun knownSpeciesOnly_ok() {
        assertTrue(
            CaptureCommitValidator.validateDraftContent(
                MultimodalDraftSnapshot(
                    speciesLabelIds = listOf(
                        ScoredLabel("raccoon", "Raccoon", 0.9f, FieldProvenance.VISION, true)
                    )
                )
            ).ok
        )
    }

    @Test
    fun unknownSpeciesOnly_failsWithoutNotesOrPhotos() {
        assertFalse(
            CaptureCommitValidator.validateDraftContent(
                MultimodalDraftSnapshot(
                    speciesLabelIds = listOf(
                        ScoredLabel("unknown", "Unknown", 0.4f, FieldProvenance.VISION, false)
                    )
                )
            ).ok
        )
    }
}
