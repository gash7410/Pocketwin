package com.pocketwin.voice

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantTest {
    private val assistant = Assistant(object : Environment {
        override fun now(): LocalDateTime = LocalDateTime.of(2026, 10, 11, 15, 5)
        override fun batteryPercent(): Int? = 80
    })

    @Test fun time() = assertEquals("It's 3:05 PM.", assistant.respond("What time is it?").text)

    @Test fun date() = assertEquals("Today is Sunday, October 11.", assistant.respond("what's the date today").text)

    @Test fun battery() = assertEquals("Battery is at 80 percent.", assistant.respond("battery level").text)

    @Test fun timerCompound() {
        val r = assistant.respond("set a timer for 1 hour 30 minutes")
        assertEquals(Action.SetTimer(5400, null), r.action)
        assertEquals("Timer set for 1 hour 30 minutes.", r.text)
    }

    @Test fun timerWords() =
        assertEquals(Action.SetTimer(300, null), assistant.respond("start a timer for five minutes").action)

    @Test fun openApp() = assertEquals(Action.OpenApp("chrome"), assistant.respond("Open Chrome").action)

    @Test fun search() =
        assertEquals(Action.WebSearch("best pizza"), assistant.respond("search for best pizza").action)

    @Test fun unknownFallsBackToHelp() {
        val r = assistant.respond("sing opera")
        assertNull(r.action)
        assertTrue(r.text.startsWith("Sorry"))
    }

    @Test fun empty() = assertEquals("I didn't catch that.", assistant.respond("  ").text)
}
