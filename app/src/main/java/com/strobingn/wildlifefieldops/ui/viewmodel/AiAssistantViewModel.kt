package com.strobingn.wildlifefieldops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.data.remote.AiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val aiService: AiService
) : ViewModel() {

    private val welcomeMessage = buildString {
        append("Hello! I'm your Wildlife FieldOps AI assistant.\n\n")
        append("I can help with species ID, safety protocols, equipment, estimates, and field plans.")
        if (aiService.isConfigured) {
            append("\n\n✅ Live AI connected.")
        } else {
            append("\n\n⚠️ AI not connected.")
            append("\n\nTo enable live AI, add LLM_API_KEY to your environment variables and rebuild:")
            append("\n• xAI (Grok): LLM_API_KEY=your-xai-key")
            append("\n• OpenAI: LLM_API_KEY=your-openai-key LLM_BASE_URL=https://api.openai.com/v1")
            append("\n\nOr use the Supabase edge function path if you have that set up.")
        }
    }

    private val _messages = MutableStateFlow(listOf(ChatMessage(welcomeMessage, false)))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    fun send(userMessage: String) {
        val trimmed = userMessage.trim()
        if (trimmed.isEmpty() || _isTyping.value) return
        _messages.value = _messages.value + ChatMessage(trimmed, true)
        _isTyping.value = true
        viewModelScope.launch {
            // Always try direct LLM first. Fallback to Supabase edge only if LLM not configured.
            val reply = if (aiService.isConfigured) {
                aiService.ask(trimmed)
            } else {
                // Try Supabase as fallback
                aiService.askViaSupabase(trimmed)
            }
            _messages.value = _messages.value + ChatMessage(reply, false)
            _isTyping.value = false
        }
    }
}
