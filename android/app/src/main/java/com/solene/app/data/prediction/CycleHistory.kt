package com.solene.app.data.prediction

import com.solene.app.domain.model.BleedingIntensity
import com.solene.app.domain.model.CompletedCycle
import com.solene.app.domain.model.CycleDay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Derives periods and completed cycles from logged days, the way drip does:
 * bleeding days separated by at most one dry day form one period, and a
 * period made only of spotting does not start a cycle.
 */
object CycleHistory {
    const val MAX_BREAK_IN_BLEEDING = 1

    /**
     * First day of every period, oldest first, ignoring future days.
     * [setupStart] (the last period entered during onboarding) is kept only
     * when it comes before every logged period, so logged data wins.
     */
    fun periodStarts(days: List<CycleDay>, setupStart: LocalDate?, today: LocalDate): List<LocalDate> {
        val starts = loggedPeriodStarts(days, today)
        val first = starts.firstOrNull()
        val keepSetupStart = setupStart != null && !setupStart.isAfter(today) &&
            (first == null || setupStart.isBefore(first.minusDays(MAX_BREAK_IN_BLEEDING + 1L)))
        return if (keepSetupStart) listOf(setupStart!!) + starts else starts
    }

    /**
     * Cycles between consecutive period starts, preceded by the lengths entered
     * at setup (oldest first, ending at [setupStart]). Entered cycles that end
     * after the first logged period overlap logged data and are dropped; a
     * logged start moved a day or two from [setupStart] is not an overlap.
     */
    fun completedCycles(
        starts: List<LocalDate>,
        setupLengths: List<Int>,
        setupStart: LocalDate?,
    ): List<CompletedCycle> {
        val logged = starts.zipWithNext { from, to ->
            CompletedCycle(from, ChronoUnit.DAYS.between(from, to).toInt())
        }
        if (setupStart == null) return logged

        val latestAllowedEnd = starts.firstOrNull()?.plusDays(MAX_BREAK_IN_BLEEDING + 1L)
        val entered = mutableListOf<CompletedCycle>()
        var end: LocalDate = setupStart
        for (length in setupLengths.asReversed()) {
            val start = end.minusDays(length.toLong())
            if (latestAllowedEnd == null || !end.isAfter(latestAllowedEnd)) {
                entered += CompletedCycle(start, length, enteredAtSetup = true)
            }
            end = start
        }
        return entered.asReversed() + logged
    }

    /**
     * The run of bleeding days that opens the cycle at [cycleStart],
     * allowing one dry day inside the period like drip does.
     */
    fun mensesRun(loggedDays: List<CycleDay>, cycleStart: LocalDate): List<CycleDay> {
        val bleeding = loggedDays
            .filter { it.bleeding != null && !it.date.isBefore(cycleStart) }
            .sortedBy { it.date }
        if (bleeding.firstOrNull()?.date != cycleStart) return emptyList()

        val run = mutableListOf<CycleDay>()
        for (day in bleeding) {
            val previous = run.lastOrNull()
            if (previous != null && daysBetween(previous.date, day.date) > MAX_BREAK_IN_BLEEDING + 1) break
            run += day
        }
        return run
    }

    /**
     * Length in days of each logged period, keyed by its start. A period that
     * could still continue (bleeding within the allowed break of [today]) is left out.
     */
    fun periodLengths(days: List<CycleDay>, starts: List<LocalDate>, today: LocalDate): Map<LocalDate, Int> =
        starts.mapNotNull { start ->
            val run = mensesRun(days, start)
            val last = run.lastOrNull() ?: return@mapNotNull null
            if (daysBetween(last.date, today) <= MAX_BREAK_IN_BLEEDING + 1) return@mapNotNull null
            start to daysBetween(start, last.date) + 1
        }.toMap()

    private fun loggedPeriodStarts(days: List<CycleDay>, today: LocalDate): List<LocalDate> {
        val bleeding = days
            .filter { it.bleeding != null && !it.date.isAfter(today) }
            .sortedBy { it.date }

        val starts = mutableListOf<LocalDate>()
        var runStart: LocalDate? = null
        var runHasFlow = false
        var previous: LocalDate? = null
        for (day in bleeding) {
            if (previous == null || daysBetween(previous, day.date) > MAX_BREAK_IN_BLEEDING + 1) {
                if (runHasFlow) starts += runStart!!
                runStart = day.date
                runHasFlow = false
            }
            if (day.bleeding != BleedingIntensity.SPOTTING) runHasFlow = true
            previous = day.date
        }
        if (runHasFlow) starts += runStart!!
        return starts
    }

    private fun daysBetween(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()
}
