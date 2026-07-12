package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.model.DefaultServiceTypes
import com.strobingn.wildlifefieldops.data.repository.ServiceTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun addType(label: String) = viewModelScope.launch {
        repository.addCustomType(label)
    }

    fun removeCustomType(label: String) = viewModelScope.launch {
        repository.removeCustomType(label)
    }
}
