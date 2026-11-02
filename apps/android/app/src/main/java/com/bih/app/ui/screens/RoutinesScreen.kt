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
import com.bih.app.data.Routine
import kotlinx.coroutines.launch

// Step-by-step checklists, e.g. a morning routine. Add steps one per line.
@Composable
fun RoutinesScreen(api: ApiClient) {
    var routines by remember { mutableStateOf<List<Routine>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var stepsText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            try { routines = api.listRoutines(); error = null }
            catch (e: ApiClient.ApiException) { error = e.message }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Routines", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { showAdd = !showAdd }) { Text(if (showAdd) "Close" else "Add") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        if (showAdd) {
            item {
                Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Routine name") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = stepsText,
                            onValueChange = { stepsText = it },
                            label = { Text("Steps, one per line") },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            minLines = 3,
                        )
                        Button(
                            onClick = {
                                val steps = stepsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                                if (name.isBlank() || steps.isEmpty()) return@Button
                                scope.launch {
                                    try {
                                        api.addRoutine(name.trim(), steps)
                                        name = ""; stepsText = ""; showAdd = false
                                        refresh()
                                    } catch (e: ApiClient.ApiException) { error = e.message }
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp),
                        ) { Text("Save routine") }
                    }
                }
            }
        }

        items(routines, key = { it.id }) { routine ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(routine.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            scope.launch {
                                try { api.deleteRoutine(routine.id); refresh() }
                                catch (e: ApiClient.ApiException) { error = e.message }
                            }
                        }) { Text("Delete") }
                    }
                    routine.steps.forEachIndexed { i, step ->
                        Text("${i + 1}. $step", Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}
