package com.bih.app.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Stores the API address and API key in private app storage.
// Secrets (API key, profile cache) are encrypted with the Android Keystore
// (risk S4); the non-sensitive API address stays plaintext.
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("bih_settings", Context.MODE_PRIVATE)

    var apiUrl: String
        get() = prefs.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(value) = prefs.edit().putString(KEY_URL, value).apply()

    var apiKey: String
        get() = prefs.getString(KEY_KEY, null)
            ?.let { KeystoreCipher.decrypt(it) }
            ?: ""
        set(value) = prefs.edit().putString(KEY_KEY, KeystoreCipher.encrypt(value)).apply()

    // Local copy of the paperwork profile, written whenever the app fetches
    // or saves the profile. The autofill service reads ONLY this cache — it
    // never makes network calls while filling a form. Encrypted at rest.
    var profileCache: Map<String, String>
        get() = prefs.getString(KEY_PROFILE, null)
            ?.let { KeystoreCipher.decrypt(it) }
            ?.let { runCatching { Json.decodeFromString<Map<String, String>>(it) }.getOrNull() }
            ?: emptyMap()
        set(value) = prefs.edit()
            .putString(KEY_PROFILE, KeystoreCipher.encrypt(Json.encodeToString(value)))
            .apply()

    companion object {
        private const val KEY_URL = "api_url"
        private const val KEY_KEY = "api_key"
        private const val KEY_PROFILE = "profile_cache"
        // Default for the real setup (see docs/PHONE_SETUP.md): the API
        // runs in Termux on the SAME phone, so loopback is correct.
        // Emulator developers: use http://10.0.2.2:8000 instead.
        const val DEFAULT_URL = "http://127.0.0.1:8000"
    }
}
