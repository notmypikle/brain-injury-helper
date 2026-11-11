package com.bih.app.autofill

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bih.app.ui.theme.BihTheme
import java.util.Locale

// "Read this screen" (user request 2026-11-11: help understanding texts,
// emails and online documents). Shows the screen's text BIG and calm,
// away from the clutter of the original app, with optional read-aloud
// (off by default — the user chooses when sound helps).
//
// Privacy (S12/S15): the text came from the accessibility service, which
// only reads on the user's tap, never reads password fields, never
// stores or sends anything. This screen keeps it in memory only.
class ReadScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent.getStringExtra(EXTRA_SCREEN_TEXT)
        if (text.isNullOrBlank()) {
            finish(); return
        }

        setContent {
            BihTheme {
                Surface(Modifier.fillMaxSize()) {
                    var speaking by remember { mutableStateOf(false) }
                    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

                    DisposableEffect(Unit) {
                        var engine: TextToSpeech? = null
                        engine = TextToSpeech(this@ReadScreenActivity) { status ->
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

                    fun speakAll() {
                        // TTS accepts ~4000 chars per call — feed long
                        // screens in line-sized chunks.
                        val chunks = mutableListOf<String>()
                        var rest = text
                        while (rest.length > 3500) {
                            val cut = rest.lastIndexOf('\n', 3500).takeIf { it > 0 } ?: 3500
                            chunks.add(rest.take(cut))
                            rest = rest.drop(cut).trimStart('\n')
                        }
                        chunks.add(rest)
                        chunks.forEachIndexed { i, chunk ->
                            tts?.speak(
                                chunk,
                                if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                                null,
                                "bih-read-$i",
                            )
                        }
                    }

                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text(
                            "What's on your screen",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            Text(text, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (speaking) {
                                    tts?.stop()
                                    speaking = false
                                } else {
                                    speakAll()
                                    speaking = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) { Text(if (speaking) "⏹ Stop reading" else "🔊 Read aloud") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { finish() },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Close") }
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
