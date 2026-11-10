package com.bih.app.autofill

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bih.app.ui.theme.BihTheme

// Confirmation gate for the accessibility screen-fill (user's cross-app
// idea, risk S12). Shows every box + the value that would go into it, and
// asks before doing anything. "Not now" shares nothing and closes.
class ScreenFillConfirmationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val labels = intent.getStringArrayListExtra(EXTRA_LABELS) ?: run { finish(); return }
        val values = intent.getStringArrayListExtra(EXTRA_VALUES) ?: run { finish(); return }
        val hasSubmit = intent.getBooleanExtra(EXTRA_HAS_SUBMIT, false)
        if (labels.isEmpty() || labels.size != values.size) {
            finish(); return
        }
        val rows = labels.zip(values)

        setContent {
            BihTheme {
                Surface(Modifier.fillMaxSize()) {
                    var alsoTapSubmit by remember { mutableStateOf(false) }
                    Column(Modifier.padding(16.dp)) {
                        Text("Fill this screen?", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "B.I.H. will put your saved details into these boxes. " +
                                "Check them afterwards — you stay in control.",
                            Modifier.padding(vertical = 8.dp),
                        )
                        LazyColumn(Modifier.weight(1f)) {
                            items(rows) { (label, value) ->
                                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(label, style = MaterialTheme.typography.titleMedium)
                                        Text(value)
                                    }
                                }
                            }
                            if (hasSubmit) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 8.dp),
                                    ) {
                                        Checkbox(
                                            checked = alsoTapSubmit,
                                            onCheckedChange = { alsoTapSubmit = it },
                                        )
                                        Text("Also tap the Send / Submit button")
                                    }
                                }
                            }
                        }
                        Row {
                            Button(onClick = { confirm(alsoTapSubmit) }) { Text("Fill") }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { finish() }) { Text("Not now") }
                        }
                    }
                }
            }
        }
    }

    private fun confirm(includeSubmit: Boolean) {
        // Tell the service to do the fill. setPackage keeps this broadcast
        // private to our own app.
        sendBroadcast(
            Intent(BihAccessibilityService.ACTION_FILL_CONFIRMED)
                .setPackage(packageName)
                .putExtra(BihAccessibilityService.EXTRA_INCLUDE_SUBMIT, includeSubmit),
        )
        finish()
    }

    companion object {
        private const val EXTRA_LABELS = "bih.extra.LABELS"
        private const val EXTRA_VALUES = "bih.extra.VALUES"
        private const val EXTRA_HAS_SUBMIT = "bih.extra.HAS_SUBMIT"

        fun buildIntent(
            context: Context,
            labels: ArrayList<String>,
            values: ArrayList<String>,
            hasSubmit: Boolean,
        ): Intent = Intent(context, ScreenFillConfirmationActivity::class.java).apply {
            putStringArrayListExtra(EXTRA_LABELS, labels)
            putStringArrayListExtra(EXTRA_VALUES, values)
            putExtra(EXTRA_HAS_SUBMIT, hasSubmit)
        }
    }
}
