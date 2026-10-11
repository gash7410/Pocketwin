package com.pocketwin.voice

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

data class Message(val text: String, val fromUser: Boolean)

class VoiceViewModel(app: Application) : AndroidViewModel(app) {
    val messages = mutableStateListOf<Message>()
    var listening by mutableStateOf(false)
        private set
    var partial by mutableStateOf("")
        private set

    private val assistant = Assistant(AndroidEnvironment(app))
    private val runner = ActionRunner(app)
    private val speaker = Speaker(app)
    private val input = SpeechInput(
        context = app,
        onPartial = { partial = it },
        onResult = { handle(it) },
        onFailure = { reply(it) },
        onListeningChanged = { listening = it; if (!it) partial = "" },
    )

    fun startListening() = input.start()

    fun submit(text: String) = handle(text)

    private fun handle(utterance: String) {
        messages += Message(utterance, fromUser = true)
        val r = assistant.respond(utterance)
        val failure = r.action?.let(runner::run)
        reply(failure ?: r.text)
    }

    private fun reply(text: String) {
        messages += Message(text, fromUser = false)
        speaker.speak(text)
    }

    override fun onCleared() {
        input.stop()
        speaker.shutdown()
    }
}
