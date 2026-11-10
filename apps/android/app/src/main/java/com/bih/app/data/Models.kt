package com.bih.app.data

import kotlinx.serialization.Serializable

// These mirror the response models in services/api/app/main.py.

@Serializable
data class ChatRequest(val message: String)

@Serializable
data class ChatResponse(val reply: String, val safety_flag: Boolean = false)

@Serializable
data class CalmSteps(val steps: List<String>)

@Serializable
data class NoteIn(val text: String)

@Serializable
data class Note(val id: Int, val text: String, val created_at: String)

@Serializable
data class ReminderIn(val text: String, val due_at: String)

@Serializable
data class Reminder(
    val id: Int,
    val text: String,
    val due_at: String,
    val done: Boolean,
    val created_at: String,
)

@Serializable
data class RoutineIn(val name: String, val steps: List<String>)

@Serializable
data class Routine(val id: Int, val name: String, val steps: List<String>, val created_at: String)

// --- Paperwork wizard ---

@Serializable
data class ProfileUpdate(val fields: Map<String, String>)

@Serializable
data class TemplateField(val key: String, val label: String, val auto: Boolean)

@Serializable
data class PaperworkTemplate(
    val id: String,
    val title: String,
    val description: String,
    val fields: List<TemplateField>,
)

@Serializable
data class FillRequest(val template_id: String, val values: Map<String, String>)

@Serializable
data class PaperworkDocument(
    val id: Int,
    val template_id: String,
    val title: String,
    val content: String,
    val missing_fields: List<String>,
    val created_at: String,
)

// --- E-signature (user's idea, Nov 2026) ---
// A drawn signature is stored as strokes (lists of points), not an image
// file — smaller, simpler, and encrypted the same way as the profile.

@Serializable
data class SignaturePoint(val x: Float, val y: Float)

@Serializable
data class SignatureStroke(val points: List<SignaturePoint>)
