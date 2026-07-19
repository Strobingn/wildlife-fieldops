package com.strobingn.wildlifefieldops.ml.commit

import com.strobingn.wildlifefieldops.ml.model.CaptureSessionStatus
import com.strobingn.wildlifefieldops.ml.model.MultimodalDraftSnapshot

/**
 * Pure validation for capture commit (unit-tested, no Room).
 */
object CaptureCommitValidator {

    data class Outcome(val ok: Boolean, val message: String = "")

    fun validateSessionStatus(status: CaptureSessionStatus): Outcome {
        return when (status) {
            CaptureSessionStatus.DRAFT, CaptureSessionStatus.REVIEW -> Outcome(true)
            CaptureSessionStatus.COMMITTED ->
                Outcome(false, "Session already committed")
            CaptureSessionStatus.DISCARDED ->
                Outcome(false, "Session was discarded")
        }
    }

    /**
     * Design rule: at least one of non-blank findings/notes, ≥1 photo, or non-unknown species.
     */
    fun validateDraftContent(draft: MultimodalDraftSnapshot): Outcome {
        val hasNotes = draft.findings.isNotBlank() || draft.notes.isNotBlank()
        val hasPhotos = draft.photoIds.isNotEmpty()
        val hasSpecies = draft.speciesLabelIds.any {
            it.labelId !in setOf("unknown", "none", "")
        }
        if (hasNotes || hasPhotos || hasSpecies) {
            return Outcome(true)
        }
        return Outcome(
            false,
            "Add findings/notes, at least one photo, or a confirmed species before saving"
        )
    }
}
