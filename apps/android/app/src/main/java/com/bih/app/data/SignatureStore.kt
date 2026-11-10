package com.bih.app.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// USER'S IDEA (Nov 2026): e-signature for online paperwork.
// The drawn signature never leaves the phone — it is encrypted with the
// same Android Keystore key as the profile cache (risk S13).
class SignatureStore(context: Context) {
    // Same private prefs file as SettingsStore; this value is encrypted.
    private val prefs = context.getSharedPreferences("bih_settings", Context.MODE_PRIVATE)

    fun load(): List<SignatureStroke> = prefs.getString(KEY, null)
        ?.let { KeystoreCipher.decrypt(it) }
        ?.let { runCatching { Json.decodeFromString<List<SignatureStroke>>(it) }.getOrNull() }
        ?: emptyList()

    fun save(strokes: List<SignatureStroke>) {
        prefs.edit()
            .putString(KEY, KeystoreCipher.encrypt(Json.encodeToString(strokes)))
            .apply()
    }

    fun clear() = prefs.edit().remove(KEY).apply()

    fun hasSignature(): Boolean = prefs.getString(KEY, null) != null

    companion object {
        private const val KEY = "signature_strokes"
    }
}
