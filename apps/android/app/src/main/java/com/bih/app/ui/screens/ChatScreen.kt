package com.bih.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bih.app.data.ApiClient
import kotlinx.coroutines.launch

private data class Message(val text: String, val fromUser: Boolean, val isSafety: Boolean = false)

@Composable
fun ChatScreen(api: ApiClient) {
    val messages = remember { mutableStateListOf<Message>() }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || busy) return
        input = ""
        busy = true
        messages.add(Message(text, fromUser = true))
        scope.launch {
            try {
                val resp = api.chat(text)
                messages.add(Message(resp.reply, fromUser = false, isSafety = resp.safety_flag))
            } catch (e: ApiClient.ApiException) {
                messages.add(Message(e.message ?: "Something went wrong.", fromUser = false))
            }
            busy = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                val bg = when {
                    msg.isSafety -> MaterialTheme.colorScheme.errorContainer
                    msg.fromUser -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Surface(
                    color = bg,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Text(msg.text, Modifier.padding(12.dp))
                }
            }
        }
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message…") },
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = ::send, enabled = !busy) { Text("Send") }
        }
    }
}
