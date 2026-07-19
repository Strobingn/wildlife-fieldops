package com.strobingn.wildlifefieldops.data.repository

import com.strobingn.wildlifefieldops.data.local.CustomerDao
import com.strobingn.wildlifefieldops.data.local.InspectionDao
import com.strobingn.wildlifefieldops.data.local.InvoiceDao
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.local.PendingOperationDao
import com.strobingn.wildlifefieldops.data.model.Customer
import com.strobingn.wildlifefieldops.data.model.EntityType
import com.strobingn.wildlifefieldops.data.model.Inspection
import com.strobingn.wildlifefieldops.data.model.Invoice
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.data.model.OperationType
import com.strobingn.wildlifefieldops.data.model.PendingOperation
import com.strobingn.wildlifefieldops.data.remote.RemoteCustomerDto
import com.strobingn.wildlifefieldops.data.remote.RemoteInspectionDto
import com.strobingn.wildlifefieldops.data.remote.RemoteInvoiceDto
import com.strobingn.wildlifefieldops.data.remote.RemoteJobDto
import com.strobingn.wildlifefieldops.data.remote.SupabaseService
import com.strobingn.wildlifefieldops.data.remote.toLocal
import com.strobingn.wildlifefieldops.data.remote.toRemoteDto
import com.strobingn.wildlifefieldops.data.remote.toRemoteDtoOrNull
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SyncResult(
    val pushedInvoices: Int = 0,
    val pulledInvoices: Int = 0,
    val success: Boolean,
    val message: String,
    val pushedJobs: Int = 0,
    val pushedCustomers: Int = 0,
    val pushedInspections: Int = 0,
    val pulledJobs: Int = 0,
    val pulledCustomers: Int = 0
)

@Singleton
class SyncRepository @Inject constructor(
    private val supabaseService: SupabaseService,
    private val jobDao: JobDao,
    private val customerDao: CustomerDao,
    private val inspectionDao: InspectionDao,
    private val invoiceDao: InvoiceDao,
    private val pendingOperationDao: PendingOperationDao
) {
    fun isCloudConfigured(): Boolean = supabaseService.isConfigured

    suspend fun syncAll(): SyncResult = withContext(Dispatchers.IO) {
        try {
            doSync()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            android.util.Log.e("SyncRepository", "Sync crashed", t)
            SyncResult(
                success = false,
                message = "Sync failed: ${t.message ?: t.javaClass.simpleName}. Check connection and Supabase config."
            )
        }
    }

    private suspend fun doSync(): SyncResult {
        val client = supabaseService.client
            ?: return SyncResult(
                success = false,
                message = "Cloud not configured. Rebuild the APK with Supabase secrets (VITE_SUPABASE_URL / VITE_SUPABASE_ANON_KEY)."
            )

        // Retry previously failed pushes tracked in the sync queue before the regular pass.
        val queuedRetries = processPendingOperations(client)

        var pushedJobs = 0
        var pushedCustomers = 0
        var pushedInspections = 0
        var pushedInvoices = 0
        var pulledJobs = 0
        var pulledCustomers = 0
        var pulledInvoices = 0
        val warnings = mutableListOf<String>()
        val pushedJobIds = mutableSetOf<String>()

        var unsyncedCustomers = emptyList<Customer>()
        try {
            unsyncedCustomers = customerDao.getUnsynced()
            if (unsyncedCustomers.isNotEmpty()) {
                val dtos = unsyncedCustomers.mapNotNull { customer ->
                    runCatching { customer.toRemoteDto() }
                        .onFailure { android.util.Log.w("SyncRepository", "Skip customer ${customer.id}: ${it.message}") }
                        .getOrNull()
                }
                if (dtos.isNotEmpty()) {
                    client.from("customers").upsert(dtos)
                    unsyncedCustomers.forEach { customerDao.markSynced(it.id) }
                    unsyncedCustomers.forEach { pendingOperationDao.deleteByEntity(EntityType.CUSTOMER, it.id) }
                    pushedCustomers = dtos.size
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncRepository", "Customer push failed", e)
            warnings += "customer push: ${e.message ?: e.javaClass.simpleName}"
            enqueueFailed(EntityType.CUSTOMER, unsyncedCustomers.map { it.id }, e.message ?: e.javaClass.simpleName)
        }

        var unsyncedJobs = emptyList<Job>()
        try {
            unsyncedJobs = jobDao.getUnsynced()
            if (unsyncedJobs.isNotEmpty()) {
                val dtos = unsyncedJobs.mapNotNull { job ->
                    runCatching { job.toRemoteDto() }
                        .onFailure { android.util.Log.w("SyncRepository", "Skip job ${job.id}: ${it.message}") }
                        .getOrNull()
                }
                if (dtos.isNotEmpty()) {
                    client.from("jobs").upsert(dtos)
                    pushedJobIds += dtos.map { it.id }
                    unsyncedJobs.filter { it.id in pushedJobIds }.forEach { jobDao.markSynced(it.id) }
                    unsyncedJobs.filter { it.id in pushedJobIds }.forEach { pendingOperationDao.deleteByEntity(EntityType.JOB, it.id) }
                    pushedJobs = dtos.size
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncRepository", "Job push failed", e)
            warnings += "job push: ${e.message ?: e.javaClass.simpleName}"
            enqueueFailed(EntityType.JOB, unsyncedJobs.map { it.id }, e.message ?: e.javaClass.simpleName)
        }

        var unsyncedInspections = emptyList<Inspection>()
        try {
            unsyncedInspections = inspectionDao.getUnsynced()
            if (unsyncedInspections.isNotEmpty()) {
                val dtos = mutableListOf<RemoteInspectionDto>()
                val okIds = mutableListOf<String>()
                unsyncedInspections.forEach { inspection ->
                    runCatching {
                        dtos += inspection.toRemoteDtoOrNull()
                        okIds += inspection.id
                    }.onFailure {
                        android.util.Log.w("SyncRepository", "Skip inspection ${inspection.id}: ${it.message}")
                    }
                }
                if (dtos.isNotEmpty()) {
                    client.from("inspections").upsert(dtos)
                    okIds.forEach { inspectionDao.markSynced(it) }
                    okIds.forEach { pendingOperationDao.deleteByEntity(EntityType.INSPECTION, it) }
                    pushedInspections = dtos.size
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("SyncRepository", "Inspection push skipped", e)
            warnings += "inspection push: ${e.message ?: e.javaClass.simpleName}"
            enqueueFailed(EntityType.INSPECTION, unsyncedInspections.map { it.id }, e.message ?: e.javaClass.simpleName)
        }

        // --- Push invoices ---
        var unsyncedInvoices = emptyList<Invoice>()
        try {
            unsyncedInvoices = invoiceDao.getUnsynced()
            if (unsyncedInvoices.isNotEmpty()) {
                val dtos = unsyncedInvoices.mapNotNull { invoice ->
                    runCatching { invoice.toRemoteDto() }
                        .onFailure { android.util.Log.w("SyncRepository", "Skip invoice ${invoice.id}: ${it.message}") }
                        .getOrNull()
                }
                if (dtos.isNotEmpty()) {
                    client.from("invoices").upsert(dtos)
                    dtos.forEach { invoiceDao.markSynced(it.id) }
                    dtos.forEach { pendingOperationDao.deleteByEntity(EntityType.INVOICE, it.id) }
                    pushedInvoices = dtos.size
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("SyncRepository", "Invoice push skipped", e)
            warnings += "invoice push: ${e.message ?: e.javaClass.simpleName}"
            enqueueFailed(EntityType.INVOICE, unsyncedInvoices.map { it.id }, e.message ?: e.javaClass.simpleName)
        }

        try {
            val remoteCustomers = client.from("customers").select().decodeList<RemoteCustomerDto>()
            if (remoteCustomers.isNotEmpty()) {
                val locals = remoteCustomers.mapNotNull { dto ->
                    runCatching { dto.toLocal() }
                        .onFailure { android.util.Log.w("SyncRepository", "Bad customer ${dto.id}: ${it.message}") }
                        .getOrNull()
                }
                if (locals.isNotEmpty()) {
                    customerDao.insertAll(locals)
                    pulledCustomers = locals.size
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("SyncRepository", "Customer pull failed", e)
            warnings += "customer pull: ${e.message ?: e.javaClass.simpleName}"
        }

        try {
            val remoteJobs = client.from("jobs").select().decodeList<RemoteJobDto>()
            if (remoteJobs.isNotEmpty()) {
                val locals = remoteJobs.mapNotNull { dto ->
                    runCatching {
                        val remote = dto.toLocal()
                        val existing = jobDao.getById(dto.id)

                        // Never replace a record just pushed in this same sync pass.
                        if (dto.id in pushedJobIds) {
                            null
                        } else if (existing != null && !existing.isSynced) {
                            // Local unsynced edits always win until they are successfully pushed.
                            null
                        } else {
                            remote.copy(
                                // Older/partial cloud rows often return zero for these fields.
                                // Preserve the known local amount instead of erasing it.
                                estimatedValue = if (remote.estimatedValue != 0.0 || existing == null) {
                                    remote.estimatedValue
                                } else {
                                    existing.estimatedValue
                                },
                                actualCost = if (remote.actualCost != 0.0 || existing == null) {
                                    remote.actualCost
                                } else {
                                    existing.actualCost
                                },
                                latitude = remote.latitude ?: existing?.latitude,
                                longitude = remote.longitude ?: existing?.longitude,
                                isSynced = true,
                                syncError = null
                            )
                        }
                    }.onFailure {
                        android.util.Log.w("SyncRepository", "Bad job ${dto.id}: ${it.message}")
                    }.getOrNull()
                }
                if (locals.isNotEmpty()) {
                    jobDao.insertAll(locals)
                    pulledJobs = locals.size
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("SyncRepository", "Job pull failed", e)
            warnings += "job pull: ${e.message ?: e.javaClass.simpleName}"
        }

        val base = "Synced. Pushed: $pushedJobs jobs, $pushedCustomers customers, $pushedInspections inspections, $pushedInvoices invoices. " +
            "Pulled: $pulledJobs jobs, $pulledCustomers customers, $pulledInvoices invoices."
        val withRetries = if (queuedRetries > 0) "$base Retried $queuedRetries queued operations." else base
        val message = if (warnings.isEmpty()) withRetries else "$withRetries Warnings: ${warnings.joinToString("; ")}"

        return SyncResult(
            success = true,
            message = message,
            pushedJobs = pushedJobs,
            pushedCustomers = pushedCustomers,
            pushedInspections = pushedInspections,
            pulledJobs = pulledJobs,
            pulledCustomers = pulledCustomers,
            pushedInvoices = pushedInvoices,
            pulledInvoices = pulledInvoices
        )
    }

    /**
     * Re-attempts pushes that failed in earlier sync runs. Each operation re-reads the
     * entity from Room (always the freshest data), upserts it, and is removed on success.
     * Failures bump the retry count via [PendingOperationDao.markFailed]; operations that
     * exhaust retries stay in the queue for manual retry from the Sync Queue screen.
     */
    private suspend fun processPendingOperations(client: SupabaseClient): Int {
        val pending = runCatching {
            // Clear isProcessing flags left behind if a previous run was killed mid-operation.
            pendingOperationDao.resetProcessing()
            pendingOperationDao.getPending()
        }.getOrElse { return 0 }

        var succeeded = 0
        pending.filter { it.canRetry() }.forEach { op ->
            pendingOperationDao.markProcessing(op.id)
            try {
                when (op.entityType) {
                    EntityType.JOB -> jobDao.getById(op.entityId)?.let { job ->
                        client.from("jobs").upsert(job.toRemoteDto())
                        jobDao.markSynced(job.id)
                    }
                    EntityType.CUSTOMER -> customerDao.getById(op.entityId)?.let { customer ->
                        client.from("customers").upsert(customer.toRemoteDto())
                        customerDao.markSynced(customer.id)
                    }
                    EntityType.INSPECTION -> inspectionDao.getById(op.entityId)?.let { inspection ->
                        client.from("inspections").upsert(inspection.toRemoteDtoOrNull())
                        inspectionDao.markSynced(inspection.id)
                    }
                    EntityType.INVOICE -> invoiceDao.getById(op.entityId)?.let { invoice ->
                        client.from("invoices").upsert(invoice.toRemoteDto())
                        invoiceDao.markSynced(invoice.id)
                    }
                    else -> null
                }
                // Success — or the entity was deleted locally, making the operation moot.
                pendingOperationDao.deleteById(op.id)
                succeeded++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("SyncRepository", "Queued ${op.entityType} ${op.entityId} failed", e)
                pendingOperationDao.markFailed(
                    op.id,
                    e.message ?: e.javaClass.simpleName,
                    System.currentTimeMillis()
                )
            }
        }
        return succeeded
    }

    /** Records failed pushes in the sync queue so they are visible and retried on later syncs. */
    private suspend fun enqueueFailed(entityType: EntityType, entityIds: List<String>, error: String) {
        entityIds.forEach { id ->
            runCatching {
                val existing = pendingOperationDao.findByEntity(entityType, id)
                if (existing != null) {
                    pendingOperationDao.update(existing.copy(lastError = error, isProcessing = false))
                } else {
                    pendingOperationDao.insert(
                        PendingOperation(
                            operationType = OperationType.SYNC,
                            entityType = entityType,
                            entityId = id,
                            lastError = error
                        )
                    )
                }
            }
        }
    }
}
