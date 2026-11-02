package com.bih.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.bih.app.api.BihApi
import com.bih.app.api.ChatRequest
import com.bih.app.voice.VoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class ChatMessage(val fromUser: Boolean, val text: String)

/**
 * Chat state + voice live in a ViewModel so the conversation, an in-flight
 * reply, and ongoing speech survive tab switches AND configuration changes
 * (rotation, theme change) — important for users with memory difficulties
 * who may check another screen mid-conversation.
 */
class ChatViewModel(app: Application) : AndroidViewModel(app) {
    val messages = mutableStateListOf<ChatMessage>()
    var input by mutableStateOf("")
    var busy by mutableStateOf(false)
    var listening by mutableStateOf(false)

    val voice = VoiceManager(
        context = app,
        onTranscript = { input = it },
        onListeningChanged = { listening = it },
        onError = { messages.add(ChatMessage(false, it)) },
    )

    fun send(api: BihApi, speakReplies: Boolean, scope: CoroutineScope) {
        val text = input.trim()
        if (text.isEmpty() || busy) return
        messages.add(ChatMessage(true, text))
        input = ""
        busy = true
        scope.launch {
            try {
                val response = api.chat(ChatRequest(message = text))
                messages.add(ChatMessage(false, response.reply))
                if (speakReplies) voice.speak(response.reply)
            } catch (e: Exception) {
                messages.add(
                    ChatMessage(
                        false,
                        "I couldn't reach my brain right now. " +
                            "Check the API address in Settings, then try again.",
                    )
                )
            } finally {
                busy = false
            }
        }
    }

    override fun onCleared() {
        voice.destroy()
        super.onCleared()
    }
}
