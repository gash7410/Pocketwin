package com.pocketwin.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** Thin wrapper over [SpeechRecognizer]; must be used from the main thread. */
class SpeechInput(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onResult: (String) -> Unit,
    private val onFailure: (String) -> Unit,
    private val onListeningChanged: (Boolean) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        if (!isAvailable) {
            onFailure("Speech recognition isn't available on this device.")
            return
        }
        stop()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = onListeningChanged(true)
            override fun onEndOfSpeech() = onListeningChanged(false)
            override fun onPartialResults(partialResults: Bundle?) {
                first(partialResults)?.let(onPartial)
            }
            override fun onResults(results: Bundle?) {
                onListeningChanged(false)
                first(results)?.let(onResult) ?: onFailure("I didn't catch that.")
            }
            override fun onError(error: Int) {
                onListeningChanged(false)
                onFailure(describe(error))
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer = r
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            },
        )
    }

    fun stop() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun first(b: Bundle?) =
        b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private fun describe(code: Int) = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't catch that."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy, try again."
        else -> "Speech recognition error ($code)."
    }
}
