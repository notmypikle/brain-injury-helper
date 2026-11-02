package com.bih.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bih.app.api.BihApi
import kotlinx.coroutines.CoroutineScope

/** Chat screen: big text field, one Send button, one mic button.
 *  Low cognitive load by design — one thing to do at a time.
 *  All state lives in [ChatViewModel] so nothing is lost on tab
 *  switches or screen rotation. */
@Composable
fun ChatScreen(
    api: BihApi,
    speakReplies: Boolean,
    state: ChatViewModel,
    scope: CoroutineScope,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) state.voice.startListening() }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages) { msg ->
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        text = (if (msg.fromUser) "You: " else "B.I.H.: ") + msg.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.input,
            onValueChange = { state.input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Type or tap the mic to talk") },
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    when {
                        state.listening -> state.voice.stopListening()
                        granted -> state.voice.startListening()
                        else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (state.listening) "Stop listening" else "🎙️ Talk")
            }
            Button(
                onClick = { state.send(api, speakReplies, scope) },
                enabled = !state.busy && state.input.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (state.busy) "Thinking…" else "Send")
            }
        }
    }
}
