package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.local.VoiceNoteDao
import com.strobingn.wildlifefieldops.data.model.VoiceNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceNotesViewModel @Inject constructor(
    private val voiceNoteDao: VoiceNoteDao
) : ViewModel() {

    val notes: StateFlow<List<VoiceNote>> = voiceNoteDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addDemoNote(jobId: String? = null) {
        viewModelScope.launch {
            val note = VoiceNote(
                jobId = jobId,
                title = "Field note ${System.currentTimeMillis() % 1000}",
                localPath = "/demo/voice_${System.currentTimeMillis()}.m4a",
                durationMs = 12_000 + (System.currentTimeMillis() % 30_000),
                transcript = "Demo voice note recorded on site. Check traps and seal entry points."
            )
            voiceNoteDao.insert(note)
        }
    }

    fun delete(note: VoiceNote) {
        viewModelScope.launch {
            voiceNoteDao.delete(note)
        }
    }
}
