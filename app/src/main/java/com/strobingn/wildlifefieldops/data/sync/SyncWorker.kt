package com.strobingn.wildlifefieldops.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.strobingn.wildlifefieldops.data.repository.SyncQueueRepository
import com.strobingn.wildlifefieldops.data.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val syncQueueRepository: SyncQueueRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val queuedCount = syncQueueRepository.prepareQueue()
        val batch = syncQueueRepository.readyBatch()

        if (batch.isEmpty()) {
            syncQueueRepository.pruneCompleted()
            return Result.success(
                Data.Builder()
                    .putString(KEY_MESSAGE, "Nothing waiting to sync.")
                    .putInt(KEY_QUEUED_ITEMS, queuedCount)
                    .putInt(KEY_COMPLETED_QUEUE_ITEMS, 0)
                    .build()
            )
        }

        if (!syncRepository.isCloudConfigured()) {
            syncQueueRepository.markRemainingFailed(batch, "Cloud sync is not configured.")
            return Result.failure(
                Data.Builder()
                    .putString(KEY_MESSAGE, "Cloud sync is not configured.")
                    .putInt(KEY_QUEUED_ITEMS, queuedCount)
                    .build()
            )
        }

        syncQueueRepository.markProcessing(batch)
        val result = syncRepository.syncAll()
        val completedCount = syncQueueRepository.reconcile(batch)
        val completedIds = batch.take(completedCount).map { it.id }.toSet()
        val remaining = batch.filterNot { it.id in completedIds }

        if (remaining.isNotEmpty()) {
            syncQueueRepository.markRemainingFailed(remaining, result.message)
        }
        syncQueueRepository.pruneCompleted()

        val output = Data.Builder()
            .putString(KEY_MESSAGE, result.message)
            .putInt(KEY_QUEUED_ITEMS, queuedCount)
            .putInt(KEY_COMPLETED_QUEUE_ITEMS, completedCount)
            .putInt(KEY_PUSHED_JOBS, result.pushedJobs)
            .putInt(KEY_PUSHED_CUSTOMERS, result.pushedCustomers)
            .putInt(KEY_PUSHED_INSPECTIONS, result.pushedInspections)
            .putInt(KEY_PUSHED_INVOICES, result.pushedInvoices)
            .putInt(KEY_PULLED_JOBS, result.pulledJobs)
            .putInt(KEY_PULLED_CUSTOMERS, result.pulledCustomers)
            .putInt(KEY_PULLED_INVOICES, result.pulledInvoices)
            .build()

        return when {
            remaining.isEmpty() -> Result.success(output)
            runAttemptCount < MAX_RETRY_ATTEMPTS -> Result.retry()
            else -> Result.failure(output)
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "wildlife-fieldops-periodic-sync"
        const val KEY_MESSAGE = "sync_message"
        const val KEY_QUEUED_ITEMS = "queued_items"
        const val KEY_COMPLETED_QUEUE_ITEMS = "completed_queue_items"
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
