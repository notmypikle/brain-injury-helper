package com.bih.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bih.app.api.BihApiClient
import com.bih.app.api.PaperworkResult
import com.bih.app.auth.BiometricGate
import com.bih.app.auth.PinStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppContent(activity = this)
                }
            }
        }
    }
}

private enum class AuthStage {
    SET_PIN,
    UNLOCK_OPTIONS,
    ENTER_PIN,
    UNLOCKED,
}

private enum class WorkMode {
    CHAT,
    PAPERWORK,
    CALL_SUPPORT,
}

@Composable
private fun AppContent(activity: MainActivity) {
    val context = LocalContext.current
    val pinStore = remember { PinStore(context) }
    val thoughtStore = remember { ThoughtStore(context) }
    val biometric = remember {
        BiometricGate(context, activity, ContextCompat.getMainExecutor(context))
    }
    var stage by remember { mutableStateOf(if (pinStore.hasPin()) AuthStage.UNLOCK_OPTIONS else AuthStage.SET_PIN) }
    var info by remember { mutableStateOf("Keep your PIN somewhere safe.") }

    when (stage) {
        AuthStage.SET_PIN -> SetPinScreen(
            onSavePin = { pin ->
                pinStore.savePin(pin)
                info = "PIN saved. You can now unlock."
                stage = AuthStage.UNLOCK_OPTIONS
            },
            info = info,
        )

        AuthStage.UNLOCK_OPTIONS -> UnlockOptionsScreen(
            canUseBiometric = biometric.isAvailable(),
            onUseBiometric = {
                biometric.prompt(
                    title = "Unlock B.I.H.",
                    subtitle = "Use fingerprint or face",
                    onSuccess = {
                        info = "Unlocked."
                        stage = AuthStage.UNLOCKED
                    },
                    onError = { err ->
                        info = err
                        stage = AuthStage.ENTER_PIN
                    },
                )
            },
            onUsePin = { stage = AuthStage.ENTER_PIN },
            info = info,
        )

        AuthStage.ENTER_PIN -> EnterPinScreen(
            onUnlock = { pin ->
                if (pinStore.verifyPin(pin)) {
                    info = "Unlocked."
                    stage = AuthStage.UNLOCKED
                } else {
                    info = "That PIN did not match. Please try again."
                }
            },
            onBack = { stage = AuthStage.UNLOCK_OPTIONS },
            info = info,
        )

        AuthStage.UNLOCKED -> HomeScreen(thoughtStore = thoughtStore)
    }
}

@Composable
private fun SetPinScreen(
    onSavePin: (String) -> Unit,
    info: String,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val pinOk = pin.length == 6 && pin.all { it.isDigit() }
    val matches = pin == confirm && pin.isNotBlank()

    ScreenFrame(title = "Set backup PIN", subtitle = "Use 6 digits you can remember.") {
        PinField(label = "New 6-digit PIN", value = pin, onValueChange = { pin = it.take(6) })
        PinField(label = "Confirm PIN", value = confirm, onValueChange = { confirm = it.take(6) })
        Text(text = info)
        Button(
            onClick = { onSavePin(pin) },
            enabled = pinOk && matches,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save PIN")
        }
    }
}

@Composable
private fun UnlockOptionsScreen(
    canUseBiometric: Boolean,
    onUseBiometric: () -> Unit,
    onUsePin: () -> Unit,
    info: String,
) {
    ScreenFrame(title = "Unlock B.I.H.", subtitle = "Pick the easiest safe option.") {
        if (canUseBiometric) {
            Button(onClick = onUseBiometric, modifier = Modifier.fillMaxWidth()) {
                Text("Use fingerprint/face")
            }
        }
        Button(onClick = onUsePin, modifier = Modifier.fillMaxWidth()) {
            Text("Use backup PIN")
        }
        Text(text = info)
    }
}

@Composable
private fun EnterPinScreen(
    onUnlock: (String) -> Unit,
    onBack: () -> Unit,
    info: String,
) {
    var pin by remember { mutableStateOf("") }
    val valid = pin.length == 6 && pin.all { it.isDigit() }

    ScreenFrame(title = "Enter backup PIN", subtitle = "6 digits.") {
        PinField(label = "PIN", value = pin, onValueChange = { pin = it.take(6) })
        Button(
            onClick = { onUnlock(pin) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Unlock")
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
        Text(text = info)
    }
}

@Composable
private fun HomeScreen(thoughtStore: ThoughtStore) {
    val context = LocalContext.current
    val client = remember { BihApiClient(BuildConfig.API_BASE_URL) }
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(WorkMode.CHAT) }
    var input by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var callTopic by remember { mutableStateOf("") }
    var callDetails by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready.") }
    var output by remember { mutableStateOf("") }
    var lastAutoSaved by remember { mutableStateOf("") }
    val speechLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            status = "Voice capture canceled."
            return@rememberLauncherForActivityResult
        }
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (text.isBlank()) {
            status = "No voice text captured."
            return@rememberLauncherForActivityResult
        }
        if (mode == WorkMode.CALL_SUPPORT) {
            callDetails = text
            thoughtStore.save("$callTopic\n$callDetails")
            lastAutoSaved = "$callTopic\n$callDetails"
            status = "Saved your spoken call notes."
        } else {
            input = text
            thoughtStore.save(text)
            lastAutoSaved = text
            status = "Saved your spoken thought."
        }
    }

    LaunchedEffect(input) {
        if (mode == WorkMode.CALL_SUPPORT) return@LaunchedEffect
        if (input.isBlank() || input == lastAutoSaved) return@LaunchedEffect
        delay(1200)
        if (input.isBlank() || input == lastAutoSaved) return@LaunchedEffect
        thoughtStore.save(input)
        lastAutoSaved = input
        if (!status.startsWith("Working")) {
            status = "Auto-saved thought."
        }

        LaunchedEffect(callTopic, callDetails, mode) {
            if (mode != WorkMode.CALL_SUPPORT) return@LaunchedEffect
            val packed = "$callTopic\n$callDetails"
            if (packed.isBlank() || packed == lastAutoSaved) return@LaunchedEffect
            delay(1200)
            val latest = "$callTopic\n$callDetails"
            if (latest.isBlank() || latest == lastAutoSaved) return@LaunchedEffect
            thoughtStore.save(latest)
            lastAutoSaved = latest
            if (!status.startsWith("Working")) {
                status = "Auto-saved call notes."
            }
        }
    }

    ScreenFrame(title = "B.I.H. Home", subtitle = "Connected to API") {
        Text("Server: ${BuildConfig.API_BASE_URL}")
        RowButtons(
            leftText = "Chat mode",
            rightText = "Paperwork mode",
            onLeft = { mode = WorkMode.CHAT },
            onRight = { mode = WorkMode.PAPERWORK },
        )
        Button(onClick = { mode = WorkMode.CALL_SUPPORT }, modifier = Modifier.fillMaxWidth()) {
            Text("Call support mode")
        }
        if (mode == WorkMode.CALL_SUPPORT) {
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone number") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = callTopic,
                onValueChange = { callTopic = it },
                label = { Text("Call topic (meds, housing, etc.)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = callDetails,
                onValueChange = { callDetails = it },
                label = { Text("Details you want help saying") },
                modifier = Modifier.fillMaxWidth(),
            )
            RowButtons(
                leftText = "Build call script",
                rightText = "Open phone dialer",
                onLeft = {
                    if (callTopic.isBlank() && callDetails.isBlank()) {
                        status = "Add topic or details first."
                        return@RowButtons
                    }
                    scope.launch {
                        status = "Building script..."
                        try {
                            val prompt = """
                                Make a short call script for someone with brain injury.
                                Keep plain language and short lines.
                                Return:
                                1) Opening line
                                2) Key points to say
                                3) Questions to ask
                                4) Closing line

                                Topic: $callTopic
                                Details: $callDetails
                            """.trimIndent()
                            val reply = withContext(Dispatchers.IO) { client.chat(prompt) }
                            output = reply
                            status = "Script ready."
                        } catch (e: Exception) {
                            status = "Error: ${e.message ?: "Failed to build script"}"
                        }
                    }
                },
                onRight = {
                    val number = phoneNumber.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
                    if (number.isBlank()) {
                        status = "Add a phone number first."
                        return@RowButtons
                    }
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                    try {
                        val launch = Intent.createChooser(dialIntent, "Open call app")
                        context.startActivity(launch)
                        status = "Dialer opened. Tap Call when ready."
                    } catch (_: Exception) {
                        status = "No dialer app available."
                    }
                },
            )
            RowButtons(
                leftText = "Hold my thought",
                rightText = "Restore thought",
                onLeft = {
                    thoughtStore.save("$callTopic\n$callDetails")
                    lastAutoSaved = "$callTopic\n$callDetails"
                    status = "Saved your call notes."
                },
                onRight = {
                    val restored = thoughtStore.load()
                    if (restored.isBlank()) {
                        status = "No saved thought yet."
                    } else {
                        val parts = restored.split("\n", limit = 2)
                        callTopic = parts.firstOrNull().orEmpty()
                        callDetails = parts.getOrNull(1).orEmpty()
                        status = "Restored your call notes."
                    }
                },
            )
        } else {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = {
                    Text(if (mode == WorkMode.CHAT) "Type message" else "Paste paperwork text")
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (mode != WorkMode.CALL_SUPPORT) {
            RowButtons(
                leftText = "Hold my thought",
                rightText = "Restore thought",
                onLeft = {
                    thoughtStore.save(input)
                    lastAutoSaved = input
                    status = "Saved your thought."
                },
                onRight = {
                    input = thoughtStore.load()
                    lastAutoSaved = input
                    status = if (input.isBlank()) "No saved thought yet." else "Restored your thought."
                },
            )
        }
        Button(
            onClick = {
                if (mode == WorkMode.CALL_SUPPORT) {
                    status = "Use Build call script and Open phone dialer above."
                    return@Button
                }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your thought")
                }
                try {
                    speechLauncher.launch(intent)
                } catch (_: Exception) {
                    status = "Voice capture is not available on this device."
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Talk and save thought")
        }
        Button(
            onClick = {
                if (mode == WorkMode.CALL_SUPPORT) {
                    status = "Call support is ready above."
                    return@Button
                }
                if (input.isBlank()) {
                    status = "Please add text first."
                    return@Button
                }
                scope.launch {
                    status = "Working..."
                    try {
                        if (mode == WorkMode.CHAT) {
                            val reply = withContext(Dispatchers.IO) { client.chat(input) }
                            output = reply
                        } else {
                            val result = withContext(Dispatchers.IO) { client.analyzePaperwork(input) }
                            output = formatPaperwork(result)
                        }
                        status = "Done."
                    } catch (e: Exception) {
                        status = "Error: ${e.message ?: "Request failed"}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                when (mode) {
                    WorkMode.CHAT -> "Send chat"
                    WorkMode.PAPERWORK -> "Analyze paperwork"
                    WorkMode.CALL_SUPPORT -> "Call support ready"
                },
            )
        }
        Text("Status: $status")
        OutlinedTextField(
            value = output,
            onValueChange = {},
            readOnly = true,
            label = { Text("Result") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
        )
    }
}

private fun formatPaperwork(result: PaperworkResult): String {
    val actions = if (result.keyActions.isEmpty()) "- none" else result.keyActions.joinToString("\n") { "- $it" }
    val deadlines = if (result.deadlines.isEmpty()) "- none" else result.deadlines.joinToString("\n") { "- $it" }
    val questions = if (result.questionsToAsk.isEmpty()) "- none" else result.questionsToAsk.joinToString("\n") { "- $it" }
    return buildString {
        appendLine("Summary: ${result.plainSummary}")
        appendLine()
        appendLine("Actions:")
        appendLine(actions)
        appendLine()
        appendLine("Deadlines:")
        appendLine(deadlines)
        appendLine()
        appendLine("Questions to ask:")
        appendLine(questions)
        appendLine()
        append("Safety note: ${result.safetyNote}")
    }
}

@Composable
private fun RowButtons(
    leftText: String,
    rightText: String,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onLeft, modifier = Modifier.fillMaxWidth()) { Text(leftText) }
        Button(onClick = onRight, modifier = Modifier.fillMaxWidth()) { Text(rightText) }
    }
}

@Composable
private fun PinField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            val digits = newValue.filter { it.isDigit() }
            onValueChange(digits)
        },
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ScreenFrame(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(text = subtitle, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

private class ThoughtStore(context: Context) {
    private val prefs = context.getSharedPreferences("bih_thoughts", Context.MODE_PRIVATE)

    fun save(text: String) {
        prefs.edit().putString("last_thought", text).apply()
    }

    fun load(): String = prefs.getString("last_thought", "") ?: ""
}
