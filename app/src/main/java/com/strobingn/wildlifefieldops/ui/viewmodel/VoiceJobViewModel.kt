package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.remote.AiService
import com.strobingn.wildlifefieldops.data.remote.VoiceJobDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceJobViewModel @Inject constructor(
    private val aiService: AiService
) : ViewModel() {

    val isConfigured: Boolean get() = aiService.isConfigured
    val providerLabel: String get() = aiService.providerLabel

    private val _parsing = MutableStateFlow(false)
    val parsing = _parsing.asStateFlow()

    private val _draft = MutableStateFlow<VoiceJobDraft?>(null)
    val draft = _draft.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun parse(transcript: String) {
        if (_parsing.value || transcript.isBlank()) return
        _parsing.value = true
        _message.value = null
        viewModelScope.launch {
            _draft.value = aiService.parseVoiceJob(transcript)
            _parsing.value = false
            if (_draft.value?.fromAi != true) {
                _message.value = "AI parse unavailable — transcript kept in notes, fill fields before saving."
            }
        }
    }

    fun clearDraft() { _draft.value = null }

    fun setMessage(text: String?) { _message.value = text }
}
