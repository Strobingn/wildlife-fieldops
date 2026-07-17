package com.strobingn.wildlifefieldops.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.strobingn.wildlifefieldops.data.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!syncRepository.isCloudConfigured()) {
            return Result.failure(
                Data.Builder()
                    .putString(KEY_MESSAGE, "Cloud sync is not configured.")
                    .build()
            )
        }

        val result = syncRepository.syncAll()
        val output = Data.Builder()
            .putString(KEY_MESSAGE, result.message)
            .putInt(KEY_PUSHED_JOBS, result.pushedJobs)
            .putInt(KEY_PUSHED_CUSTOMERS, result.pushedCustomers)
            .putInt(KEY_PUSHED_INSPECTIONS, result.pushedInspections)
            .putInt(KEY_PUSHED_INVOICES, result.pushedInvoices)
            .putInt(KEY_PULLED_JOBS, result.pulledJobs)
            .putInt(KEY_PULLED_CUSTOMERS, result.pulledCustomers)
            .putInt(KEY_PULLED_INVOICES, result.pulledInvoices)
            .build()

        return when {
            result.success -> Result.success(output)
            runAttemptCount < MAX_RETRY_ATTEMPTS -> Result.retry()
            else -> Result.failure(output)
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "wildlife-fieldops-periodic-sync"
        const val KEY_MESSAGE = "sync_message"
        const val KEY_PUSHED_JOBS = "pushed_jobs"
        const val KEY_PUSHED_CUSTOMERS = "pushed_customers"
        const val KEY_PUSHED_INSPECTIONS = "pushed_inspections"
        const val KEY_PUSHED_INVOICES = "pushed_invoices"
        const val KEY_PULLED_JOBS = "pulled_jobs"
        const val KEY_PULLED_CUSTOMERS = "pulled_customers"
        const val KEY_PULLED_INVOICES = "pulled_invoices"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}
