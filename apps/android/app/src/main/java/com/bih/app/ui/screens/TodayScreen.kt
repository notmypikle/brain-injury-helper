package com.bih.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bih.app.data.ApiClient
import com.bih.app.data.Reminder
import com.bih.app.data.Routine
import kotlinx.coroutines.launch

// "Today" answers one question: what do I need to do right now?
// Shows unfinished reminders, then routine checklists.
@Composable
fun TodayScreen(api: ApiClient) {
    var reminders by remember { mutableStateOf<List<Reminder>>(emptyList()) }
    var routines by remember { mutableStateOf<List<Routine>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            try {
                reminders = api.listReminders()
                routines = api.listRoutines()
                error = null
            } catch (e: ApiClient.ApiException) {
                error = e.message
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Today", style = MaterialTheme.typography.titleLarge) }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }

        item { Text("Reminders", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)) }
        if (reminders.isEmpty()) {
            item { Text("Nothing due. Nice and calm.", Modifier.padding(vertical = 8.dp)) }
        }
        items(reminders, key = { it.id }) { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(r.text)
                    Text("Due: ${r.due_at}", style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = {
                    scope.launch {
                        try { api.completeReminder(r.id); refresh() }
                        catch (e: ApiClient.ApiException) { error = e.message }
                    }
                }) { Text("Done") }
            }
        }

        item { Text("Routines", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)) }
        items(routines, key = { it.id }) { routine ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(routine.name, style = MaterialTheme.typography.titleMedium)
                    routine.steps.forEachIndexed { i, step ->
                        Text("${i + 1}. $step", Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}
