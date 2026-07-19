package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.PendingOperationDao
import com.strobingn.wildlifefieldops.data.model.PendingOperation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncQueueViewModel @Inject constructor(
    private val pendingOperationDao: PendingOperationDao
) : ViewModel() {

    val operations = pendingOperationDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount = pendingOperationDao.getCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isLoading = MutableStateFlow(true)

    init {
        operations.onEach { isLoading.value = false }.launchIn(viewModelScope)
    }

    fun retryOperation(operation: PendingOperation) = viewModelScope.launch {
        pendingOperationDao.update(operation.copy(
            retryCount = 0,
            lastError = "",
            isProcessing = false
        ))
    }

    fun deleteOperation(operation: PendingOperation) = viewModelScope.launch {
        pendingOperationDao.delete(operation)
    }

    fun clearAll() = viewModelScope.launch {
        pendingOperationDao.deleteAll()
    }

    fun resetProcessing() = viewModelScope.launch {
        pendingOperationDao.resetProcessing()
    }
}
