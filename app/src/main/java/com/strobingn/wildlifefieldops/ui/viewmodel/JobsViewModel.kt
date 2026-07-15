package com.strobingn.wildlifefieldops.ui.viewmodel

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.model.DefaultServiceTypes
import com.strobingn.wildlifefieldops.data.model.Job
import com.strobingn.wildlifefieldops.data.model.JobPriority
import com.strobingn.wildlifefieldops.data.model.JobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class JobsViewModel @Inject constructor(
    private val jobDao: JobDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /** Writes must survive a navigation pop that clears the screen-scoped ViewModel. */
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedStatus = MutableStateFlow<JobStatus?>(null)
    val selectedStatus = _selectedStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    val jobs = combine(_searchQuery, _selectedStatus) { query, status -> query to status }
        .flatMapLatest { (query, status) ->
            when {
                query.isNotBlank() -> jobDao.search(query)
                status != null -> jobDao.getByStatus(status)
                else -> jobDao.getAll()
            }
        }
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingCount = jobDao.getByStatus(JobStatus.PENDING)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val inProgressCount = jobDao.getByStatus(JobStatus.IN_PROGRESS)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val completedCount = jobDao.getByStatus(JobStatus.COMPLETED)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val totalRevenue = jobDao.getByStatus(JobStatus.PAID)
        .map { records -> records.sumOf { it.actualCost } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setStatusFilter(status: JobStatus?) { _selectedStatus.value = status }

    fun getJobById(id: String): Flow<Job?> {
        if (id.isBlank() || id == "new") return flowOf(null)
        return jobDao.observeById(id)
    }

    suspend fun loadJobOnce(id: String): Job? {
        if (id.isBlank() || id == "new") return null
        return jobDao.getById(id)
    }

    fun saveJob(job: Job) = persistenceScope.launch { saveJobNow(job) }

    suspend fun saveJobNow(job: Job) {
        val coordinates = coordinatesFor(job.address, job.latitude, job.longitude)
        jobDao.insert(
            job.copy(
                latitude = coordinates?.first ?: job.latitude,
                longitude = coordinates?.second ?: job.longitude,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun updateJob(job: Job) = persistenceScope.launch { updateJobNow(job) }

    suspend fun updateJobNow(job: Job) {
        val coordinates = coordinatesFor(job.address, job.latitude, job.longitude)
        jobDao.insert(
            job.copy(
                latitude = coordinates?.first ?: job.latitude,
                longitude = coordinates?.second ?: job.longitude,
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
        )
    }

    fun updateJobDetails(
        jobId: String,
        title: String,
        description: String,
        customerId: String,
        customerName: String,
        address: String,
        type: String,
        priority: JobPriority,
        estimatedValue: Double,
        actualCost: Double? = null,
        notes: String,
        scheduledDate: Long? = null
    ) = persistenceScope.launch {
        updateJobDetailsNow(
            jobId, title, description, customerId, customerName, address, type,
            priority, estimatedValue, actualCost, notes, scheduledDate
        )
    }

    suspend fun updateJobDetailsNow(
        jobId: String,
        title: String,
        description: String,
        customerId: String,
        customerName: String,
        address: String,
        type: String,
        priority: JobPriority,
        estimatedValue: Double,
        actualCost: Double? = null,
        notes: String,
        scheduledDate: Long? = null
    ) {
        val existing = jobDao.getById(jobId) ?: error("Job not found")
        val normalizedAddress = address.trim()
        val addressChanged = !normalizedAddress.equals(existing.address.trim(), ignoreCase = true)
        val coordinates = if (addressChanged || existing.latitude == null || existing.longitude == null) {
            geocodeAddress(normalizedAddress)
        } else {
            existing.latitude?.let { lat -> existing.longitude?.let { lng -> lat to lng } }
        }

        jobDao.insert(
            existing.copy(
                title = title,
                description = description,
                customerId = customerId,
                customerName = customerName,
                address = normalizedAddress,
                latitude = coordinates?.first,
                longitude = coordinates?.second,
                type = DefaultServiceTypes.display(type),
                priority = priority,
                estimatedValue = estimatedValue,
                actualCost = actualCost ?: existing.actualCost,
                notes = notes,
                scheduledDate = scheduledDate ?: existing.scheduledDate,
                updatedAt = System.currentTimeMillis(),
                isSynced = false,
                syncError = null
            )
        )
    }

    fun deleteJob(job: Job) = persistenceScope.launch { jobDao.delete(job) }
    fun deleteJobById(id: String) = persistenceScope.launch { jobDao.deleteById(id) }

    fun updateJobStatus(jobId: String, status: JobStatus) = persistenceScope.launch {
        val job = jobDao.getById(jobId)
        job?.let {
            jobDao.update(
                it.copy(
                    status = status,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
            )
        }
    }

    fun createJob(
        title: String,
        description: String,
        customerId: String,
        customerName: String,
        address: String,
        type: String,
        priority: JobPriority,
        estimatedValue: Double,
        scheduledDate: Long?,
        notes: String,
        actualCost: Double = 0.0
    ) = persistenceScope.launch {
        createJobNow(
            title, description, customerId, customerName, address, type, priority,
            estimatedValue, scheduledDate, notes, actualCost
        )
    }

    suspend fun createJobNow(
        title: String,
        description: String,
        customerId: String,
        customerName: String,
        address: String,
        type: String,
        priority: JobPriority,
        estimatedValue: Double,
        scheduledDate: Long?,
        notes: String,
        actualCost: Double = 0.0
    ): Job {
        val normalizedAddress = address.trim()
        val coordinates = geocodeAddress(normalizedAddress)
        val job = Job(
            title = title,
            description = description,
            customerId = customerId,
            customerName = customerName,
            address = normalizedAddress,
            latitude = coordinates?.first,
            longitude = coordinates?.second,
            type = DefaultServiceTypes.display(type),
            priority = priority,
            estimatedValue = estimatedValue,
            actualCost = actualCost,
            scheduledDate = scheduledDate,
            notes = notes,
            isSynced = false,
            syncError = null
        )
        jobDao.insert(job)
        return job
    }

    private suspend fun coordinatesFor(
        address: String,
        latitude: Double?,
        longitude: Double?
    ): Pair<Double, Double>? {
        if (latitude != null && longitude != null) return latitude to longitude
        return geocodeAddress(address)
    }

    @Suppress("DEPRECATION")
    private suspend fun geocodeAddress(address: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (address.isBlank() || !Geocoder.isPresent()) return@withContext null
        runCatching {
            Geocoder(context, Locale.getDefault())
                .getFromLocationName(address, 1)
                ?.firstOrNull()
                ?.let { it.latitude to it.longitude }
        }.getOrNull()
    }
}
