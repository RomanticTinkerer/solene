/*
 * Kotlin port of sympto 2.0.0 (https://gitlab.com/bloodyhealth/sympto), the
 * symptothermal rules used by drip. Copyright (C) Julia Friesel, Marie Kochsiek,
 * Tina Baumann and the drip contributors. Licensed AGPL-3.0-or-later.
 *
 * Behaviour matches the JavaScript library except that cycle days for the
 * minus-8-day rule are counted in real days: js-joda's LocalDate.compareTo,
 * which sympto uses there, returns the month difference across months.
 */
package com.solene.app.data.fertility

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.min

/** One logged day in sympto's units: mucus 0–4 (t, Ø, f, S, +S), cervix opening 0–2 and firmness 0–1. */
data class SymptoDay(
    val date: LocalDate,
    val bleeding: Int? = null,
    val temperature: Double? = null,
    val mucus: Int? = null,
    val cervix: SymptoCervix? = null,
)

data class SymptoCervix(val opening: Int, val firmness: Int) {
    val closedAndHard get() = opening == 0 && firmness == 0
}

enum class SecondarySymptom { MUCUS, CERVIX }

/** [end] is inclusive; the peri phase ends and the post phase starts on the same evening. */
data class SymptoPhase(val start: LocalDate, val end: LocalDate?, val days: List<SymptoDay>)

/** [rule]: 0 regular, 1 first exception, 2 second exception. [ltl] is the low temperature level. */
data class TemperatureShift(
    val rule: Int,
    val ltl: Double,
    val firstHighMeasurementDay: SymptoDay,
    val evaluationCompleteDay: SymptoDay,
)

data class SecondaryShift(val peakDay: SymptoDay, val evaluationCompleteDay: SymptoDay)

/** The shifts are only set when both were detected, as in sympto. */
data class SymptoStatus(
    val preOvulatory: SymptoPhase? = null,
    val periOvulatory: SymptoPhase? = null,
    val postOvulatory: SymptoPhase? = null,
    val temperatureShift: TemperatureShift? = null,
    val secondaryShift: SecondaryShift? = null,
)

object Sympto {

    private const val DEFAULT_PRE_OVULATORY_LENGTH = 5
    private const val MIN_FHMS_TO_LENGTHEN = 12
    private const val ERROR_MARGIN = 0.0001

    /** Each cycle is its logged days oldest first and must start with a bleeding day. */
    fun status(
        cycle: List<SymptoDay>,
        previousCycle: List<SymptoDay>? = null,
        earlierCycles: List<List<SymptoDay>> = emptyList(),
        secondary: SecondarySymptom = SecondarySymptom.MUCUS,
        excludePreOvulatory: Boolean = false,
    ): SymptoStatus {
        (listOf(cycle) + earlierCycles).forEach {
            require(it.isNotEmpty()) { "Cycle must not be empty." }
            require(it.first().bleeding != null) { "First cycle day should have bleeding." }
        }

        // Without a full shift in the previous cycle no infertile pre-ovulatory phase may be assumed.
        val pre = if (!excludePreOvulatory && previousCycle != null &&
            status(previousCycle, secondary = secondary).temperatureShift != null
        ) {
            preOvulatoryPhase(cycle, listOf(previousCycle) + earlierCycles, secondary)
        } else {
            null
        }
        if (pre != null && pre.days.size == cycle.size) return SymptoStatus(preOvulatory = pre)

        val periDays = if (pre != null) cycle.drop(cycle.indexOf(pre.days.last()) + 1) else cycle
        val peri = SymptoPhase(pre?.end?.plusDays(1) ?: cycle.first().date, null, periDays)

        val temperature = temperatureShift(cycle) ?: return SymptoStatus(pre, peri)
        val tempEvalEnd = cycle.indexOf(temperature.evaluationCompleteDay)
        val secondaryShift = when (secondary) {
            SecondarySymptom.MUCUS -> mucusShift(cycle, tempEvalEnd)
            SecondarySymptom.CERVIX -> cervixShift(cycle, tempEvalEnd)
        } ?: return SymptoStatus(pre, peri)

        val periEnd = if (temperature.evaluationCompleteDay.date >= secondaryShift.evaluationCompleteDay.date) {
            temperature.evaluationCompleteDay
        } else {
            secondaryShift.evaluationCompleteDay
        }
        // Both peaks need fertile signs, which end the pre phase, so the end is always in the peri days.
        val endIndex = periDays.indexOf(periEnd)
        if (endIndex < 0) return SymptoStatus(pre, peri)

        return SymptoStatus(
            preOvulatory = pre,
            periOvulatory = peri.copy(end = periEnd.date, days = periDays.take(endIndex + 1)),
            postOvulatory = SymptoPhase(periEnd.date, null, periDays.drop(endIndex)),
            temperatureShift = temperature,
            secondaryShift = secondaryShift,
        )
    }

    fun temperatureShift(cycle: List<SymptoDay>): TemperatureShift? {
        val days = cycle.filter { it.temperature != null }
        val temps = days.map { rounded(it.temperature!!) }

        for (i in 6 until temps.size) {
            val ltl = temps.subList(i - 6, i).max()
            if (temps[i] <= ltl) continue
            // need at least 3 high temps to form a high temperature level
            if (i > temps.size - 3) continue
            val next = (i + 1 until min(i + 4, temps.size)).toList()

            val shift = regularRule(next.take(2), temps, ltl)
                ?: firstExceptionRule(next, temps, ltl)
                ?: secondExceptionRule(next, temps, ltl)
                ?: continue
            return TemperatureShift(shift.first, ltl, days[i], days[shift.second])
        }
        return null
    }

    /** Pairs of (rule, index of the day that completes the evaluation). */
    private fun regularRule(next: List<Int>, temps: List<Double>, ltl: Double): Pair<Int, Int>? {
        if (!next.all { temps[it] > ltl }) return null
        if (temps[next[1]] - ltl < 0.2 - ERROR_MARGIN) return null
        return 0 to next[1]
    }

    private fun firstExceptionRule(next: List<Int>, temps: List<Double>, ltl: Double): Pair<Int, Int>? {
        if (next.size < 3 || !next.all { temps[it] > ltl }) return null
        return 1 to next[2]
    }

    private fun secondExceptionRule(next: List<Int>, temps: List<Double>, ltl: Double): Pair<Int, Int>? {
        if (next.size < 3) return null
        val secondIsLow = temps[next[0]] <= ltl
        val thirdIsLow = temps[next[1]] <= ltl
        if (secondIsLow == thirdIsLow) return null
        if (temps[next[2]] - ltl < 0.2 - ERROR_MARGIN) return null
        return 2 to next[2]
    }

    private fun rounded(value: Double) = Math.round(value * 20) / 20.0

    fun mucusShift(cycle: List<SymptoDay>, tempEvalEnd: Int): SecondaryShift? {
        val mucusDays = cycle.filter { it.mucus != null }
        var best = 0

        for ((i, day) in mucusDays.withIndex()) {
            val mucus = day.mucus!!
            if (mucus > best) best = mucus
            // if mucus only changes from dry to nothing, it doesn't constitute a shift
            if (best < 2 || mucus != best) continue

            val following = mucusDays.slice(i + 1 until min(i + 4, mucusDays.size))
            if (following.size < 3 || following.any { it.mucus!! >= best }) continue

            // no best quality day may occur until the temperature evaluation is complete
            val untilTempEvalEnd = cycle.slice(cycle.indexOf(day) + 1..tempEvalEnd).filter { it.mucus != null }
            if (untilTempEvalEnd.all { it.mucus!! < best }) return SecondaryShift(day, following.last())
        }
        return null
    }

    fun cervixShift(cycle: List<SymptoDay>, tempEvalEnd: Int): SecondaryShift? {
        val cervixDays = cycle.filter { it.cervix != null }

        for ((i, day) in cervixDays.withIndex()) {
            if (day.cervix!!.closedAndHard) continue

            val following = cervixDays.slice(i + 1 until min(i + 4, cervixDays.size))
            if (following.size < 3 || following.any { !it.cervix!!.closedAndHard }) continue

            val untilTempEvalEnd = cycle.slice(cycle.indexOf(day) + 1..tempEvalEnd).filter { it.cervix != null }
            if (untilTempEvalEnd.all { it.cervix!!.closedAndHard }) return SecondaryShift(day, following.last())
        }
        return null
    }

    private fun preOvulatoryPhase(
        cycle: List<SymptoDay>,
        previousCycles: List<List<SymptoDay>>,
        secondary: SecondarySymptom,
    ): SymptoPhase? {
        // sympto treats a minus-8 result of 0 as "no result"
        val length = minus8DayRule(previousCycles, secondary)?.takeIf { it != 0 } ?: DEFAULT_PRE_OVULATORY_LENGTH
        val start = cycle.first().date
        val endDate = start.plusDays(length - 1L)
        val candidates = cycle.take(length.coerceAtLeast(0)).filter { it.date <= endDate }

        val firstFertile = candidates.indexOfFirst {
            when (secondary) {
                SecondarySymptom.MUCUS -> (it.mucus ?: 0) > 1
                SecondarySymptom.CERVIX -> it.cervix != null && (it.cervix.opening > 0 || it.cervix.firmness > 0)
            }
        }
        val days = if (firstFertile > -1) candidates.take(firstFertile) else candidates
        if (days.isEmpty()) return null

        val end = if (days.size == candidates.size) endDate else days.last().date
        return SymptoPhase(days.first().date, end, days)
    }

    /** Shortens the pre phase to the earliest first high measurement minus 8; lengthens it only after 12 cycles. */
    private fun minus8DayRule(previousCycles: List<List<SymptoDay>>, secondary: SecondarySymptom): Int? {
        val firstHighCycleDays = previousCycles.mapNotNull { cycle ->
            val shift = status(cycle, secondary = secondary, excludePreOvulatory = true).temperatureShift
                ?: return@mapNotNull null
            ChronoUnit.DAYS.between(cycle.first().date, shift.firstHighMeasurementDay.date).toInt() + 1
        }
        val length = (firstHighCycleDays.minOrNull() ?: return null) - 8
        return if (length < DEFAULT_PRE_OVULATORY_LENGTH || firstHighCycleDays.size >= MIN_FHMS_TO_LENGTHEN) {
            length
        } else {
            null
        }
    }
}
