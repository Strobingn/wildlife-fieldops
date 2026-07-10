package com.strobingn.wildlifefieldops

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WildlifeFieldOpsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Catch uncaught crashes so we can identify future launch failures from logcat.
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("WildlifeFieldOps", "FATAL on ${thread.name}", throwable)
            previous?.uncaughtException(thread, throwable)
        }
        Log.i("WildlifeFieldOps", "App starting v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    }
}
