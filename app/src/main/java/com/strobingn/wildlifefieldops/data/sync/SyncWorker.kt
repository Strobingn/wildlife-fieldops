package com.strobingn.wildlifefieldopsfieldops.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.strobingn.wildlifefieldopsfieldops.data.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val result = syncRepository.syncAll()
            if (result.success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "Sync failed", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "wildlife_fieldops_sync"
    }
}
