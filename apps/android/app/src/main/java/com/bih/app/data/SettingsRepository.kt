package com.bih.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bih_settings")

data class Settings(
    val apiUrl: String = DEFAULT_API_URL,
    val apiKey: String = "",
    val speakReplies: Boolean = true,
) {
    companion object {
        const val DEFAULT_API_URL = "http://127.0.0.1:8000"
    }
}

/** Persists user settings on-device (API address, API key, voice toggle). */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_URL = stringPreferencesKey("api_url")
        val API_KEY = stringPreferencesKey("api_key")
        val SPEAK_REPLIES = stringPreferencesKey("speak_replies")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            apiUrl = prefs[Keys.API_URL] ?: Settings.DEFAULT_API_URL,
            apiKey = prefs[Keys.API_KEY] ?: "",
            speakReplies = prefs[Keys.SPEAK_REPLIES] != "false",
        )
    }

    suspend fun save(settings: Settings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.API_URL] = settings.apiUrl
            prefs[Keys.API_KEY] = settings.apiKey
            prefs[Keys.SPEAK_REPLIES] = settings.speakReplies.toString()
        }
    }
}
