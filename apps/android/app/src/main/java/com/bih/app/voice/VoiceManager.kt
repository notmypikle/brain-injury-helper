package com.bih.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * On-device voice for B.I.H. — talk-to-text and spoken replies.
 *
 * Audio never leaves the phone: SpeechRecognizer and TextToSpeech are the
 * Android system engines (see docs/VOICE_AND_PRODUCTION.md). The API only
 * ever receives the transcribed text.
 */
class VoiceManager(
    context: Context,
    private val onTranscript: (String) -> Unit,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val appContext = context.applicationContext

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private var recognizer: SpeechRecognizer? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            tts?.language = Locale.getDefault()
            // Slightly slower than default — easier to follow for users
            // with cognitive difficulties.
            tts?.setSpeechRate(0.9f)
        }
    }

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    /** Start listening. Caller must hold RECORD_AUDIO permission. */
    fun startListening() {
        if (!isRecognitionAvailable) {
            onError("Speech recognition is not available on this device")
            return
        }
        releaseRecognizer()
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = onListeningChanged(true)
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() = onListeningChanged(false)

                override fun onError(error: Int) {
                    onListeningChanged(false)
                    onError("Couldn't hear that — please try again")
                }

                override fun onResults(results: Bundle?) {
                    onListeningChanged(false)
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (text.isNullOrBlank()) {
                        onError("I didn't catch that — please try again")
                    } else {
                        onTranscript(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }
            )
        }
    }

    fun stopListening() {
        recognizer?.stopListening()
        onListeningChanged(false)
    }

    /** Speak a reply aloud. */
    fun speak(text: String) {
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bih_reply")
        }
    }

    /** Call from the screen's DisposableEffect onDispose. */
    fun destroy() {
        releaseRecognizer()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun releaseRecognizer() {
        recognizer?.destroy()
        recognizer = null
    }
}
