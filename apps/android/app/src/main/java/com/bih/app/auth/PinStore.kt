package com.bih.app.auth

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "bih_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun hasPin(): Boolean = prefs.contains(PIN_HASH_KEY) && prefs.contains(PIN_SALT_KEY)

    fun savePin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(PIN_SALT_KEY, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(PIN_HASH_KEY, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltRaw = prefs.getString(PIN_SALT_KEY, null) ?: return false
        val hashRaw = prefs.getString(PIN_HASH_KEY, null) ?: return false
        val salt = Base64.decode(saltRaw, Base64.NO_WRAP)
        val expected = Base64.decode(hashRaw, Base64.NO_WRAP)
        val actual = hashPin(pin, salt)
        return expected.contentEquals(actual)
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    companion object {
        private const val PIN_HASH_KEY = "pin_hash"
        private const val PIN_SALT_KEY = "pin_salt"
        private const val PBKDF2_ITERATIONS = 120_000
        private const val PBKDF2_KEY_LENGTH_BITS = 256
    }
}
