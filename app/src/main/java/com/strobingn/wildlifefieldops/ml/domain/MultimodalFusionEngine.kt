package com.strobingn.wildlifefieldops.ml.domain

import com.strobingn.wildlifefieldops.data.model.VisionPrediction
import com.strobingn.wildlifefieldops.ml.model.GpsFix
import com.strobingn.wildlifefieldops.ml.model.MultimodalDraftSnapshot
import com.strobingn.wildlifefieldops.ml.model.VoiceParseResult

/**
 * Deterministic merge of voice + vision + GPS into a review draft.
 * Pure rules — unit tested; no network.
 */
interface MultimodalFusionEngine {
    fun fuse(
        voice: VoiceParseResult?,
        visions: List<VisionPrediction>,
        gps: GpsFix?,
        existingDraft: MultimodalDraftSnapshot = MultimodalDraftSnapshot(),
        /** When true, fields already marked USER in existingDraft.provenance are never overwritten. */
        lockUserFields: Boolean = true
    ): MultimodalDraftSnapshot
}
