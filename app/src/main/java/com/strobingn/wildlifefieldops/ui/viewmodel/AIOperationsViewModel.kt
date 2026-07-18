package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.ai.AIOperationsEngine
import com.strobingn.wildlifefieldops.ai.AIOperationsSnapshot
import com.strobingn.wildlifefieldops.ai.operations.AIOperationsEngine as QualityOperationsEngine
import com.strobingn.wildlifefieldops.data.local.InventoryItemDao
import com.strobingn.wildlifefieldops.data.local.JobDao
import com.strobingn.wildlifefieldops.data.remote.AiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIOperationsViewModel @Inject constructor(
    jobDao: JobDao,
    inventoryItemDao: InventoryItemDao,
    private val aiService: AiService
) : ViewModel() {

    val snapshot: StateFlow<AIOperationsSnapshot> = combine(
        jobDao.getAll(),
        inventoryItemDao.getAll()
    ) { jobs, inventory ->
        AIOperationsEngine.analyze(jobs, inventory)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AIOperationsSnapshot()
    )

    // Preserve and expose the original offline quality-control/pricing engine.
    // The new screen combines it with the richer risk/season/inventory snapshot.
    val qualityDashboard = jobDao.getAll()
        .map(QualityOperationsEngine::analyze)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = QualityOperationsEngine.analyze(emptyList())
        )

    val isLiveAiConfigured: Boolean
        get() = aiService.isConfigured

    val providerLabel: String
        get() = aiService.providerLabel

    private val _briefing = MutableStateFlow<String?>(null)
    val briefing = _briefing.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    fun generateBriefing() {
        if (_isGenerating.value) return
        _isGenerating.value = true
        viewModelScope.launch {
            _briefing.value = aiService.ask(snapshot.value.toPrivacySafePrompt())
            _isGenerating.value = false
        }
    }
}
