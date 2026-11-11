package com.bih.app.autofill

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bih.app.ui.theme.BihTheme

// Shown when the user taps the system accessibility button. One simple
// choice, big buttons (low cognitive load):
//   📖 Read this screen — show its text big, optionally read aloud
//   ✍️ Fill boxes      — the existing confirmed screen-fill flow
// Only the choices that make sense for the current screen are shown.
class AssistMenuActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screenText = intent.getStringExtra(EXTRA_SCREEN_TEXT).orEmpty()
        val labels = intent.getStringArrayListExtra(EXTRA_LABELS) ?: arrayListOf()
        val values = intent.getStringArrayListExtra(EXTRA_VALUES) ?: arrayListOf()
        val hasSubmit = intent.getBooleanExtra(EXTRA_HAS_SUBMIT, false)

        setContent {
            BihTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "What would you like?",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(24.dp))
                        if (screenText.isNotBlank()) {
                            Button(
                                onClick = {
                                    startActivity(
                                        ReadScreenActivity.buildIntent(
                                            this@AssistMenuActivity, screenText,
                                        ),
                                    )
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                            ) { Text("📖 Read this screen") }
                            Spacer(Modifier.height(12.dp))
                        }
                        if (labels.isNotEmpty()) {
                            Button(
                                onClick = {
                                    startActivity(
                                        ScreenFillConfirmationActivity.buildIntent(
                                            this@AssistMenuActivity, labels, values, hasSubmit,
                                        ),
                                    )
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                            ) { Text("✍️ Fill boxes on this screen") }
                            Spacer(Modifier.height(12.dp))
                        }
                        OutlinedButton(
                            onClick = { finish() },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Cancel") }
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_SCREEN_TEXT = "bih.extra.SCREEN_TEXT"
        private const val EXTRA_LABELS = "bih.extra.MENU_LABELS"
        private const val EXTRA_VALUES = "bih.extra.MENU_VALUES"
        private const val EXTRA_HAS_SUBMIT = "bih.extra.MENU_HAS_SUBMIT"

        fun buildIntent(
            context: Context,
            screenText: String,
            labels: ArrayList<String>,
            values: ArrayList<String>,
            hasSubmit: Boolean,
        ): Intent = Intent(context, AssistMenuActivity::class.java).apply {
            putExtra(EXTRA_SCREEN_TEXT, screenText)
            putStringArrayListExtra(EXTRA_LABELS, labels)
            putStringArrayListExtra(EXTRA_VALUES, values)
            putExtra(EXTRA_HAS_SUBMIT, hasSubmit)
        }
    }
}
