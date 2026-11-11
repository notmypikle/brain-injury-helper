package com.bih.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bih.app.data.ApiClient
import com.bih.app.data.Note
import kotlinx.coroutines.launch

// Memory journal: save things you want to remember, find them later.
@Composable
fun NotesScreen(api: ApiClient) {
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            try { notes = api.listNotes(); error = null }
            catch (e: ApiClient.ApiException) { error = e.message }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Notes", style = MaterialTheme.typography.titleLarge)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Something to remember…") },
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val text = input.trim()
                if (text.isEmpty()) return@Button
                scope.launch {
                    try { api.addNote(text); input = ""; refresh() }
                    catch (e: ApiClient.ApiException) { error = e.message }
                }
            }) { Text("Save") }
        }

        LazyColumn {
            items(notes, key = { it.id }) { note ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            if (note.folder.isNotBlank()) {
                                Text(
                                    "📁 ${note.folder}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(note.text)
                            Text(note.created_at, style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(onClick = {
                            scope.launch {
                                try { api.deleteNote(note.id); refresh() }
                                catch (e: ApiClient.ApiException) { error = e.message }
                            }
                        }) { Text("Delete") }
                    }
                }
            }
        }
    }
}
