package com.solene.app.data.fertility

import com.solene.app.domain.model.CervixFirmness
import com.solene.app.domain.model.CycleDay
import com.solene.app.domain.model.FertilityLevel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The sign read next to temperature. Settings store [label]. */
enum class FertilitySigns(val label: String, val secondary: SecondarySymptom?) {
    MUCUS("Temperature + cervical mucus", SecondarySymptom.MUCUS),
    CERVIX("Temperature + cervix", SecondarySymptom.CERVIX),
    OFF("Off, periods only", null);

    companion object {
        /** Earlier builds stored method names (Symptothermal, Sensiplan, …) that were never applied. */
        fun fromSetting(value: String): FertilitySigns =
            entries.find { it.label == value } ?: if (value == "None") OFF else MUCUS
    }
}

data class DayFertility(val phase: Int, val level: FertilityLevel, val text: String)

/** drip's adapter between logged days and [Sympto], with drip's status texts. */
object FertilityRules {

    private val RULE_NAMES = listOf("regular", "1st exception", "2nd exception")

    /** Null before the first known period start. */
    fun forDay(
        date: LocalDate,
        loggedDays: List<CycleDay>,
        periodStarts: List<LocalDate>,
        secondary: SecondarySymptom,
    ): DayFertility? {
        val index = periodStarts.indexOfLast { !it.isAfter(date) }
        if (index < 0) return null
        fun cycle(i: Int) = symptoCycle(periodStarts[i], periodStarts.getOrNull(i + 1), loggedDays)

        val current = cycle(index)
        val status = Sympto.status(
            cycle = current,
            previousCycle = if (index > 0) cycle(index - 1) else null,
            earlierCycles = (0 until index - 1).map(::cycle),
            secondary = secondary,
        )
        return describe(date, status, current)
    }

    private fun describe(date: LocalDate, status: SymptoStatus, cycle: List<SymptoDay>): DayFertility {
        val pre = status.preOvulatory
        val post = status.postOvulatory
        val rule = status.temperatureShift?.let { RULE_NAMES[it.rule] }
        return when {
            pre != null && pre.end != null && date <= pre.end -> {
                val lastDay = ChronoUnit.DAYS.between(cycle.first().date, pre.end) + 1
                DayFertility(
                    1,
                    FertilityLevel.INFERTILE,
                    "With NFP rules, you may assume infertility through cycle day $lastDay, provided " +
                        "you don't observe any fertile cervical mucus or cervix values.",
                )
            }
            post != null && date > post.start -> DayFertility(
                3,
                FertilityLevel.INFERTILE,
                "We detected a temperature shift ($rule temperature rule), as well as a cervical " +
                    "mucus/cervix shift according to NFP rules. You may assume infertility, but always " +
                    "remember to double-check for yourself. Make sure the data makes sense to you.",
            )
            post != null && date == post.start -> DayFertility(
                2,
                FertilityLevel.FERTILE,
                "Fertile phase ends in the evening. We detected a temperature shift ($rule temperature " +
                    "rule), as well as a cervical mucus/cervix shift according to NFP rules. In the evening " +
                    "today you may assume infertility, but always remember to double-check for yourself. " +
                    "Make sure the data makes sense to you.",
            )
            else -> DayFertility(
                2,
                FertilityLevel.FERTILE,
                "We were not able to detect both a temperature shift and cervical mucus or cervix shift." +
                    if (cycle.none { it.temperature != null }) {
                        " Log your waking temperature each morning so the rules can find infertile days."
                    } else {
                        ""
                    },
            )
        }
    }

    /** Logged days from [start] up to [nextStart]; the first day always counts as bleeding. */
    private fun symptoCycle(start: LocalDate, nextStart: LocalDate?, days: List<CycleDay>): List<SymptoDay> {
        val logged = days
            .filter { !it.date.isBefore(start) && (nextStart == null || it.date.isBefore(nextStart)) }
            .map { it.toSympto() }
        val first = logged.firstOrNull()?.takeIf { it.date == start }
        val head = (first ?: SymptoDay(start)).copy(bleeding = first?.bleeding ?: 0)
        return listOf(head) + logged.drop(if (first != null) 1 else 0)
    }

    private fun CycleDay.toSympto(): SymptoDay {
        val opening = cervix?.opening
        val firmness = cervix?.firmness
        return SymptoDay(
            date = date,
            bleeding = bleeding?.ordinal,
            temperature = temperature?.takeUnless { temperatureExcluded },
            mucus = mucus?.nfpValue,
            // Sensiplan only knows hard and soft, so anything not firm counts as soft.
            cervix = if (opening != null && firmness != null) {
                SymptoCervix(opening.ordinal, if (firmness == CervixFirmness.FIRM) 0 else 1)
            } else {
                null
            },
        )
    }
}
