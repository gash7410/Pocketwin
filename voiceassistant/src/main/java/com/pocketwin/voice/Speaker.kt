package com.pocketwin.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Text-to-speech output. Utterances queued before the engine is ready are held, not dropped. */
class Speaker(context: Context) {
    private var ready = false
    private var pending: String? = null
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                ready = true
                pending?.let { speak(it) }
                pending = null
            }
        }
    }

    fun speak(text: String) {
        if (!ready) {
            pending = text
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reply")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
