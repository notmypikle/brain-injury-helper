package com.bih.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bih.app.data.Settings
import com.bih.app.data.SettingsRepository
import kotlinx.coroutines.launch

/** Settings: where the API lives, the API key, and voice preferences. */
@Composable
fun SettingsScreen(settingsRepo: SettingsRepository, current: Settings) {
    val scope = rememberCoroutineScope()
    var apiUrl by remember(current) { mutableStateOf(current.apiUrl) }
    var apiKey by remember(current) { mutableStateOf(current.apiKey) }
    var speakReplies by remember(current) { mutableStateOf(current.speakReplies) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = apiUrl,
            onValueChange = { apiUrl = it; saved = false },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            label = { Text("API address (e.g. http://192.168.1.10:8000)") },
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; saved = false },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text("API key (only if the server has one set)") },
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Speak replies aloud", Modifier.weight(1f))
            Switch(
                checked = speakReplies,
                onCheckedChange = { speakReplies = it; saved = false },
            )
        }

        Button(
            onClick = {
                scope.launch {
                    settingsRepo.save(
                        Settings(
                            apiUrl = apiUrl.trim(),
                            apiKey = apiKey.trim(),
                            speakReplies = speakReplies,
                        )
                    )
                    saved = true
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text("Save")
        }

        if (saved) {
            Text(
                "Saved ✓",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Text(
            "B.I.H. is not a medical provider and does not replace professional care.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}
