package com.solene.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderScheduleTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val eight = LocalTime.of(8, 0)

    private fun at(text: String, zone: ZoneId = this.zone) = ZonedDateTime.of(LocalDateTime.parse(text), zone)

    @Test
    fun dailyReminderFiresLaterTodayWhenTheTimeIsAhead() {
        assertEquals(at("2026-10-07T08:00"), ReminderSchedule.nextDaily(at("2026-10-07T06:30"), eight))
    }

    @Test
    fun dailyReminderMovesToTomorrowOnceTheTimeHasCome() {
        assertEquals(at("2026-10-08T08:00"), ReminderSchedule.nextDaily(at("2026-10-07T08:00"), eight))
        assertEquals(at("2026-10-08T08:00"), ReminderSchedule.nextDaily(at("2026-10-07T21:00"), eight))
    }

    @Test
    fun dailyReminderInADaylightSavingGapFiresAfterTheGap() {
        val berlin = ZoneId.of("Europe/Berlin")
        val next = ReminderSchedule.nextDaily(at("2026-03-28T23:00", berlin), LocalTime.of(2, 30))
        assertEquals(at("2026-03-29T03:30", berlin).toInstant(), next.toInstant())
    }

    @Test
    fun periodReminderComesThreeDaysAhead() {
        assertEquals(
            at("2026-10-12T08:00"),
            ReminderSchedule.periodReminder(at("2026-10-07T12:00"), LocalDate.of(2026, 10, 15), eight),
        )
    }

    @Test
    fun noPeriodReminderOnceItsMomentHasPassed() {
        assertNull(ReminderSchedule.periodReminder(at("2026-10-13T09:00"), LocalDate.of(2026, 10, 15), eight))
        assertNull(ReminderSchedule.periodReminder(at("2026-10-07T09:00"), null, eight))
    }

    @Test
    fun parsesStoredAndTwentyFourHourTimes() {
        assertEquals(LocalTime.of(8, 0), ReminderTime.parse("08:00 AM"))
        assertEquals(LocalTime.of(19, 30), ReminderTime.parse("07:30 pm"))
        assertEquals(LocalTime.of(19, 45), ReminderTime.parse("19:45"))
        assertEquals(ReminderTime.DEFAULT, ReminderTime.parse("not a time"))
    }

    @Test
    fun formatRoundTrips() {
        val time = LocalTime.of(21, 5)
        assertEquals("09:05 PM", ReminderTime.format(time))
        assertEquals(time, ReminderTime.parse(ReminderTime.format(time)))
    }
}
