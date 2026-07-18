package com.strobingn.wildlifefieldops.ai

import android.app.Activity
import android.content.Context
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException

object ARMeasurementHelper {

    /** High-level ARCore availability used by the AR Measure flow. */
    enum class ARSupport {
        /** ARCore service is present — a session can be created right away. */
        READY,

        /** Device can run ARCore, but "Google Play Services for AR" must be installed/updated first. */
        NEEDS_INSTALL,

        /** Availability could not be determined yet (the Play-services backed check is still in flight). */
        CHECKING,

        /** This device cannot run ARCore at all. */
        UNSUPPORTED
    }

    /**
     * Snapshot of ARCore availability. Note that [ArCoreApk.Availability.isSupported] is
     * true even when the ARCore APK is NOT installed yet, so callers must distinguish
     * READY from NEEDS_INSTALL — creating a [Session] in the NEEDS_INSTALL state throws
     * and previously left the AR screen dead with a permanent spinner.
     */
    fun checkSupport(context: Context): ARSupport {
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            when {
                availability.isUnknown -> ARSupport.CHECKING
                availability.isUnsupported -> ARSupport.UNSUPPORTED
                availability.isSupported -> {
                    if (availability == ArCoreApk.Availability.SUPPORTED_INSTALLED) ARSupport.READY
                    else ARSupport.NEEDS_INSTALL
                }
                else -> ARSupport.CHECKING
            }
        } catch (e: Exception) {
            ARSupport.CHECKING
        }
    }

    fun isARCoreSupported(context: Context): Boolean {
        return checkSupport(context).let { it == ARSupport.READY || it == ARSupport.NEEDS_INSTALL }
    }

    /**
     * Prompts (once per call) to install "Google Play Services for AR" if needed.
     * Must be called from a resumed Activity.
     *
     * @return true when ARCore is installed and a session can be created; false when the
     * user was sent to the Play Store (retry on the next ON_RESUME).
     * @throws UnavailableException when the user declined installation or the device is incompatible.
     */
    @Throws(UnavailableException::class)
    fun ensureInstalled(activity: Activity, userRequestedInstall: Boolean): Boolean {
        return when (ArCoreApk.getInstance().requestInstall(activity, userRequestedInstall)) {
            ArCoreApk.InstallStatus.INSTALLED -> true
            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> false
        }
    }

    /**
     * Creates and configures an AR session. Throws [UnavailableException] (or a
     * camera-in-use [Exception]) so the caller can surface a real error message
     * instead of silently failing.
     */
    @Throws(UnavailableException::class)
    fun createARSession(context: Context): Session {
        val session = Session(context)
        val config = Config(session)
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.focusMode = Config.FocusMode.AUTO
        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.depthMode = Config.DepthMode.AUTOMATIC
        }
        session.configure(config)
        return session
    }

    data class MeasurementResult(
        val distanceMeters: Float,
        val confidence: Float,
        val planeType: String = "horizontal",
        val notes: String = "AR measured damage/entry point size"
    )
}
