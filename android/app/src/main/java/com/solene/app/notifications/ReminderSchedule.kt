package com.solene.app.notifications

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** When reminders fire; kept free of Android types so it can be unit-tested. */
object ReminderSchedule {
    const val PERIOD_REMINDER_DAYS_BEFORE = 3L

    /** The next time-of-day [time] strictly after [now]. */
    fun nextDaily(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
        val today = now.toLocalDate().atTime(time).atZone(now.zone)
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
    }

    /** [PERIOD_REMINDER_DAYS_BEFORE] days before [nextPeriod] at [time], or null once that has passed. */
    fun periodReminder(now: ZonedDateTime, nextPeriod: LocalDate?, time: LocalTime): ZonedDateTime? {
        if (nextPeriod == null) return null
        val at = nextPeriod.minusDays(PERIOD_REMINDER_DAYS_BEFORE).atTime(time).atZone(now.zone)
        return at.takeIf { it.isAfter(now) }
    }
}

/** The reminder time as stored in preferences, e.g. "08:00 AM". */
object ReminderTime {
    val DEFAULT: LocalTime = LocalTime.of(8, 0)
    private val stored = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    fun parse(text: String): LocalTime {
        val trimmed = text.trim()
        return runCatching { LocalTime.parse(trimmed.uppercase(Locale.US), stored) }
            .recoverCatching { LocalTime.parse(trimmed) }
            .getOrDefault(DEFAULT)
    }

    fun format(time: LocalTime): String = time.format(stored)
}
