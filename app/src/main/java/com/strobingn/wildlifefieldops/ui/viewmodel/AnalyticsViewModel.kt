package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.analytics.AnalyticsRepository
import com.strobingn.wildlifefieldops.analytics.AnalyticsSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    repository: AnalyticsRepository
) : ViewModel() {
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    val analytics: StateFlow<AnalyticsSnapshot> = repository.snapshot
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AnalyticsSnapshot()
        )
}
