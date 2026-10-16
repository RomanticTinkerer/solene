package com.solene.app.data.prediction

import com.solene.app.domain.model.BleedingIntensity
import com.solene.app.domain.model.CompletedCycle
import com.solene.app.domain.model.CycleDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleHistoryTest {

    private val start = LocalDate.of(2026, 6, 1)
    private val today = LocalDate.of(2026, 9, 15)

    private fun bleeding(offset: Int, intensity: BleedingIntensity = BleedingIntensity.MEDIUM) =
        CycleDay(date = start.plusDays(offset.toLong()), bleeding = intensity)

    private fun period(offset: Int, length: Int = 4) = (offset until offset + length).map { bleeding(it) }

    @Test
    fun findsTheFirstDayOfEachLoggedPeriod() {
        val days = period(0) + period(29) + period(57)
        assertEquals(
            listOf(start, start.plusDays(29), start.plusDays(57)),
            CycleHistory.periodStarts(days, setupStart = start, today = today),
        )
    }

    @Test
    fun spottingOnlyAndFutureBleedingDoNotStartACycle() {
        val days = period(0) +
            bleeding(14, BleedingIntensity.SPOTTING) +
            listOf(CycleDay(date = today.plusDays(2), bleeding = BleedingIntensity.HEAVY))
        assertEquals(listOf(start), CycleHistory.periodStarts(days, setupStart = null, today = today))
    }

    @Test
    fun aPeriodMayOpenWithSpotting() {
        val days = listOf(bleeding(0, BleedingIntensity.SPOTTING)) + period(1)
        assertEquals(listOf(start), CycleHistory.periodStarts(days, setupStart = null, today = today))
    }

    @Test
    fun setupStartCountsOnlyBeforeEveryLoggedPeriod() {
        val logged = period(29)
        assertEquals(
            listOf(start, start.plusDays(29)),
            CycleHistory.periodStarts(logged, setupStart = start, today = today),
        )
        assertEquals(
            listOf(start.plusDays(29)),
            CycleHistory.periodStarts(logged, setupStart = start.plusDays(30), today = today),
        )
        assertEquals(listOf(start), CycleHistory.periodStarts(emptyList(), setupStart = start, today = today))
    }

    @Test
    fun completedCyclesPutSetupLengthsBeforeLoggedOnes() {
        val starts = listOf(start, start.plusDays(29), start.plusDays(57))
        val cycles = CycleHistory.completedCycles(starts, setupLengths = listOf(30, 27), setupStart = start)
        assertEquals(
            listOf(
                CompletedCycle(start.minusDays(57), 30, enteredAtSetup = true),
                CompletedCycle(start.minusDays(27), 27, enteredAtSetup = true),
                CompletedCycle(start, 29),
                CompletedCycle(start.plusDays(29), 28),
            ),
            cycles,
        )
    }

    @Test
    fun setupLengthsOverlappingLoggedPeriodsAreDropped() {
        val backfilled = listOf(start.minusDays(28), start)
        val cycles = CycleHistory.completedCycles(backfilled, setupLengths = listOf(30, 28), setupStart = start)
        assertEquals(
            listOf(
                CompletedCycle(start.minusDays(58), 30, enteredAtSetup = true),
                CompletedCycle(start.minusDays(28), 28),
            ),
            cycles,
        )
    }

    @Test
    fun movingTheFirstPeriodByADayKeepsTheSetupLengths() {
        val starts = listOf(start.minusDays(1))
        val cycles = CycleHistory.completedCycles(starts, setupLengths = listOf(30, 28), setupStart = start)
        assertEquals(listOf(30, 28), cycles.map { it.length })
    }

    @Test
    fun periodLengthsLeaveOutAPeriodThatMayStillBeGoing() {
        val days = period(0, length = 5) + period(29, length = 3)
        val starts = listOf(start, start.plusDays(29))
        val lengths = CycleHistory.periodLengths(days, starts, today = start.plusDays(30))
        assertEquals(mapOf(start to 5), lengths)
    }

    @Test
    fun mensesRunAllowsOneDryDay() {
        val days = listOf(0, 1, 3, 4).map { bleeding(it) }
        assertEquals(4, CycleHistory.mensesRun(days, start).size)
    }

    @Test
    fun mensesRunStopsAfterTwoDryDays() {
        val days = listOf(0, 1, 4).map { bleeding(it) }
        assertEquals(2, CycleHistory.mensesRun(days, start).size)
    }

    @Test
    fun mensesRunIsEmptyWithoutBleedingOnTheStartDay() {
        assertTrue(CycleHistory.mensesRun(listOf(bleeding(1)), start).isEmpty())
    }
}
