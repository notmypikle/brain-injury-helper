package com.bih.app.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class BiometricGate(
    private val context: Context,
    private val activity: FragmentActivity,
    private val executor: Executor,
) {
    fun isAvailable(): Boolean {
        val auth = BiometricManager.Authenticators.BIOMETRIC_STRONG
        return BiometricManager.from(context).canAuthenticate(auth) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun prompt(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use PIN instead")
            .build()
        prompt.authenticate(info)
    }
}
