package com.bih.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.dp
import com.bih.app.data.SignaturePoint
import com.bih.app.data.SignatureStore
import com.bih.app.data.SignatureStroke

// USER'S IDEA (Nov 2026): "e-signature for paperwork."
// Draw with a finger, save, done. The signature is stored encrypted on the
// phone only (SignatureStore). When an online form asks you to sign, most
// sites accept a typed full name — B.I.H.'s fill feature does that for you.
// Stamping this drawn signature into exported PDFs is a planned Phase 4
// follow-up (see docs/PROJECT_STATUS.md).
@Composable
fun SignatureScreen(store: SignatureStore, onBack: () -> Unit) {
    var strokes by remember { mutableStateOf(store.load()) }
    var current by remember { mutableStateOf(listOf<Offset>()) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("My signature", style = MaterialTheme.typography.titleLarge)
        Text(
            "Draw your signature with your finger. It stays encrypted on " +
                "this phone and is never uploaded.",
            Modifier.padding(vertical = 8.dp),
        )

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.outline)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start -> current = listOf(start) },
                        onDrag = { change, _ ->
                            change.consume()
                            current = current + change.position
                        },
                        onDragEnd = {
                            if (current.size > 1) {
                                strokes = strokes + SignatureStroke(
                                    current.map { SignaturePoint(it.x, it.y) },
                                )
                            }
                            current = emptyList()
                            saved = false
                        },
                    )
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                fun drawStroke(points: List<Offset>) {
                    if (points.size > 1) {
                        drawPoints(points, PointMode.Polygon, Color.Black, strokeWidth = 6f)
                    }
                }
                strokes.forEach { s -> drawStroke(s.points.map { Offset(it.x, it.y) }) }
                drawStroke(current)
            }
        }

        if (saved) {
            Text("Saved.", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        }
        Row(Modifier.padding(top = 8.dp)) {
            Button(onClick = { store.save(strokes); saved = true }) { Text("Save signature") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { strokes = emptyList(); store.clear(); saved = false }) {
                Text("Clear")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }
    }
}
