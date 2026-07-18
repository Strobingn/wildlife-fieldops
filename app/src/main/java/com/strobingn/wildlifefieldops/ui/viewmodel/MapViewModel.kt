package com.strobingn.wildlifefieldops.ui.viewmodel

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.model.JobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

data class MapProperty(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val status: JobStatus,
    val type: String,
    val riskScore: Int
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val jobDao: JobDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedStatus = MutableStateFlow<JobStatus?>(null)
    val selectedStatus = _selectedStatus.asStateFlow()

    private val _isDrawingBoundary = MutableStateFlow(false)
    val isDrawingBoundary = _isDrawingBoundary.asStateFlow()

    private val _boundaryPoints = MutableStateFlow<List<LatLng>>(emptyList())
    val boundaryPoints = _boundaryPoints.asStateFlow()

    val properties: StateFlow<List<MapProperty>> = jobDao.getAll()
        .map { jobs ->
            val addressCounts = jobs
                .filter { it.address.isNotBlank() }
                .groupingBy { it.address.trim().lowercase() }
                .eachCount()
            jobs.filter { it.latitude != null && it.longitude != null }
                .map { job ->
                    val text = "${job.title} ${job.type} ${job.description} ${job.notes}".lowercase()
                    val activeWeight = when (job.status) {
                        JobStatus.PENDING -> 18
                        JobStatus.IN_PROGRESS -> 25
                        JobStatus.INVOICED -> 8
                        else -> 0
                    }
                    val repeatWeight = ((addressCounts[job.address.trim().lowercase()] ?: 1) - 1) * 12
                    val damageWeight = if (listOf("damage", "entry", "hole", "attic", "roof", "guano", "dropping")
                            .any { text.contains(it) }
                    ) 18 else 0
                    MapProperty(
                        id = job.id,
                        name = job.title,
                        address = job.address,
                        latitude = job.latitude!!,
                        longitude = job.longitude!!,
                        status = job.status,
                        type = job.type,
                        riskScore = (activeWeight + repeatWeight + damageWeight).coerceIn(0, 100)
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val missingCoordinateCount: StateFlow<Int> = jobDao.getAll()
        .map { jobs -> jobs.count { it.address.isNotBlank() && (it.latitude == null || it.longitude == null) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val filteredProperties = combine(properties, _searchQuery, _selectedStatus) { props, query, status ->
        props.filter { property ->
            val matchesQuery = query.isBlank() ||
                property.name.contains(query, ignoreCase = true) ||
                property.address.contains(query, ignoreCase = true) ||
                property.type.contains(query, ignoreCase = true)
            val matchesStatus = status == null || property.status == status
            matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        backfillMissingCoordinates()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: JobStatus?) {
        _selectedStatus.value = status
    }

    fun toggleDrawingMode() {
        _isDrawingBoundary.value = !_isDrawingBoundary.value
        if (!_isDrawingBoundary.value) {
            _boundaryPoints.value = emptyList()
        }
    }

    fun addBoundaryPoint(point: LatLng) {
        if (_isDrawingBoundary.value) {
            _boundaryPoints.value = _boundaryPoints.value + point
        }
    }

    fun clearBoundary() {
        _boundaryPoints.value = emptyList()
        _isDrawingBoundary.value = false
    }

    fun saveBoundary() {
        viewModelScope.launch {
            _isDrawingBoundary.value = false
            _boundaryPoints.value = emptyList()
        }
    }

    private fun backfillMissingCoordinates() {
        viewModelScope.launch {
            jobDao.getAll().first()
                .filter { job ->
                    job.address.isNotBlank() && (job.latitude == null || job.longitude == null)
                }
                .forEach { job ->
                    val coordinates = geocodeAddress(job.address) ?: return@forEach
                    jobDao.update(
                        job.copy(
                            latitude = coordinates.first,
                            longitude = coordinates.second,
                            updatedAt = System.currentTimeMillis(),
                            isSynced = false
                        )
                    )
                }
        }
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
