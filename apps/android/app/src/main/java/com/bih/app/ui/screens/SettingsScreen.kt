package com.bih.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.bih.app.data.SettingsStore

// Where the app finds the helper API.
// Emulator default (http://10.0.2.2:8000) is pre-filled.
@Composable
fun SettingsScreen(settings: SettingsStore) {
    var url by remember { mutableStateOf(settings.apiUrl) }
    var key by remember { mutableStateOf(settings.apiKey) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = url,
            onValueChange = { url = it; saved = false },
            label = { Text("API address") },
            supportingText = { Text("Example: http://10.0.2.2:8000 (emulator) or http://192.168.1.10:8000 (your computer)") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        OutlinedTextField(
            value = key,
            onValueChange = { key = it; saved = false },
            label = { Text("API key (if set on the server)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Button(
            onClick = {
                settings.apiUrl = url.trim()
                settings.apiKey = key.trim()
                saved = true
            },
            modifier = Modifier.padding(top = 16.dp),
        ) { Text("Save") }

        if (saved) {
            Text("Saved.", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
