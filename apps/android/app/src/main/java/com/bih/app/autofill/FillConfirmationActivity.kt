package com.bih.app.autofill

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.autofill.Dataset
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bih.app.ui.theme.BihTheme

// Confirmation screen required before ANY value is handed to a form
// (risk S11). Shows exactly which box gets which value; the user can
// back out with no data shared.
class FillConfirmationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ids: List<AutofillId> = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(EXTRA_IDS, AutofillId::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(EXTRA_IDS)
        } ?: run { finish(); return }
        val labels = intent.getStringArrayListExtra(EXTRA_LABELS) ?: run { finish(); return }
        val values = intent.getStringArrayListExtra(EXTRA_VALUES) ?: run { finish(); return }
        if (ids.isEmpty() || ids.size != labels.size || ids.size != values.size) {
            finish(); return
        }

        val rows = ids.indices.map { Triple(labels[it], values[it], it) }

        setContent {
            BihTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Fill this form?", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "B.I.H. will put your saved details into these boxes. Nothing else is shared.",
                            Modifier.padding(vertical = 8.dp),
                        )
                        LazyColumn(Modifier.weight(1f)) {
                            items(rows) { (label, value, _) ->
                                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(label, style = MaterialTheme.typography.titleMedium)
                                        Text(value)
                                    }
                                }
                            }
                        }
                        Row {
                            Button(onClick = { confirm(ids, values) }) { Text("Fill") }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { cancel() }) { Text("Not now") }
                        }
                    }
                }
            }
        }
    }

    private fun confirm(ids: List<AutofillId>, values: List<String>) {
        val builder = Dataset.Builder()
        ids.forEachIndexed { i, id ->
            builder.setValue(id, AutofillValue.forText(values[i]))
        }
        setResult(
            Activity.RESULT_OK,
            // Hands the filled dataset back to the autofill framework.
            Intent().putExtra(EXTRA_RESULT_DATASET, builder.build()),
        )
        finish()
    }

    private fun cancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        // Same value as AutofillManager.EXTRA_AUTHENTICATION_RESULT_DATASET.
        private const val EXTRA_RESULT_DATASET =
            "android.view.autofill.extra.AUTHENTICATION_RESULT_DATASET"
        private const val EXTRA_IDS = "bih.extra.IDS"
        private const val EXTRA_LABELS = "bih.extra.LABELS"
        private const val EXTRA_VALUES = "bih.extra.VALUES"

        fun buildIntent(context: Context, matches: List<BihAutofillService.Match>): Intent =
            Intent(context, FillConfirmationActivity::class.java).apply {
                putParcelableArrayListExtra(EXTRA_IDS, ArrayList(matches.map { it.id }))
                putStringArrayListExtra(EXTRA_LABELS, ArrayList(matches.map { it.label }))
                putStringArrayListExtra(EXTRA_VALUES, ArrayList(matches.map { it.value }))
            }
    }
}
