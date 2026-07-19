package com.strobingn.wildlifefieldops.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.strobingn.wildlifefieldops.data.preferences.AppSettingsKeys
import com.strobingn.wildlifefieldops.data.preferences.settingsDataStore
import com.strobingn.wildlifefieldops.data.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val preferences = applicationContext.settingsDataStore.data.first()
        val autoSync = preferences[AppSettingsKeys.AUTO_SYNC] ?: true
        val offlineMode = preferences[AppSettingsKeys.OFFLINE_MODE] ?: false

        if (!autoSync || offlineMode) {
            android.util.Log.i(
                "SyncWorker",
                "Skipping sync: autoSync=$autoSync offlineMode=$offlineMode"
            )
            return Result.success()
        }

        if (!syncRepository.isCloudConfigured()) {
            android.util.Log.i("SyncWorker", "Skipping sync: cloud is not configured")
            return Result.success()
        }

        return try {
            val result = syncRepository.syncAll()
            if (result.success) Result.success() else Result.retry()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "Sync failed", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "wildlife_fieldops_sync"
    }
}
