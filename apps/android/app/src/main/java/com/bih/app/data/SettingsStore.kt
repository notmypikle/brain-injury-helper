package com.bih.app.data

import android.content.Context

// Stores the API address and API key in private app storage.
// Upgrade path (risk S4): move the API key into Android Keystore-backed
// encrypted storage before public release.
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("bih_settings", Context.MODE_PRIVATE)

    var apiUrl: String
        get() = prefs.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(value) = prefs.edit().putString(KEY_URL, value).apply()

    var apiKey: String
        get() = prefs.getString(KEY_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_KEY, value).apply()

    companion object {
        private const val KEY_URL = "api_url"
        private const val KEY_KEY = "api_key"
        // 10.0.2.2 is how the Android emulator reaches the host machine's
        // localhost. On a real phone, use the computer's LAN address.
        const val DEFAULT_URL = "http://10.0.2.2:8000"
    }
}
