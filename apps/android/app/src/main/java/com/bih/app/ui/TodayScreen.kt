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
import com.bih.app.api.Reminder
import com.bih.app.api.ReminderIn
import com.bih.app.api.Routine
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Today screen: what's due, and routines to follow step by step. */
@Composable
fun TodayScreen(api: BihApi) {
    val scope = rememberCoroutineScope()
    var reminders by remember { mutableStateOf<List<Reminder>>(emptyList()) }
    var routines by remember { mutableStateOf<List<Routine>>(emptyList()) }
    var newReminder by remember { mutableStateOf("") }
    var offline by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            try {
                reminders = api.getReminders()
                routines = api.getRoutines()
                offline = false
            } catch (e: Exception) {
                offline = true
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Today", style = MaterialTheme.typography.headlineMedium)

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
            items(reminders, key = { it.id }) { reminder ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(reminder.text, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Due: ${reminder.due_at}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Button(onClick = {
                            scope.launch {
                                try {
                                    api.completeReminder(reminder.id)
                                    refresh()
                                } catch (e: Exception) {
                                    offline = true
                                }
                            }
                        }) { Text("Done") }
                    }
                }
            }

            item {
                Text(
                    "Routines",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            items(routines, key = { "routine-${it.id}" }) { routine ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(routine.name, style = MaterialTheme.typography.titleMedium)
                        routine.steps.forEachIndexed { i, step ->
                            Text("${i + 1}. $step", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = newReminder,
            onValueChange = { newReminder = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("New reminder (e.g. Take medication)") },
        )
        Button(
            onClick = {
                if (newReminder.isNotBlank()) {
                    scope.launch {
                        try {
                            // Default: due in 1 hour. Natural-language times
                            // ("tomorrow at 9") come with the chat assistant.
                            val due = LocalDateTime.now().plusHours(1)
                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            api.createReminder(ReminderIn(text = newReminder, due_at = due))
                            newReminder = ""
                            refresh()
                        } catch (e: Exception) {
                            offline = true
                        }
                    }
                }
            },
            enabled = newReminder.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Add reminder (due in 1 hour)")
        }
    }
}
