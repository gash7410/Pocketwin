package com.pocketwin.voice

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Side effects the UI layer must perform on the assistant's behalf. */
sealed interface Action {
    data class SetTimer(val seconds: Int, val label: String?) : Action
    data class OpenApp(val name: String) : Action
    data class WebSearch(val query: String) : Action
}

data class Reply(val text: String, val action: Action? = null)

/** Device facts the assistant needs; injected so [Assistant] stays pure and testable. */
interface Environment {
    fun now(): LocalDateTime
    fun batteryPercent(): Int?
}

/**
 * Rule-based command interpreter. Fully offline: speech-to-text produces a string, this maps it
 * to a spoken [Reply] plus an optional [Action].
 */
class Assistant(private val env: Environment) {

    fun respond(utterance: String): Reply {
        val text = utterance.lowercase(Locale.ROOT).trim().trimEnd('.', '?', '!')
        if (text.isEmpty()) return Reply("I didn't catch that.")

        parseTimer(text)?.let { (seconds, label) ->
            return Reply("Timer set for ${describeDuration(seconds)}.", Action.SetTimer(seconds, label))
        }
        OPEN.find(text)?.let {
            val app = it.groupValues[1].trim()
            return Reply("Opening $app.", Action.OpenApp(app))
        }
        SEARCH.find(text)?.let {
            val q = it.groupValues[1].trim()
            return Reply("Searching for $q.", Action.WebSearch(q))
        }
        return when {
            "time" in text.split(' ') && ("what" in text || "tell" in text || "current" in text) ->
                Reply("It's ${env.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))}.")
            Regex("\\b(date|day|today)\\b").containsMatchIn(text) && ("what" in text || "tell" in text) ->
                Reply("Today is ${env.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US))}.")
            "battery" in text ->
                env.batteryPercent()?.let { Reply("Battery is at $it percent.") }
                    ?: Reply("I can't read the battery level right now.")
            Regex("^(hello|hi|hey)\\b").containsMatchIn(text) -> Reply("Hello! How can I help?")
            "thank" in text -> Reply("You're welcome.")
            "help" in text || "what can you do" in text -> Reply(HELP)
            else -> Reply("Sorry, I don't know how to do that yet. $HELP")
        }
    }

    internal fun parseTimer(text: String): Pair<Int, String?>? {
        if ("timer" !in text) return null
        val m = TIMER_AMOUNT.findAll(text).toList()
        if (m.isEmpty()) return null
        var seconds = 0
        for (match in m) {
            val n = match.groupValues[1].toIntOrNull() ?: WORDS[match.groupValues[1]] ?: continue
            seconds += when (match.groupValues[2][0]) {
                'h' -> n * 3600
                'm' -> n * 60
                else -> n
            }
        }
        return if (seconds > 0) seconds to null else null
    }

    private fun describeDuration(total: Int): String {
        val h = total / 3600
        val m = total % 3600 / 60
        val s = total % 60
        return listOfNotNull(
            plural(h, "hour"), plural(m, "minute"), plural(s, "second"),
        ).joinToString(" ")
    }

    private fun plural(n: Int, unit: String) = if (n == 0) null else "$n $unit${if (n == 1) "" else "s"}"

    private companion object {
        const val HELP = "Try: what time is it, set a timer for 5 minutes, open Chrome, " +
            "search for pizza, or what's my battery."
        val OPEN = Regex("^(?:please )?(?:open|launch|start)\\s+(.+)$")
        val SEARCH = Regex("^(?:please )?(?:search(?: the web)?(?: for)?|google|look up)\\s+(.+)$")
        val WORDS = mapOf(
            "a" to 1, "an" to 1, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "fifteen" to 15,
            "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50, "sixty" to 60,
        )
        val TIMER_AMOUNT = Regex(
            "(\\d+|${WORDS.keys.joinToString("|")})\\s*(hours?|hrs?|minutes?|mins?|seconds?|secs?)\\b",
        )
    }
}
