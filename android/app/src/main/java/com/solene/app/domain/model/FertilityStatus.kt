package com.solene.app.domain.model

import java.time.LocalDate

/**
 * Fertility status for a given day, derived from symptothermal analysis.
 *
 * Mirrors the original drip. fertility status system with phase tracking
 * and predicted menstruation dates.
 */
data class FertilityStatus(
    val phase: Int,
    val status: FertilityLevel,
    val statusText: String,
    val predictedNextPeriod: LocalDate? = null,
    /** Negative once the predicted start has passed without a new period logged. */
    val daysUntilNextPeriod: Int? = null,
    val usedOnDeviceModel: Boolean = false,
    /** False when the user turned fertility signs off and only tracks periods. */
    val fertilityTracked: Boolean = true,
)

/** Only the symptothermal rules decide INFERTILE; predictions never do. */
enum class FertilityLevel {
    INFERTILE,
    FERTILE,
    UNKNOWN,
}
