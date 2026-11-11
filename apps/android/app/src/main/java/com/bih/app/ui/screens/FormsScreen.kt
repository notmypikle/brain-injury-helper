package com.bih.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bih.app.data.ApiClient
import com.bih.app.data.PaperworkChecklist
import com.bih.app.data.PaperworkDocument
import com.bih.app.data.PaperworkTemplate
import kotlinx.coroutines.launch

// Standard "My details" keys — anything saved here auto-fills matching
// form fields, so the user only ever types it once.
private val DETAIL_FIELDS = listOf(
    "full_name" to "Full name",
    "dob" to "Date of birth",
    "address" to "Home address",
    "phone" to "Phone number",
    "email" to "Email address",
    "doctor_name" to "Doctor's name",
    "doctor_phone" to "Doctor's phone",
    "emergency_contact_name" to "Emergency contact name",
    "emergency_contact_phone" to "Emergency contact phone",
    "allergies" to "Allergies",
    "conditions" to "Medical conditions",
    "medications" to "Current medications",
)

// The paperwork wizard. Three views in one screen, switched by [mode]:
// list of forms + saved documents, filling one form, viewing one document.
@Composable
fun FormsScreen(api: ApiClient, onOpenSignature: () -> Unit = {}) {
    var profile by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var templates by remember { mutableStateOf<List<PaperworkTemplate>>(emptyList()) }
    var documents by remember { mutableStateOf<List<PaperworkDocument>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var filling by remember { mutableStateOf<PaperworkTemplate?>(null) }
    var viewing by remember { mutableStateOf<PaperworkDocument?>(null) }
    var checklists by remember { mutableStateOf<List<PaperworkChecklist>>(emptyList()) }
    var viewingChecklist by remember { mutableStateOf<PaperworkChecklist?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            try {
                profile = api.getProfile()
                templates = api.getTemplates()
                documents = api.listDocuments()
                checklists = api.getChecklists()
                error = null
            } catch (e: ApiClient.ApiException) {
                error = e.message
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    when {
        filling != null -> FillForm(
            template = filling!!,
            profile = profile,
            onDone = { doc ->
                filling = null
                viewing = doc
                refresh()
            },
            onCancel = { filling = null },
            api = api,
        )
        viewing != null -> DocumentView(doc = viewing!!, onClose = { viewing = null })
        viewingChecklist != null -> ChecklistView(
            checklist = viewingChecklist!!, onClose = { viewingChecklist = null },
        )
        else -> FormsList(
            templates = templates,
            checklists = checklists,
            documents = documents,
            profile = profile,
            error = error,
            onPick = { filling = it },
            onView = { viewing = it },
            onDelete = { id ->
                scope.launch {
                    try { api.deleteDocument(id); refresh() }
                    catch (e: ApiClient.ApiException) { error = e.message }
                }
            },
            onSaveProfile = { fields ->
                scope.launch {
                    try { profile = api.putProfile(fields) }
                    catch (e: ApiClient.ApiException) { error = e.message }
                }
            },
            onOpenSignature = onOpenSignature,
            onPickChecklist = { viewingChecklist = it },
        )
    }
}

@Composable
private fun FormsList(
    templates: List<PaperworkTemplate>,
    checklists: List<PaperworkChecklist>,
    documents: List<PaperworkDocument>,
    profile: Map<String, String>,
    error: String?,
    onPick: (PaperworkTemplate) -> Unit,
    onView: (PaperworkDocument) -> Unit,
    onDelete: (Int) -> Unit,
    onSaveProfile: (Map<String, String>) -> Unit,
    onOpenSignature: () -> Unit,
    onPickChecklist: (PaperworkChecklist) -> Unit = {},
) {
    var showDetails by remember { mutableStateOf(false) }
    // Editable copies of the profile fields.
    val edits = remember { mutableStateMapOf<String, String>() }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Paperwork", style = MaterialTheme.typography.titleLarge)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text(
                "Pick a form. Anything saved in My details is filled in for you. " +
                    "Then copy each line into the online form, one box at a time.",
                Modifier.padding(vertical = 8.dp),
            )
        }

        item {
            Row {
                TextButton(onClick = {
                    if (!showDetails) {
                        edits.clear()
                        DETAIL_FIELDS.forEach { (k, _) -> edits[k] = profile[k] ?: "" }
                    }
                    showDetails = !showDetails
                }) { Text(if (showDetails) "Close My details" else "My details (auto-fill)") }
                // User's e-signature idea: draw once, reuse for paperwork.
                TextButton(onClick = onOpenSignature) { Text("My signature") }
            }
        }

        if (showDetails) {
            item {
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        DETAIL_FIELDS.forEach { (key, label) ->
                            OutlinedTextField(
                                value = edits[key] ?: "",
                                onValueChange = { edits[key] = it },
                                label = { Text(label) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            )
                        }
                        Button(
                            onClick = {
                                onSaveProfile(edits.toMap().filterValues { it.isNotBlank() })
                                showDetails = false
                            },
                            modifier = Modifier.padding(top = 8.dp),
                        ) { Text("Save details") }
                    }
                }
            }
        }

        // "What form do I need, and what must I gather?" (2026-11-11)
        item { Text("Which form? What to gather?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)) }
        items(checklists, key = { it.id }) { cl ->
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                onClick = { onPickChecklist(cl) },
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(cl.title, style = MaterialTheme.typography.titleSmall)
                    Text("${cl.gathers.size} things to gather · tap to see",
                         style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text(
                "Tip: save copies of your verification papers in the Notes tab " +
                    "under a 'Verification documents' folder — everything in one place.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        item { Text("Forms", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)) }
        items(templates, key = { it.id }) { t ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(t.title, style = MaterialTheme.typography.titleMedium)
                    Text(t.description, Modifier.padding(top = 4.dp))
                    Button(onClick = { onPick(t) }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Start")
                    }
                }
            }
        }

        item { Text("Finished documents", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)) }
        if (documents.isEmpty()) {
            item { Text("None yet.", Modifier.padding(vertical = 8.dp)) }
        }
        items(documents, key = { it.id }) { doc ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(doc.title)
                        Text(doc.created_at, style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { onView(doc) }) { Text("View") }
                    TextButton(onClick = { onDelete(doc.id) }) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun FillForm(
    template: PaperworkTemplate,
    profile: Map<String, String>,
    onDone: (PaperworkDocument) -> Unit,
    onCancel: () -> Unit,
    api: ApiClient,
) {
    // Pre-fill auto fields from the profile so the user only answers what's new.
    val answers = remember {
        mutableStateMapOf<String, String>().apply {
            template.fields.forEach { f ->
                put(f.key, if (f.auto) profile[f.key] ?: "" else "")
            }
        }
    }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(template.title, style = MaterialTheme.typography.titleLarge)
            Text(
                "One question at a time. Leave a box empty if you don't know it yet.",
                Modifier.padding(vertical = 8.dp),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        items(template.fields, key = { it.key }) { field ->
            OutlinedTextField(
                value = answers[field.key] ?: "",
                onValueChange = { answers[field.key] = it },
                label = { Text(field.label) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                minLines = if (field.label.length > 30) 2 else 1,
            )
        }
        item {
            Row(Modifier.padding(top = 8.dp)) {
                Button(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            try {
                                onDone(api.fillPaperwork(template.id, answers.toMap()))
                            } catch (e: ApiClient.ApiException) {
                                error = e.message
                                busy = false
                            }
                        }
                    },
                ) { Text(if (busy) "Working…" else "Finish document") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onCancel) { Text("Back") }
            }
        }
    }
}

@Composable
private fun DocumentView(doc: PaperworkDocument, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(doc.title, style = MaterialTheme.typography.titleLarge)
        if (doc.missing_fields.isNotEmpty()) {
            Text(
                "Still needed: ${doc.missing_fields.joinToString()}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            "Long-press to select and copy each line into the online form.",
            Modifier.padding(vertical = 8.dp),
        )
        Card(Modifier.weight(1f).fillMaxWidth()) {
            SelectionContainer {
                Text(doc.content, Modifier.padding(12.dp))
            }
        }
        Button(onClick = onClose, modifier = Modifier.padding(top = 8.dp)) { Text("Done") }
    }
}


@Composable
private fun ChecklistView(checklist: PaperworkChecklist, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(checklist.title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f)) {
            item { Text("The form to ask for:", style = MaterialTheme.typography.titleSmall) }
            items(checklist.forms) { f -> Text("• $f", Modifier.padding(vertical = 2.dp)) }
            item {
                Spacer(Modifier.height(8.dp))
                Text("Gather first:", style = MaterialTheme.typography.titleSmall)
            }
            items(checklist.gathers) { g -> Text("☐ $g", Modifier.padding(vertical = 2.dp)) }
            item {
                Spacer(Modifier.height(8.dp))
                Text(checklist.note, style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
