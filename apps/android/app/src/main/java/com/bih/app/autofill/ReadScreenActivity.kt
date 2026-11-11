package com.bih.app.autofill

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bih.app.data.ApiClient
import com.bih.app.data.SettingsStore
import com.bih.app.ui.theme.BihTheme
import kotlinx.coroutines.launch
import java.util.Locale

// "Read this screen" — rebuilt from first real use (user, 2026-11-11):
// "I can't interact with what it's reading. No pause, no save, no
// repeat this section. Stuff like that."
//
// The text is split into SECTIONS (one per line) so the user stays in
// control: play/pause, back/next section, repeat, tap any section to
// start reading there, and save the whole thing to Notes for later.
// The section being read is highlighted and kept scrolled into view —
// following along with eyes + ears together helps comprehension.
class ReadScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent.getStringExtra(EXTRA_SCREEN_TEXT)
        if (text.isNullOrBlank()) {
            finish(); return
        }
        val content: String = text
        val sections = content.split("\n").filter { it.isNotBlank() }

        setContent {
            BihTheme {
                Surface(Modifier.fillMaxSize()) {
                    var speaking by remember { mutableStateOf(false) }
                    var paused by remember { mutableStateOf(false) }
                    var currentIndex by remember { mutableIntStateOf(0) }
                    var notice by remember { mutableStateOf<String?>(null) }
                    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
                    val listState = rememberLazyListState()
                    val scope = rememberCoroutineScope()
                    val api = remember { ApiClient(SettingsStore(this@ReadScreenActivity)) }

                    fun speakSection(i: Int) {
                        val engine = tts ?: return
                        // TTS accepts ~4000 chars per call — split long
                        // sections at word boundaries. The LAST chunk
                        // carries the section id so we know when the
                        // whole section has finished.
                        val chunks = mutableListOf<String>()
                        var rest = sections[i]
                        while (rest.length > 3500) {
                            val cut = rest.lastIndexOf(' ', 3500).takeIf { it > 0 } ?: 3500
                            chunks.add(rest.take(cut))
                            rest = rest.drop(cut).trimStart()
                        }
                        chunks.add(rest)
                        chunks.forEachIndexed { idx, chunk ->
                            engine.speak(
                                chunk,
                                if (idx == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                                null,
                                if (idx == chunks.size - 1) "sec-$i" else "part-$i-$idx",
                            )
                        }
                    }

                    DisposableEffect(Unit) {
                        var engine: TextToSpeech? = null
                        engine = TextToSpeech(this@ReadScreenActivity) { status ->
                            if (status == TextToSpeech.SUCCESS) {
                                engine?.setLanguage(Locale.getDefault())
                            }
                        }
                        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(id: String?) {}
                            override fun onError(id: String?) {
                                this@ReadScreenActivity.runOnUiThread { speaking = false }
                            }
                            override fun onDone(id: String?) {
                                // Only "sec-N" ids advance; chunk ids are ignored.
                                val done = id?.removePrefix("sec-")?.toIntOrNull() ?: return
                                this@ReadScreenActivity.runOnUiThread {
                                    if (paused) return@runOnUiThread
                                    if (done + 1 < sections.size) {
                                        currentIndex = done + 1
                                        speakSection(done + 1)
                                    } else {
                                        speaking = false // reached the end
                                    }
                                }
                            }
                        })
                        tts = engine
                        onDispose {
                            tts = null
                            engine.shutdown()
                        }
                    }

                    // Keep the section being read on screen.
                    LaunchedEffect(currentIndex, speaking) {
                        if (speaking && sections.isNotEmpty()) {
                            listState.animateScrollToItem(currentIndex)
                        }
                    }

                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("What's on your screen", style = MaterialTheme.typography.titleLarge)
                        notice?.let {
                            Text(it, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                            itemsIndexed(sections) { i, section ->
                                val isCurrent = i == currentIndex
                                Surface(
                                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                        .clickable {
                                            // Tap any paragraph to jump there.
                                            currentIndex = i
                                            if (speaking) speakSection(i)
                                        },
                                ) {
                                    Text(
                                        section,
                                        style = if (isCurrent) MaterialTheme.typography.titleMedium
                                                else MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(8.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (speaking) {
                                    paused = true; speaking = false; tts?.stop()
                                } else {
                                    paused = false; speaking = true; speakSection(currentIndex)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) { Text(if (speaking) "⏸ Pause" else "▶️ Read to me") }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (currentIndex > 0) {
                                        currentIndex -= 1
                                        if (speaking) speakSection(currentIndex)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("⏮ Back") }
                            OutlinedButton(
                                onClick = {
                                    // Repeat the current section, then carry on.
                                    paused = false; speaking = true; speakSection(currentIndex)
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("🔁 Repeat") }
                            OutlinedButton(
                                onClick = {
                                    if (currentIndex < sections.size - 1) {
                                        currentIndex += 1
                                        if (speaking) speakSection(currentIndex)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("⏭ Next") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            api.addNote("Read from screen:\n$content")
                                            notice = "Saved to your notes 📝"
                                        } catch (e: ApiClient.ApiException) {
                                            notice = e.message
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("💾 Save to notes") }
                            OutlinedButton(
                                onClick = { finish() },
                                modifier = Modifier.weight(1f),
                            ) { Text("Close") }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_SCREEN_TEXT = "bih.extra.READ_SCREEN_TEXT"

        fun buildIntent(context: Context, screenText: String): Intent =
            Intent(context, ReadScreenActivity::class.java).apply {
                putExtra(EXTRA_SCREEN_TEXT, screenText)
            }
    }
}
