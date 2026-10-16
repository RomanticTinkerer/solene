package com.solene.app.domain.model

import java.time.LocalDate

/**
 * Aggregated statistics for cycle length analysis.
 *
 * Mirrors the original drip. Stats component's data requirements:
 * mean, min, max, standard deviation, and cycle count.
 */
data class CycleStats(
    val averageCycleLength: Double,
    val minimumCycleLength: Int,
    val maximumCycleLength: Int,
    /** Sample standard deviation; 0 with fewer than two cycles. */
    val standardDeviation: Double,
    val numberOfCycles: Int,
    val cycleLengths: List<Int>,
    val cycles: List<CompletedCycle> = emptyList(),
    val averagePeriodLength: Double? = null,
)

/** A finished cycle: from one period start up to the day before the next. */
data class CompletedCycle(
    val start: LocalDate,
    val length: Int,
    /** Entered as a prior length during setup; its dates are counted back from the setup period. */
    val enteredAtSetup: Boolean = false,
)
