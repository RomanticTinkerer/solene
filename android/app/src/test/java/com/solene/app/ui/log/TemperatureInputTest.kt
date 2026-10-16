package com.solene.app.ui.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperatureInputTest {

    private fun parse(text: String) = DayLogViewModel.parseTemperature(text)

    @Test
    fun blankMeansNotRecorded() {
        assertNull(parse("  ").getOrThrow())
    }

    @Test
    fun acceptsADecimalComma() {
        assertEquals(36.55, parse(" 36,55 ").getOrThrow()!!, 0.0)
        assertEquals(36.5, parse("36.5").getOrThrow()!!, 0.0)
    }

    @Test
    fun rejectsImplausibleOrUnreadableValues() {
        assertTrue(parse("34.9").isFailure)
        assertTrue(parse("98.6").isFailure)
        assertTrue(parse("warm").isFailure)
    }
}
