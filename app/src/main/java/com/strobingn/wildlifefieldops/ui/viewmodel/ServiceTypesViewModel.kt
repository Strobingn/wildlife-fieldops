package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.model.DefaultServiceTypes
import com.strobingn.wildlifefieldops.data.repository.ServiceTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServiceTypesViewModel @Inject constructor(
    private val repository: ServiceTypeRepository
) : ViewModel() {

    val allTypes: StateFlow<List<String>> = repository.allTypes
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DefaultServiceTypes.all
        )

    val customTypes: StateFlow<List<String>> = repository.customTypes
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage.asStateFlow()

    fun addType(label: String) = viewModelScope.launch {
        if (repository.addCustomType(label)) {
            _lastMessage.value = "Added “${DefaultServiceTypes.normalize(label)}”"
        }
    }

    /**
     * Delete a custom service type. Jobs using it are reassigned to [reassignTo]
     * (default: Inspection) so nothing is left with a deleted label.
     */
    fun removeCustomType(
        label: String,
        reassignTo: String = DefaultServiceTypes.all.first()
    ) = viewModelScope.launch {
        if (repository.isBuiltIn(label)) {
            _lastMessage.value = "Built-in services can’t be deleted"
            return@launch
        }
        val jobsTouched = repository.removeCustomType(label, reassignJobsTo = reassignTo)
        _lastMessage.value = if (jobsTouched > 0) {
            "Deleted “$label” and updated $jobsTouched job(s) to “$reassignTo”"
        } else {
            "Deleted “$label”"
        }
    }

    fun clearMessage() {
        _lastMessage.value = null
    }

    fun isBuiltIn(label: String): Boolean = repository.isBuiltIn(label)
}
