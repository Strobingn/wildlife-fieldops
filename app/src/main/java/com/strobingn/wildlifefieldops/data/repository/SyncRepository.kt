package com.strobingn.wildlifefieldops.data.repository

import com.strobingn.wildlifefieldops.data.local.CustomerDao
import com.strobingn.wildlifefieldops.data.local.InspectionDao
import com.strobingn.wildlifefieldops.data.local.InvoiceDao
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.remote.RemoteCustomerDto
import com.strobingn.wildlifefieldops.data.remote.RemoteInspectionDto
import com.strobingn.wildlifefieldops.data.remote.RemoteInvoiceDto
import com.strobingn.wildlifefieldops.data.remote.RemoteJobDto
import com.strobingn.wildlifefieldops.data.remote.SupabaseService
import com.strobingn.wildlifefieldops.data.remote.toLocal
import com.strobingn.wildlifefieldops.data.remote.toRemoteDto
import com.strobingn.wildlifefieldops.data.remote.toRemoteDtoOrNull
import io.github.jan.supabase.postgrest.from
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
    private val invoiceDao: InvoiceDao
) {
    fun isCloudConfigured(): Boolean = supabaseService.isConfigured

    suspend fun syncAll(): SyncResult = withContext(Dispatchers.IO) {
        try {
            doSync()
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

        var pushedJobs = 0
        var pushedCustomers = 0
        var pushedInspections = 0
        var pushedInvoices = 0
        var pulledJobs = 0
        var pulledCustomers = 0
        var pulledInvoices = 0
        val warnings = mutableListOf<String>()
        val pushedJobIds = mutableSetOf<String>()
        val pushedInvoiceIds = mutableSetOf<String>()

        try {
            val unsyncedCustomers = customerDao.getUnsynced()
            if (unsyncedCustomers.isNotEmpty()) {
                val dtos = unsyncedCustomers.mapNotNull { customer ->
                    runCatching { customer.toRemoteDto() }
                        .onFailure { android.util.Log.w("SyncRepository", "Skip customer ${customer.id}: ${it.message}") }
                        .getOrNull()
                }
                if (dtos.isNotEmpty()) {
                    client.from("customers").upsert(dtos)
                    unsyncedCustomers.forEach { customerDao.markSynced(it.id) }
                    pushedCustomers = dtos.size
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncRepository", "Customer push failed", e)
            warnings += "customer push: ${e.message ?: e.javaClass.simpleName}"
        }

        try {
            val unsyncedJobs = jobDao.getUnsynced()
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
                    pushedJobs = dtos.size
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncRepository", "Job push failed", e)
            warnings += "job push: ${e.message ?: e.javaClass.simpleName}"
        }

        try {
            val unsyncedInspections = inspectionDao.getUnsynced()
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
                    pushedInspections = dtos.size
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("SyncRepository", "Inspection push skipped", e)
            warnings += "inspection push: ${e.message ?: e.javaClass.simpleName}"
        }

        // --- Push invoices ---
        try {
            val unsyncedInvoices = invoiceDao.getUnsynced()
            if (unsyncedInvoices.isNotEmpty()) {
                val dtos = unsyncedInvoices.mapNotNull { invoice ->
                    runCatching { invoice.toRemoteDto() }
                        .onFailure { android.util.Log.w("SyncRepository", "Skip invoice ${invoice.id}: ${it.message}") }
                        .getOrNull()
                }
                if (dtos.isNotEmpty()) {
                    client.from("invoices").upsert(dtos)
                    pushedInvoiceIds += dtos.map { it.id }
                    dtos.forEach { invoiceDao.markSynced(it.id) }
                    pushedInvoices = dtos.size
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("SyncRepository", "Invoice push skipped", e)
            warnings += "invoice push: ${e.message ?: e.javaClass.simpleName}"
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

        // --- Pull invoices ---
        try {
            val remoteInvoices = client.from("invoices").select().decodeList<RemoteInvoiceDto>()
            if (remoteInvoices.isNotEmpty()) {
                val locals = remoteInvoices.mapNotNull { dto ->
                    runCatching {
                        val remote = dto.toLocal()
                        val existing = invoiceDao.getById(dto.id)

                        // Never replace a record just pushed in this same sync pass.
                        if (dto.id in pushedInvoiceIds) {
                            null
                        } else if (existing != null && !existing.isSynced) {
                            // Local unsynced edits always win until they are successfully pushed.
                            null
                        } else {
                            remote.copy(isSynced = true, syncError = null)
                        }
                    }.onFailure {
                        android.util.Log.w("SyncRepository", "Bad invoice ${dto.id}: ${it.message}")
                    }.getOrNull()
                }
                if (locals.isNotEmpty()) {
                    invoiceDao.insertAll(locals)
                    pulledInvoices = locals.size
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("SyncRepository", "Invoice pull failed", e)
            warnings += "invoice pull: ${e.message ?: e.javaClass.simpleName}"
        }

        val base = "Synced. Pushed: $pushedJobs jobs, $pushedCustomers customers, $pushedInspections inspections, $pushedInvoices invoices. " +
            "Pulled: $pulledJobs jobs, $pulledCustomers customers, $pulledInvoices invoices."
        val message = if (warnings.isEmpty()) base else "$base Warnings: ${warnings.joinToString("; ")}"

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
}
