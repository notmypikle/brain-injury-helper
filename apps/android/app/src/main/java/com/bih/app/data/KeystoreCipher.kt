package com.bih.app.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Encrypts small strings (API key, cached profile) with a key that lives in
// the Android Keystore — the phone's hardware-backed vault (risk S4).
// The key never leaves the secure hardware and can't be extracted, so the
// stored data is unreadable if the phone's files are copied off-device.
object KeystoreCipher {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "bih_settings_key"
    private const val PREFIX = "enc:"
    private const val IV_BYTES = 12

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    fun isEncrypted(value: String) = value.startsWith(PREFIX)

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
    }

    // Returns null if decryption fails (e.g. key invalidated) — callers fall
    // back to defaults rather than crashing.
    fun decrypt(stored: String): String? {
        if (!isEncrypted(stored)) return stored // legacy plaintext, re-saved on next write
        return runCatching {
            val bytes = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE, key(),
                GCMParameterSpec(128, bytes.copyOfRange(0, IV_BYTES)),
            )
            String(cipher.doFinal(bytes.copyOfRange(IV_BYTES, bytes.size)), Charsets.UTF_8)
        }.getOrNull()
    }
}
