package com.bih.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bih.app.api.BihApi
import com.bih.app.api.Note
import com.bih.app.api.NoteIn
import kotlinx.coroutines.launch

/** Notes screen: the memory journal. Save anything, find it later. */
@Composable
fun NotesScreen(api: BihApi) {
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var newNote by remember { mutableStateOf("") }
    var offline by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            try {
                notes = api.getNotes()
                offline = false
            } catch (e: Exception) {
                offline = true
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Notes", style = MaterialTheme.typography.headlineMedium)

        if (offline) {
            Text(
                "Can't reach the B.I.H. service. Check the API address in Settings.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(notes, key = { it.id }) { note ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(note.text, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                note.created_at,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Button(onClick = {
                            scope.launch {
                                try {
                                    api.deleteNote(note.id)
                                    refresh()
                                } catch (e: Exception) {
                                    offline = true
                                }
                            }
                        }) { Text("Delete") }
                    }
                }
            }
        }

        OutlinedTextField(
            value = newNote,
            onValueChange = { newNote = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Something to remember…") },
        )
        Button(
            onClick = {
                if (newNote.isNotBlank()) {
                    scope.launch {
                        try {
                            api.createNote(NoteIn(text = newNote))
                            newNote = ""
                            refresh()
                        } catch (e: Exception) {
                            offline = true
                        }
                    }
                }
            },
            enabled = newNote.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Save note")
        }
    }
}
