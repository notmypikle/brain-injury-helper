package com.bih.app.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

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
data class Routine(
    val id: Int,
    val name: String,
    val steps: List<String>,
    val created_at: String,
)

@Serializable
data class VoiceRequest(val transcript: String)

@Serializable
data class VoiceResponse(val reply_text: String, val safety_flag: Boolean = false)

/** Mirrors services/api/app/main.py — keep field names in sync with the API. */
interface BihApi {
    @POST("chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @POST("voice")
    suspend fun voice(@Body request: VoiceRequest): VoiceResponse

    @POST("notes")
    suspend fun createNote(@Body note: NoteIn): Note

    @GET("notes")
    suspend fun getNotes(): List<Note>

    @DELETE("notes/{id}")
    suspend fun deleteNote(@Path("id") id: Int)

    @POST("reminders")
    suspend fun createReminder(@Body reminder: ReminderIn): Reminder

    @GET("reminders")
    suspend fun getReminders(): List<Reminder>

    @POST("reminders/{id}/done")
    suspend fun completeReminder(@Path("id") id: Int): Reminder

    @DELETE("reminders/{id}")
    suspend fun deleteReminder(@Path("id") id: Int)

    @POST("routines")
    suspend fun createRoutine(@Body routine: RoutineIn): Routine

    @GET("routines")
    suspend fun getRoutines(): List<Routine>

    @DELETE("routines/{id}")
    suspend fun deleteRoutine(@Path("id") id: Int)
}
