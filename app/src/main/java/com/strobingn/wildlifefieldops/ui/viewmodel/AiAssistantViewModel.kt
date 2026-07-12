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
        append("Hello — I'm your Wildlife FieldOps AI (SpaceXAI / Grok).\n\n")
        append("Ask about inspections, jobs, trapping, exclusion, safety, estimates, customers, or daily workflow.\n")
        if (aiService.isConfigured) {
            append("\n✅ Live AI connected via ${aiService.providerLabel}.")
        } else {
            append("\n⚠️ Live AI not connected yet — offline field tips still work.\n")
            append("\nTo enable SpaceXAI:")
            append("\n1. Create a key at https://console.x.ai")
            append("\n2. Add GitHub secret XAI_API_KEY (or LLM_API_KEY)")
            append("\n3. Rebuild the APK (defaults: api.x.ai · grok-4.5)")
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
            val reply = if (aiService.isConfigured) {
                aiService.ask(trimmed)
            } else {
                // Optional Supabase edge; otherwise ask() returns SpaceXAI setup help.
                val edge = runCatching { aiService.askViaSupabase(trimmed) }.getOrNull()
                if (!edge.isNullOrBlank() &&
                    !edge.startsWith("⚠️") &&
                    !edge.startsWith("Supabase") &&
                    !edge.startsWith("Network")
                ) {
                    edge
                } else {
                    aiService.ask(trimmed)
                }
            }
            _messages.value = _messages.value + ChatMessage(reply, false)
            _isTyping.value = false
        }
    }
}
