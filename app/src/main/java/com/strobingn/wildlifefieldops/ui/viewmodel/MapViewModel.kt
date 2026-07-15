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
    val type: String
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val jobDao: JobDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val boundaryPreferences =
        context.getSharedPreferences(BOUNDARY_PREFS, Context.MODE_PRIVATE)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedStatus = MutableStateFlow<JobStatus?>(null)
    val selectedStatus = _selectedStatus.asStateFlow()

    private val _isDrawingBoundary = MutableStateFlow(false)
    val isDrawingBoundary = _isDrawingBoundary.asStateFlow()

    private val _boundaryPoints = MutableStateFlow(loadSavedBoundary())
    val boundaryPoints = _boundaryPoints.asStateFlow()

    val properties: StateFlow<List<MapProperty>> = jobDao.getAll()
        .map { jobs ->
            jobs.filter { it.latitude != null && it.longitude != null }
                .map { job ->
                    MapProperty(
                        id = job.id,
                        name = job.title,
                        address = job.address,
                        latitude = job.latitude!!,
                        longitude = job.longitude!!,
                        status = job.status,
                        type = job.type
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
    }

    fun addBoundaryPoint(point: LatLng) {
        if (_isDrawingBoundary.value) {
            _boundaryPoints.value = _boundaryPoints.value + point
        }
    }

    fun clearBoundary() {
        _boundaryPoints.value = emptyList()
        _isDrawingBoundary.value = false
        boundaryPreferences.edit().remove(BOUNDARY_KEY).apply()
    }

    /**
     * Persists the current polygon locally. Returns false when fewer than three
     * points have been drawn because that cannot form a valid boundary.
     */
    fun saveBoundary(): Boolean {
        val points = _boundaryPoints.value
        if (points.size < 3) return false

        val encoded = points.joinToString(";") { point ->
            "${point.latitude},${point.longitude}"
        }
        boundaryPreferences.edit().putString(BOUNDARY_KEY, encoded).apply()
        _isDrawingBoundary.value = false
        return true
    }

    private fun loadSavedBoundary(): List<LatLng> {
        val encoded = boundaryPreferences.getString(BOUNDARY_KEY, null) ?: return emptyList()
        return encoded.split(';').mapNotNull { entry ->
            val values = entry.split(',')
            if (values.size != 2) return@mapNotNull null
            val latitude = values[0].toDoubleOrNull() ?: return@mapNotNull null
            val longitude = values[1].toDoubleOrNull() ?: return@mapNotNull null
            LatLng(latitude, longitude)
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

    private companion object {
        const val BOUNDARY_PREFS = "property_map_boundaries"
        const val BOUNDARY_KEY = "active_boundary_points"
    }
}
