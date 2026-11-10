package com.bih.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bih.app.data.ApiClient
import kotlinx.coroutines.launch
import java.util.Locale

private data class Message(val text: String, val fromUser: Boolean, val isSafety: Boolean = false)

// Voice runs 100% on-device (docs/VOICE_AND_PRODUCTION.md Part 1):
// - Mic uses Android's SpeechRecognizer; the transcript lands in the text
//   box first so the user can check it before sending (risk P5).
// - Replies are spoken with the device's TextToSpeech. The speaker button
//   mutes/unmutes. Text input is always equally available.
@Composable
fun ChatScreen(api: ApiClient) {
    val messages = remember { mutableStateListOf<Message>() }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // --- Text-to-speech (spoken replies) ---
    var speakReplies by remember { mutableStateOf(true) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.setLanguage(Locale.getDefault())
            }
        }
        tts = engine
        onDispose {
            tts = null
            engine.shutdown()
        }
    }

    fun speak(text: String) {
        if (speakReplies) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bih-reply")
        }
    }

    // --- Speech-to-text (mic) ---
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            // Text goes into the box for review — never sent automatically.
            if (!spoken.isNullOrBlank()) input = spoken
        }
    }

    fun launchMic() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now — you can check the words before sending")
            // Security review fix (Nov 2026): prefer ON-DEVICE speech
            // recognition so dictated audio (which can include medical
            // details) never leaves the phone.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        // If this device can't recognize speech on-device, say so before the
        // user dictates — their audio may go to the recognizer's server.
        if (Build.VERSION.SDK_INT >= 31 &&
            !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            notice = "Heads up: on this device, speech may be processed by the speech service online. Type instead if you prefer."
        }
        try {
            micLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            notice = "Speech recognition isn't available on this device."
        }
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) launchMic() }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || busy) return
        input = ""
        busy = true
        notice = null
        messages.add(Message(text, fromUser = true))
        scope.launch {
            try {
                val resp = api.chat(text)
                messages.add(Message(resp.reply, fromUser = false, isSafety = resp.safety_flag))
                speak(resp.reply)
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
        notice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) launchMic() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }) {
                Icon(Icons.Filled.Mic, contentDescription = "Talk")
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type or tap the mic…") },
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = ::send, enabled = !busy) { Text("Send") }
            IconButton(onClick = { speakReplies = !speakReplies }) {
                Icon(
                    if (speakReplies) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    contentDescription = if (speakReplies) "Mute spoken replies" else "Unmute spoken replies",
                )
            }
        }
    }
}
