package com.bih.app.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bih.app.data.ApiClient
import java.util.Locale

// Bundled copy of the API's calm steps (services/api/app/calm.py) so the
// "Too much" screen ALWAYS works — even if the API is down, the phone
// just rebooted, or Termux hasn't started yet. Keep the two in sync.
private val FALLBACK_STEPS = listOf(
    "It's okay. Everything can wait for a moment.",
    "Breathe in slowly through your nose — 1, 2, 3, 4.",
    "And out gently through your mouth — 1, 2, 3, 4, 5, 6.",
    "Feel your feet on the floor. Press down gently.",
    "Look around. Name one thing you can see.",
    "Name one sound you can hear — or enjoy the quiet for a moment.",
    "Let your shoulders drop. Soften your jaw and hands.",
    "This feeling will pass. You're doing well.",
    "When you're ready: pick just ONE small thing to do next. Only one.",
)

// Calm mode (user's core accessibility need, 2026-11-10): one short
// step at a time, big text, one big button. Nothing else on screen.
// Read-aloud is OFF by default — extra sound can add to the overload.
@Composable
fun CalmScreen(api: ApiClient, onBack: () -> Unit) {
    var steps by remember { mutableStateOf(FALLBACK_STEPS) }
    var index by remember { mutableStateOf(0) }
    var readAloud by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Refresh steps from the API only before the user has started, so a
    // late reply can never change the step mid-grounding.
    LaunchedEffect(Unit) {
        try {
            val fetched = api.getCalmSteps()
            if (fetched.isNotEmpty() && index == 0) steps = fetched
        } catch (e: ApiClient.ApiException) {
            // Stay silent — the bundled steps are already on screen.
        }
    }

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

    LaunchedEffect(index, readAloud, steps) {
        if (readAloud) {
            tts?.speak(steps[index], TextToSpeech.QUEUE_FLUSH, null, "bih-calm")
        }
    }

    val lastStep = index == steps.size - 1

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            steps[index],
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Step ${index + 1} of ${steps.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { if (lastStep) onBack() else index += 1 },
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Text(if (lastStep) "All done 💙" else "Next →")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("I'm okay — go back")
        }
        TextButton(onClick = {
            readAloud = !readAloud
            if (!readAloud) tts?.stop()
        }) {
            Text(if (readAloud) "Stop reading aloud" else "Read steps aloud")
        }
    }
}
