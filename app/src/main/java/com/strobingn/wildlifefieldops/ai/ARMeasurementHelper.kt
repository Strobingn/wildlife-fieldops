package com.strobingn.wildlifefieldops.ai

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException

object ARMeasurementHelper {

    /**
     * ARCore may initially report a transient UNKNOWN_CHECKING state. That must not be
     * cached as unsupported, otherwise the AR screen can remain unavailable for the
     * entire composition.
     */
    fun isARCoreSupported(context: Context): Boolean {
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            availability.isSupported || availability.isTransient
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Creates a configured ARCore session and requests Google Play Services for AR
     * installation when it is missing or outdated.
     *
     * A null result means installation/update was requested or ARCore is unavailable.
     * The screen can retry after the user returns from the installer.
     */
    fun createARSession(context: Context): Session? {
        return try {
            val activity = context.findActivity()
            if (activity != null) {
                val installStatus = ArCoreApk.getInstance().requestInstall(activity, true)
                if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                    return null
                }
            }

            val availability = ArCoreApk.getInstance().checkAvailability(context)
            if (!availability.isSupported && !availability.isTransient) {
                return null
            }

            val session = Session(context)
            val config = Config(session).apply {
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL

                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    depthMode = Config.DepthMode.AUTOMATIC
                }
            }
            session.configure(config)
            session
        } catch (_: UnavailableException) {
            null
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    data class MeasurementResult(
        val distanceMeters: Float,
        val confidence: Float,
        val planeType: String = "horizontal",
        val notes: String = "AR measured damage/entry point size"
    )
}
