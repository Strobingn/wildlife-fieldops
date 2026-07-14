package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.model.JobStatus
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val jobDao: JobDao
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
        .map { jobs -> jobs.count { it.latitude == null || it.longitude == null } }
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
}
