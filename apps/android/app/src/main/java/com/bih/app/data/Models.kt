package com.bih.app.data

import kotlinx.serialization.Serializable

// These mirror the response models in services/api/app/main.py.

@Serializable
data class ChatRequest(val message: String)

@Serializable
data class ChatResponse(val reply: String, val safety_flag: Boolean = false)

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
