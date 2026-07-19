package com.strobingn.wildlifefieldops.ml.model

/**
 * Single GPS reading from Fused Location (or manual). Used by fusion only when accuracy is usable.
 */
data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val addressGuess: String = "",
    val capturedAt: Long = System.currentTimeMillis()
) {
    /** Design rule: use fix for draft GPS when accuracy ≤ 50m or accuracy unknown (caller risk). */
    fun isUsable(maxAccuracyMeters: Float = 50f): Boolean {
        val acc = accuracyMeters ?: return true
        return acc in 0f..maxAccuracyMeters
    }
}
