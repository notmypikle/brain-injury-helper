package com.bih.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.bih.app.data.KeystoreCipher
import com.bih.app.notifications.ReminderScheduler
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

@Serializable
private data class Message(val text: String, val fromUser: Boolean, val isSafety: Boolean = false)

// Bug fix (reported by user 2026-11-10): the chat lived inside the screen,
// so switching tabs wiped it and reopening showed a blank page. ChatState
// now ALSO persists to encrypted on-device storage, so the conversation
// survives tab switches AND the app being closed/restarted.
private object ChatState {
    private const val KEY = "chat_history"
    private const val MAX_MESSAGES = 100
    val messages = mutableStateListOf<Message>()
    private var prefs: SharedPreferences? = null
    private var loaded = false

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        prefs = context.getSharedPreferences("bih_settings", Context.MODE_PRIVATE)
        prefs?.getString(KEY, null)
            ?.let { KeystoreCipher.decrypt(it) }
            ?.let { runCatching { Json.decodeFromString<List<Message>>(it) }.getOrNull() }
            ?.let { messages.addAll(it) }
    }

    fun save() {
        while (messages.size > MAX_MESSAGES) messages.removeAt(0)
        prefs?.edit()
            ?.putString(KEY, KeystoreCipher.encrypt(Json.encodeToString(messages.toList())))
            ?.apply()
    }
}

private val WELCOME = Message(
    "Hi, I'm here. 😊 Type below, or tap the mic to talk. " +
        "Whatever's on your mind — we'll take it one step at a time.",
    fromUser = false,
)

// Voice runs 100% on-device (docs/VOICE_AND_PRODUCTION.md Part 1):
// - Mic uses Android's SpeechRecognizer; the transcript lands in the text
//   box first so the user can check it before sending (risk P5).
// - Replies are spoken with the device's TextToSpeech. The speaker button
//   mutes/unmutes. Text input is always equally available.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(api: ApiClient, onOpenCalm: () -> Unit) {
    val messages = ChatState.messages
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    // User request 2026-11-11: delete SOME messages, not all-or-nothing.
    // Losing a train of thought is costly — surgical cleanup matters.
    var selectMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Int>() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    ChatState.init(context)

    // Greet on a fresh/empty chat so the page never looks blank.
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(WELCOME)
            ChatState.save()
        }
    }

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
        ChatState.save()
        scope.launch {
            try {
                val resp = api.chat(text)
                messages.add(Message(resp.reply, fromUser = false, isSafety = resp.safety_flag))
                speak(resp.reply)
                // Chat can create reminders — keep their alarms in step
                // right away, not just when the Today tab opens.
                try { ReminderScheduler.scheduleAll(context, api.listReminders()) }
                catch (e: Exception) { /* the Today tab refreshes them too */ }
            } catch (e: ApiClient.ApiException) {
                messages.add(Message(e.message ?: "Something went wrong.", fromUser = false))
            }
            ChatState.save()
            busy = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Core accessibility need (2026-11-10): one tap out of overload,
        // always visible at the top of the chat.
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onOpenCalm,
                modifier = Modifier.weight(1f).padding(bottom = 8.dp),
            ) {
                Text("😮‍💨 Too much? Tap here")
            }
            // User request (2026-11-11): a way to delete chat messages.
            IconButton(
                onClick = { showClearConfirm = true },
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear chat")
            }
        }
        if (selectMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${selected.size} selected", Modifier.weight(1f))
                TextButton(onClick = {
                    // Delete chosen messages, keep the rest (order kept).
                    val remaining = messages.filterIndexed { i, _ -> i !in selected }
                    messages.clear(); messages.addAll(remaining)
                    ChatState.save()
                    selectMode = false
                }, enabled = selected.isNotEmpty()) { Text("Delete selected") }
                TextButton(onClick = {
                    // Keep only the chosen ones — the important ones stay.
                    val kept = messages.filterIndexed { i, _ -> i in selected }
                    messages.clear(); messages.addAll(kept)
                    ChatState.save()
                    selectMode = false
                }, enabled = selected.isNotEmpty()) { Text("Keep only these") }
                TextButton(onClick = { selectMode = false; selected.clear() }) { Text("Cancel") }
            }
        }
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Clear chat?") },
                text = { Text("This deletes every message in this chat. It can't be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        messages.clear()
                        messages.add(WELCOME)
                        ChatState.save()
                        showClearConfirm = false
                    }) { Text("Delete all") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) { Text("Keep") }
                },
            )
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            items(messages.size) { i ->
                val msg = messages[i]
                val bg = when {
                    selectMode && i in selected -> MaterialTheme.colorScheme.tertiaryContainer
                    msg.isSafety -> MaterialTheme.colorScheme.errorContainer
                    msg.fromUser -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Surface(
                    color = bg,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .combinedClickable(
                            onClick = {
                                if (selectMode) {
                                    if (i in selected) selected.remove(i) else selected.add(i)
                                }
                            },
                            onLongClick = {
                                selectMode = true
                                if (i !in selected) selected.add(i)
                            },
                        ),
                ) {
                    Text(
                        (if (selectMode && i in selected) "✔️ " else "") + msg.text,
                        Modifier.padding(12.dp),
                    )
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
