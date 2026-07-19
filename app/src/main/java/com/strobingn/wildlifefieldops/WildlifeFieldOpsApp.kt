package com.strobingn.wildlifefieldops

import android.app.Application
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.strobingn.wildlifefieldops.data.preferences.AppSettingsKeys
import com.strobingn.wildlifefieldops.data.preferences.settingsDataStore
import com.strobingn.wildlifefieldops.data.sync.SyncWorker
import com.strobingn.wildlifefieldops.ui.theme.ThemeController
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val DARK_THEME = booleanPreferencesKey("dark_theme")

@HiltAndroidApp
class WildlifeFieldOpsApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("WildlifeFieldOps", "FATAL on ${thread.name}", throwable)
            previous?.uncaughtException(thread, throwable)
        }
        Log.i("WildlifeFieldOps", "App starting v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")

        appScope.launch {
            restorePersistedTheme()
            configurePeriodicSync()
        }
    }

    private suspend fun restorePersistedTheme() {
        val darkTheme = settingsDataStore.data.first()[DARK_THEME] ?: true
        ThemeController.setDark(darkTheme)
    }

    private suspend fun configurePeriodicSync() {
        val preferences = settingsDataStore.data.first()
        val autoSync = preferences[AppSettingsKeys.AUTO_SYNC] ?: true
        val offlineMode = preferences[AppSettingsKeys.OFFLINE_MODE] ?: false
        val requestedInterval = preferences[AppSettingsKeys.SYNC_INTERVAL] ?: 15
        val intervalMinutes = requestedInterval.coerceAtLeast(15).toLong()
        val cloudConfigured = BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

        val workManager = WorkManager.getInstance(this)
        if (!autoSync || offlineMode || !cloudConfigured) {
            workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
            Log.i(
                "WildlifeFieldOps",
                "Periodic sync disabled: autoSync=$autoSync offline=$offlineMode cloudConfigured=$cloudConfigured"
            )
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )

        Log.i("WildlifeFieldOps", "Periodic sync scheduled every $intervalMinutes minutes")
    }
}
