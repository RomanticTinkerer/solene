package com.solene.app.domain.repository

import com.solene.app.domain.model.CycleDay
import com.solene.app.domain.model.CycleStats
import com.solene.app.domain.model.FertilityStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for cycle data access.
 *
 * Defines the contract that any data source (Room DB, mock, network)
 * must implement. Follows clean architecture — the domain layer
 * depends only on this interface, not on concrete implementations.
 */
interface CycleRepository {

    /**
     * Emits once stored data has loaded and again after every change.
     * The getters below return empty results until the first emission.
     */
    val updates: Flow<Unit>

    /** Get the current cycle day number for today. */
    fun getCurrentCycleDayNumber(): Int?

    /** Get the total cycle length (days in the current cycle). */
    fun getCurrentCycleLength(): Int

    /** Get cycle progress as a fraction (0.0 to 1.0). */
    fun getCycleProgress(): Float

    /** Get today's cycle day data. */
    fun getTodayCycleDay(): CycleDay?

    /**
     * What was logged on [date], numbered within its cycle. Null when nothing
     * was logged and no period is known before that date.
     */
    fun getCycleDay(date: LocalDate): CycleDay?

    /** Get fertility status for today. */
    fun getFertilityStatus(): FertilityStatus

    /**
     * Statistics over completed cycles that started on or after [since]
     * (all of them when null), or null when there are none.
     */
    fun getCycleStats(since: LocalDate? = null): CycleStats?

    /** Get all cycle days for a given month (for calendar display). */
    fun getCycleDaysForMonth(year: Int, month: Int): List<CycleDay>

    /** Get bleeding days for calendar marking. */
    fun getBleedingDays(): List<LocalDate>

    /** Get predicted bleeding days. */
    fun getPredictedBleedingDays(): List<LocalDate>

    /** All days the user has logged, oldest first, numbered within their cycles. */
    fun getLoggedDays(): List<CycleDay>

    /**
     * Replace whatever was logged on [CycleDay.date]. A day with nothing
     * recorded removes the entry. [updates] emits once the change is applied.
     */
    fun saveCycleDay(day: CycleDay)
}
