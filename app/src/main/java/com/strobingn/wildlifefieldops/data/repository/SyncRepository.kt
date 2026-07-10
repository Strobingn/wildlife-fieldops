package com.strobingn.wildlifefieldops.data.repository

import com.strobingn.wildlifefieldops.data.local.CustomerDao
import com.strobingn.wildlifefieldops.data.local.InspectionDao
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.remote.RemoteCustomerDto
import com.strobingn.wildlifefieldops.data.remote.RemoteJobDto
import com.strobingn.wildlifefieldops.data.remote.SupabaseService
import com.strobingn.wildlifefieldops.data.remote.toLocal
import com.strobingn.wildlifefieldops.data.remote.toRemoteDto
import com.strobingn.wildlifefieldops.data.remote.toRemoteDtoOrNull
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

data class SyncResult(
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
    private val inspectionDao: InspectionDao
) {
    fun isCloudConfigured(): Boolean = supabaseService.isConfigured

    suspend fun syncAll(): SyncResult {
        val client = supabaseService.client
            ?: return SyncResult(
                success = false,
                message = "Cloud not configured. Rebuild the APK with Supabase secrets set."
            )

        var pushedJobs = 0
        var pushedCustomers = 0
        var pushedInspections = 0
        var pulledJobs = 0
        var pulledCustomers = 0

        return try {
            // Push local → cloud
            val unsyncedCustomers = customerDao.getUnsynced()
            if (unsyncedCustomers.isNotEmpty()) {
                val dtos = unsyncedCustomers.map { it.toRemoteDto() }
                client.from("customers").upsert(dtos)
                unsyncedCustomers.forEach { customerDao.markSynced(it.id) }
                pushedCustomers = dtos.size
            }

            val unsyncedJobs = jobDao.getUnsynced()
            if (unsyncedJobs.isNotEmpty()) {
                val dtos = unsyncedJobs.map { it.toRemoteDto() }
                client.from("jobs").upsert(dtos)
                unsyncedJobs.forEach { jobDao.markSynced(it.id) }
                pushedJobs = dtos.size
            }

            val unsyncedInspections = inspectionDao.getUnsynced()
            if (unsyncedInspections.isNotEmpty()) {
                try {
                    val pairs = unsyncedInspections.mapNotNull { insp ->
                        insp.toRemoteDtoOrNull()?.let { dto -> insp.id to dto }
                    }
                    if (pairs.isNotEmpty()) {
                        client.from("inspections").upsert(pairs.map { it.second })
                        pairs.forEach { inspectionDao.markSynced(it.first) }
                        pushedInspections = pairs.size
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncRepository", "Inspection push skipped: ${e.message}")
                }
            }

            // Pull cloud → local (merge)
            try {
                val remoteCustomers = client.from("customers").select().decodeList<RemoteCustomerDto>()
                if (remoteCustomers.isNotEmpty()) {
                    customerDao.insertAll(remoteCustomers.map { it.toLocal() })
                    pulledCustomers = remoteCustomers.size
                }
            } catch (e: Exception) {
                android.util.Log.w("SyncRepository", "Customer pull failed: ${e.message}")
            }

            try {
                val remoteJobs = client.from("jobs").select().decodeList<RemoteJobDto>()
                if (remoteJobs.isNotEmpty()) {
                    jobDao.insertAll(remoteJobs.map { it.toLocal() })
                    pulledJobs = remoteJobs.size
                }
            } catch (e: Exception) {
                android.util.Log.w("SyncRepository", "Job pull failed: ${e.message}")
            }

            SyncResult(
                success = true,
                message = "Synced. Pushed: $pushedJobs jobs, $pushedCustomers customers, $pushedInspections inspections. " +
                    "Pulled: $pulledJobs jobs, $pulledCustomers customers.",
                pushedJobs = pushedJobs,
                pushedCustomers = pushedCustomers,
                pushedInspections = pushedInspections,
                pulledJobs = pulledJobs,
                pulledCustomers = pulledCustomers
            )
        } catch (e: Exception) {
            android.util.Log.e("SyncRepository", "Sync failed", e)
            SyncResult(success = false, message = "Sync failed: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}
