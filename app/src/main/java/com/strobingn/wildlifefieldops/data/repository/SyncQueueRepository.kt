package com.strobingn.wildlifefieldops.data.repository

import com.strobingn.wildlifefieldops.data.local.CustomerDao
import com.strobingn.wildlifefieldops.data.local.InspectionDao
import com.strobingn.wildlifefieldops.data.local.InvoiceDao
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.local.SyncQueueDao
import com.strobingn.wildlifefieldops.data.model.SyncEntityType
import com.strobingn.wildlifefieldops.data.model.SyncOperation
import com.strobingn.wildlifefieldops.data.model.SyncQueueItem
import com.strobingn.wildlifefieldops.data.model.SyncQueueStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncQueueRepository @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val jobDao: JobDao,
    private val customerDao: CustomerDao,
    private val inspectionDao: InspectionDao,
    private val invoiceDao: InvoiceDao
) {
    val pendingCount: Flow<Int> = syncQueueDao.observePendingCount()
    val activeItems: Flow<List<SyncQueueItem>> = syncQueueDao.observeActive()

    suspend fun prepareQueue(): Int {
        val items = buildList {
            addAll(jobDao.getUnsynced().map { it.toQueueItem(SyncEntityType.JOB) })
            addAll(customerDao.getUnsynced().map { it.toQueueItem(SyncEntityType.CUSTOMER) })
            addAll(inspectionDao.getUnsynced().map { it.toQueueItem(SyncEntityType.INSPECTION) })
            addAll(invoiceDao.getUnsynced().map { it.toQueueItem(SyncEntityType.INVOICE) })
        }

        if (items.isEmpty()) return 0
        return syncQueueDao.insertAll(items).count { it != -1L }
    }

    suspend fun readyBatch(limit: Int = 100): List<SyncQueueItem> =
        syncQueueDao.getReady(limit = limit)

    suspend fun markProcessing(items: List<SyncQueueItem>) {
        if (items.isNotEmpty()) {
            syncQueueDao.updateStatus(items.map { it.id }, SyncQueueStatus.IN_PROGRESS)
        }
    }

    suspend fun reconcile(items: List<SyncQueueItem>): Set<String> {
        if (items.isEmpty()) return emptySet()

        val completedIds = items.filter { item ->
            when (item.entityType) {
                SyncEntityType.JOB -> jobDao.getById(item.entityId)?.isSynced != false
                SyncEntityType.CUSTOMER -> customerDao.getById(item.entityId)?.isSynced != false
                SyncEntityType.INSPECTION -> inspectionDao.getById(item.entityId)?.isSynced != false
                SyncEntityType.INVOICE -> invoiceDao.getById(item.entityId)?.isSynced != false
            }
        }.mapTo(mutableSetOf()) { it.id }

        if (completedIds.isNotEmpty()) syncQueueDao.markCompleted(completedIds.toList())
        return completedIds
    }

    suspend fun markRemainingFailed(items: List<SyncQueueItem>, message: String) {
        if (items.isEmpty()) return
        val delayMinutes = items.maxOfOrNull { (it.attemptCount + 1).coerceAtMost(6) } ?: 1
        val nextAttemptAt = System.currentTimeMillis() + delayMinutes * 60_000L
        syncQueueDao.markFailed(
            ids = items.map { it.id },
            error = message.take(MAX_ERROR_LENGTH),
            nextAttemptAt = nextAttemptAt
        )
    }

    suspend fun pruneCompleted(): Int =
        syncQueueDao.pruneCompleted(System.currentTimeMillis() - COMPLETED_RETENTION_MS)

    private fun Any.toQueueItem(entityType: SyncEntityType): SyncQueueItem {
        val entityId = when (this) {
            is com.strobingn.wildlifefieldops.data.model.Job -> id
            is com.strobingn.wildlifefieldops.data.model.Customer -> id
            is com.strobingn.wildlifefieldops.data.model.Inspection -> id
            is com.strobingn.wildlifefieldops.data.model.Invoice -> id
            else -> error("Unsupported sync entity: ${this::class.java.simpleName}")
        }
        val operation = SyncOperation.UPSERT
        return SyncQueueItem(
            id = "${entityType.name}:$entityId:${operation.name}",
            entityType = entityType,
            entityId = entityId,
            operation = operation
        )
    }

    companion object {
        private const val MAX_ERROR_LENGTH = 500
        private const val COMPLETED_RETENTION_MS = 7L * 24L * 60L * 60L * 1000L
    }
}
