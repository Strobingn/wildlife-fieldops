package com.strobingn.wildlifefieldops.ai

import android.content.Context

/**
 * AR + Vision helper for on-site measurement using ARCore + ML Kit.
 * Point camera at damage/entry point → auto measure size/area, overlay annotations, save annotated photo.
 */
object ARMeasurementHelper {

    fun launchARMeasurement(context: Context, onResult: (widthCm: Float, heightCm: Float, annotatedImageUri: String?) -> Unit) {
        // TODO: Integrate ARCore Sceneform or Jetpack XR + ML Kit Object Detection / Pose
        // For now: stub that simulates measurement
        // In production: start AR session, detect plane + object, calculate real-world size using depth, return annotated bitmap
        onResult(12.5f, 8.3f, null) // Example: 12.5cm x 8.3cm entry point
    }
}