package com.strobingn.wildlifefieldops.ml.domain

import com.strobingn.wildlifefieldops.ml.model.DraftHints
import com.strobingn.wildlifefieldops.ml.model.VoiceParseResult

/**
 * Turns raw STT / typed transcript into taxonomy-backed structured fields.
 */
interface VoiceJobParser {
    suspend fun parse(
        transcript: String,
        hints: DraftHints = DraftHints()
    ): VoiceParseResult
}
