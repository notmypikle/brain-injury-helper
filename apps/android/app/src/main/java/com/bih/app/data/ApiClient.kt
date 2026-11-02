package com.bih.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// Thin wrapper around the BIH API. Every call runs off the main thread.
// Errors throw ApiException with a short, calm message for the UI.
class ApiClient(private val settings: SettingsStore) {

    class ApiException(message: String) : Exception(message)

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonType = "application/json".toMediaType()

    private fun buildRequest(path: String, method: String, body: String? = null): Request {
        val builder = Request.Builder()
            .url(settings.apiUrl.trimEnd('/') + path)
            .method(method, body?.toRequestBody(jsonType))
        if (settings.apiKey.isNotBlank()) {
            builder.header("X-API-Key", settings.apiKey)
        }
        return builder.build()
    }

    private suspend fun <T> call(
        path: String,
        method: String = "GET",
        body: String? = null,
        parse: (String) -> T,
    ): T = withContext(Dispatchers.IO) {
        try {
            http.newCall(buildRequest(path, method, body)).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                when {
                    resp.code == 401 -> throw ApiException("Wrong or missing API key. Check Settings.")
                    resp.code == 404 -> throw ApiException("That item was not found.")
                    !resp.isSuccessful -> throw ApiException("The helper had a problem (code ${resp.code}).")
                    else -> parse(text)
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException("Can't reach the helper. Is the API running? Check Settings.")
        }
    }

    suspend fun chat(message: String): ChatResponse =
        call("/chat", "POST", json.encodeToString(ChatRequest.serializer(), ChatRequest(message))) {
            json.decodeFromString(ChatResponse.serializer(), it)
        }

    suspend fun listNotes(): List<Note> = call("/notes") { json.decodeFromString(it) }

    suspend fun addNote(text: String): Note =
        call("/notes", "POST", json.encodeToString(NoteIn.serializer(), NoteIn(text))) {
            json.decodeFromString(Note.serializer(), it)
        }

    suspend fun deleteNote(id: Int): Unit = call("/notes/$id", "DELETE") { }

    suspend fun listReminders(): List<Reminder> = call("/reminders") { json.decodeFromString(it) }

    suspend fun addReminder(text: String, dueAt: String): Reminder =
        call("/reminders", "POST", json.encodeToString(ReminderIn.serializer(), ReminderIn(text, dueAt))) {
            json.decodeFromString(Reminder.serializer(), it)
        }

    suspend fun completeReminder(id: Int): Reminder = call("/reminders/$id/done", "POST", "{}") {
        json.decodeFromString(Reminder.serializer(), it)
    }

    suspend fun listRoutines(): List<Routine> = call("/routines") { json.decodeFromString(it) }

    suspend fun addRoutine(name: String, steps: List<String>): Routine =
        call("/routines", "POST", json.encodeToString(RoutineIn.serializer(), RoutineIn(name, steps))) {
            json.decodeFromString(Routine.serializer(), it)
        }

    suspend fun deleteRoutine(id: Int): Unit = call("/routines/$id", "DELETE") { }

    // --- Paperwork wizard ---

    private fun parseProfile(text: String): Map<String, String> =
        json.parseToJsonElement(text).jsonObject.mapValues { it.value.jsonPrimitive.content }

    suspend fun getProfile(): Map<String, String> = call("/profile") { parseProfile(it) }

    suspend fun putProfile(fields: Map<String, String>): Map<String, String> =
        call("/profile", "PUT", json.encodeToString(ProfileUpdate.serializer(), ProfileUpdate(fields))) {
            parseProfile(it)
        }

    suspend fun getTemplates(): List<PaperworkTemplate> =
        call("/paperwork/templates") { json.decodeFromString(it) }

    suspend fun fillPaperwork(templateId: String, values: Map<String, String>): PaperworkDocument =
        call(
            "/paperwork/fill", "POST",
            json.encodeToString(FillRequest.serializer(), FillRequest(templateId, values)),
        ) { json.decodeFromString(PaperworkDocument.serializer(), it) }

    suspend fun listDocuments(): List<PaperworkDocument> =
        call("/paperwork/documents") { json.decodeFromString(it) }

    suspend fun deleteDocument(id: Int): Unit = call("/paperwork/documents/$id", "DELETE") { }
}
