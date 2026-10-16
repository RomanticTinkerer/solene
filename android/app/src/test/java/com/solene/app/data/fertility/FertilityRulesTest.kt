package com.solene.app.data.fertility

import com.solene.app.domain.model.BleedingIntensity
import com.solene.app.domain.model.CycleDay
import com.solene.app.domain.model.FertilityLevel
import com.solene.app.domain.model.MucusQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FertilityRulesTest {

    private val previousStart = LocalDate.of(2026, 1, 1)
    private val currentStart = LocalDate.of(2026, 1, 29)

    /** Low temperatures through [ovulation], egg white for its last three days, then dry and high. */
    private fun cycle(start: LocalDate, days: Int, ovulation: Int = 14) = (1..days).map { day ->
        CycleDay(
            date = start.plusDays(day - 1L),
            bleeding = BleedingIntensity.MEDIUM.takeIf { day <= 4 },
            temperature = if (day <= ovulation) 36.4 else 36.8,
            mucus = when {
                day <= 4 -> null
                day in ovulation - 2..ovulation -> MucusQuality.EGG_WHITE
                else -> MucusQuality.DRY
            },
        )
    }

    private val logged = cycle(previousStart, 28) + cycle(currentStart, 21)
    private val starts = listOf(previousStart, currentStart)

    private fun on(cycleDay: Int, days: List<CycleDay> = logged) =
        FertilityRules.forDay(currentStart.plusDays(cycleDay - 1L), days, starts, SecondarySymptom.MUCUS)!!

    @Test
    fun followsThePhasesOfALoggedCycle() {
        assertNull(FertilityRules.forDay(previousStart.minusDays(1), logged, starts, SecondarySymptom.MUCUS))

        // previous cycle had a full shift → five infertile days, then fertile until the evening of day 17
        assertEquals(FertilityLevel.INFERTILE, on(1).level)
        assertEquals(1, on(5).phase)
        assertEquals(FertilityLevel.FERTILE, on(6).level)
        assertTrue(on(17).text.startsWith("Fertile phase ends in the evening"))
        assertEquals(3, on(18).phase)
        assertTrue(on(18).text.contains("regular temperature rule"))
    }

    @Test
    fun excludedTemperaturesAreLeftOutOfTheRules() {
        val excluded = logged.map { if (it.date >= currentStart) it.copy(temperatureExcluded = true) else it }
        assertEquals(2, on(18, excluded).phase)
        assertTrue(on(18, excluded).text.contains("Log your waking temperature"))
    }
}
