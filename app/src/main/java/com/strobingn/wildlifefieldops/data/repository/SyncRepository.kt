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
    val success: Boolean,
    val message: String,
    val pushedJobs: Int = 0,
    val pushedCustomers: Int = 0,
    val pushedInspections: Int = 0,
    val pushedInvoices: Int = 0,
    val pulledJobs: Int = 0,
    val pulledCustomers: Int = 0,
    val pulledInvoices: Int = 0
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
            SyncResult(false, "Sync failed: ${t.message ?: t.javaClass.simpleName}. Check connection and Supabase config.")
        }
    }

    private suspend fun doSync(): SyncResult {
        val client = supabaseService.client ?: return SyncResult(
            false,
            "Cloud not configured. Rebuild the APK with Supabase secrets."
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
            val unsynced = customerDao.getUnsynced()
            if (unsynced.isNotEmpty()) {
                val dtos = unsynced.mapNotNull { item -> runCatching { item.toRemoteDto() }.getOrNull() }
                if (dtos.isNotEmpty()) {
                    client.from("customers").upsert(dtos)
                    unsynced.forEach { customerDao.markSynced(it.id) }
                    pushedCustomers = dtos.size
                }
            }
        } catch (e: Exception) {
            warnings += "customer push: ${e.message ?: e.javaClass.simpleName}"
        }

        try {
            val unsynced = jobDao.getUnsynced()
            if (unsynced.isNotEmpty()) {
                val dtos = unsynced.mapNotNull { item -> runCatching { item.toRemoteDto() }.getOrNull() }
                if (dtos.isNotEmpty()) {
                    client.from("jobs").upsert(dtos)
                    pushedJobIds += dtos.map { it.id }
                    unsynced.filter { it.id in pushedJobIds }.forEach { jobDao.markSynced(it.id) }
                    pushedJobs = dtos.size
                }
            }
        } catch (e: Exception) {
            warnings += "job push: ${e.message ?: e.javaClass.simpleName}"
        }

        try {
            val unsynced = inspectionDao.getUnsynced()
            if (unsynced.isNotEmpty()) {
                val dtos = mutableListOf<RemoteInspectionDto>()
                val ids = mutableListOf<String>()
                unsynced.forEach { inspection ->
                    runCatching {
                        dtos += inspection.toRemoteDtoOrNull()
                        ids += inspection.id
                    }
                }
                if (dtos.isNotEmpty()) {
                    client.from("inspections").upsert(dtos)
                    ids.forEach { inspectionDao.markSynced(it) }
                    pushedInspections = dtos.size
                }
            }
        } catch (e: Exception) {
            warnings += "inspection push: ${e.message ?: e.javaClass.simpleName}"
        }

        try {
            val unsynced = invoiceDao.getUnsynced()
            if (unsynced.isNotEmpty()) {
                val dtos = unsynced.mapNotNull { invoice ->
                    runCatching { invoice.toRemoteDto() }
                        .onFailure { invoiceDao.setSyncError(invoice.id, it.message) }
                        .getOrNull()
                }
                if (dtos.isNotEmpty()) {
                    client.from("invoices").upsert(dtos)
                    pushedInvoiceIds += dtos.map { it.id }
                    unsynced.filter { it.id in pushedInvoiceIds }.forEach { invoiceDao.markSynced(it.id) }
                    pushedInvoices = dtos.size
                }
            }
        } catch (e: Exception) {
            warnings += "invoice push: ${e.message ?: e.javaClass.simpleName}"
        }

        try {
            val remote = client.from("customers").select().decodeList<RemoteCustomerDto>()
            val locals = remote.mapNotNull { runCatching { it.toLocal() }.getOrNull() }
            if (locals.isNotEmpty()) {
                customerDao.insertAll(locals)
                pulledCustomers = locals.size
            }
        } catch (e: Exception) {
            warnings += "customer pull: ${e.message ?: e.javaClass.simpleName}"
        }

        try {
            val remote = client.from("jobs").select().decodeList<RemoteJobDto>()
            val locals = remote.mapNotNull { dto ->
                runCatching {
                    val incoming = dto.toLocal()
                    val existing = jobDao.getById(dto.id)
                    when {
                        dto.id in pushedJobIds -> null
                        existing != null && !existing.isSynced -> null
                        else -> incoming.copy(
                            estimatedValue = if (incoming.estimatedValue != 0.0 || existing == null) incoming.estimatedValue else existing.estimatedValue,
                            actualCost = if (incoming.actualCost != 0.0 || existing == null) incoming.actualCost else existing.actualCost,
                            latitude = incoming.latitude ?: existing?.latitude,
                            longitude = incoming.longitude ?: existing?.longitude,
                            isSynced = true,
                            syncError = null
                        )
                    }
                }.getOrNull()
            }
            if (locals.isNotEmpty()) {
                jobDao.insertAll(locals)
                pulledJobs = locals.size
            }
        } catch (e: Exception) {
            warnings += "job pull: ${e.message ?: e.javaClass.simpleName}"
        }

        try {
            val remote = client.from("invoices").select().decodeList<RemoteInvoiceDto>()
            val locals = remote.mapNotNull { dto ->
                runCatching {
                    val existing = invoiceDao.getById(dto.id)
                    when {
                        dto.id in pushedInvoiceIds -> null
                        existing != null && !existing.isSynced -> null
                        else -> dto.toLocal()
                    }
                }.getOrNull()
            }
            if (locals.isNotEmpty()) {
                invoiceDao.insertAll(locals)
                pulledInvoices = locals.size
            }
        } catch (e: Exception) {
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
            pushedInvoices = pushedInvoices,
            pulledJobs = pulledJobs,
            pulledCustomers = pulledCustomers,
            pulledInvoices = pulledInvoices
        )
    }
}