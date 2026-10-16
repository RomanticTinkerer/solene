package com.solene.app.data.repository

import com.solene.app.data.datastore.UserPreferencesRepository
import com.solene.app.data.datastore.UserProfile
import com.solene.app.data.fertility.FertilityRules
import com.solene.app.data.fertility.FertilitySigns
import com.solene.app.data.prediction.CycleHistory
import com.solene.app.data.prediction.CyclePrediction
import com.solene.app.data.prediction.CyclePredictionEngine
import com.solene.app.domain.model.BleedingIntensity
import com.solene.app.domain.model.CompletedCycle
import com.solene.app.domain.model.CycleDay
import com.solene.app.domain.model.CycleStats
import com.solene.app.domain.model.FertilityLevel
import com.solene.app.domain.model.FertilityStatus
import com.solene.app.domain.repository.CycleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Cycle repository backed by the user's onboarding + logged days.
 *
 * Replaces the old mock stock-data repository: no invented historical cycles,
 * no fabricated mucus/temperature/mood. Predictions come from the on-device
 * XGBoost engine using the user's onboarding and logged days only.
 */
@Singleton
class UserCycleRepository @Inject constructor(
    private val userPrefs: UserPreferencesRepository,
    private val predictionEngine: CyclePredictionEngine,
) : CycleRepository {

    private data class Snapshot(
        val profile: UserProfile,
        val loggedDays: List<CycleDay>,
        val periodStarts: List<LocalDate>,
        val cycles: List<CompletedCycle>,
        val prediction: CyclePrediction?,
    ) {
        val cycleStart: LocalDate? get() = periodStarts.lastOrNull()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val snapshot = MutableStateFlow<Snapshot?>(null)
    private val writeLock = Mutex()

    override val updates: Flow<Unit> = snapshot.filterNotNull().map { }

    init {
        scope.launch {
            combine(userPrefs.userProfile, userPrefs.loggedCycleDays) { profile, stored ->
                // Installs from before onboarding saved its period have nothing stored yet.
                buildSnapshot(profile, stored ?: buildOnboardingBleedingDays(profile))
            }.collect { snapshot.value = it }
        }
    }

    private fun buildSnapshot(profile: UserProfile, days: List<CycleDay>): Snapshot {
        val today = LocalDate.now()
        val starts = CycleHistory.periodStarts(days, profile.lastPeriodDate, today)
        val cycles = CycleHistory.completedCycles(starts, profile.priorCycleLengths, profile.lastPeriodDate)
        return Snapshot(
            profile = profile,
            loggedDays = days,
            periodStarts = starts,
            cycles = cycles,
            prediction = predictionEngine.predict(
                profile = profile,
                loggedDays = days,
                cycleStart = starts.lastOrNull(),
                today = today,
                history = cycles.map { it.length },
                periodStarts = starts,
            ),
        )
    }

    private fun profile(): UserProfile = snapshot.value?.profile ?: UserProfile()

    private fun loggedDays(): List<CycleDay> = snapshot.value?.loggedDays.orEmpty()

    private fun periodStarts(): List<LocalDate> = snapshot.value?.periodStarts.orEmpty()

    private fun cycleStart(): LocalDate? = snapshot.value?.cycleStart

    private fun currentPrediction(): CyclePrediction? = snapshot.value?.prediction

    private fun cycleDayNumber(date: LocalDate): Int? {
        val start = periodStarts().lastOrNull { !it.isAfter(date) } ?: return null
        return ChronoUnit.DAYS.between(start, date).toInt() + 1
    }

    /** Mean length of finished logged periods, else what the user entered at setup. */
    private fun typicalPeriodLength(): Int {
        val logged = CycleHistory.periodLengths(loggedDays(), periodStarts(), LocalDate.now()).values
        return logged.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
            ?: profile().averagePeriodLength
    }

    override fun getCurrentCycleDayNumber(): Int? {
        val start = cycleStart() ?: return null
        val days = ChronoUnit.DAYS.between(start, LocalDate.now()).toInt() + 1
        return days.takeIf { it > 0 }
    }

    override fun getCurrentCycleLength(): Int {
        val prediction = currentPrediction()
        return prediction?.predictedCycleLength
            ?: profile().averageCycleLength.takeIf { it > 0 }
            ?: 0
    }

    override fun getCycleProgress(): Float {
        val day = getCurrentCycleDayNumber() ?: return 0f
        val length = getCurrentCycleLength().takeIf { it > 0 } ?: return 0f
        return (day.toFloat() / length).coerceIn(0f, 1f)
    }

    override fun getTodayCycleDay(): CycleDay? {
        return getCycleDay(LocalDate.now())
    }

    override fun getCycleDay(date: LocalDate): CycleDay? {
        val number = cycleDayNumber(date)
        val logged = loggedDays().find { it.date == date }
        return when {
            logged != null -> logged.copy(cycleDayNumber = number)
            number != null -> CycleDay(date = date, cycleDayNumber = number)
            else -> null
        }
    }

    override fun getFertilityStatus(): FertilityStatus {
        val today = LocalDate.now()
        val prediction = currentPrediction()
        val unknown = FertilityStatus(
            phase = 0,
            status = FertilityLevel.UNKNOWN,
            statusText = "Add your last period to unlock personalized predictions",
            predictedNextPeriod = prediction?.predictedNextPeriod,
            daysUntilNextPeriod = prediction?.let { ChronoUnit.DAYS.between(today, it.predictedNextPeriod).toInt() },
            usedOnDeviceModel = prediction?.usedOnDeviceModel == true,
        )
        val secondary = FertilitySigns.fromSetting(profile().nfpMethod).secondary
            ?: return unknown.copy(fertilityTracked = false)
        val day = FertilityRules.forDay(today, loggedDays(), periodStarts(), secondary) ?: return unknown
        return unknown.copy(phase = day.phase, status = day.level, statusText = day.text)
    }

    override fun getCycleStats(since: LocalDate?): CycleStats? {
        val current = snapshot.value ?: return null
        val cycles = current.cycles.filter { since == null || !it.start.isBefore(since) }
        if (cycles.isEmpty()) return null

        val lengths = cycles.map { it.length }
        val periodLengths = CycleHistory
            .periodLengths(current.loggedDays, current.periodStarts, LocalDate.now())
            .filterKeys { since == null || !it.isBefore(since) }
            .values
        return CycleStats(
            averageCycleLength = lengths.average().roundTo(1),
            minimumCycleLength = lengths.min(),
            maximumCycleLength = lengths.max(),
            standardDeviation = CyclePredictionEngine.sampleStandardDeviation(lengths).roundTo(2),
            numberOfCycles = lengths.size,
            cycleLengths = lengths,
            cycles = cycles,
            averagePeriodLength = periodLengths.takeIf { it.isNotEmpty() }?.average()?.roundTo(1),
        )
    }

    override fun getCycleDaysForMonth(year: Int, month: Int): List<CycleDay> {
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.plusMonths(1).minusDays(1)
        return generateSequence(firstDay) { it.plusDays(1) }
            .takeWhile { !it.isAfter(lastDay) }
            .mapNotNull { getCycleDay(it) }
            .toList()
    }

    override fun getBleedingDays(): List<LocalDate> {
        return loggedDays()
            .filter { it.bleeding != null }
            .map { it.date }
            .sorted()
    }

    override fun getPredictedBleedingDays(): List<LocalDate> {
        val prediction = currentPrediction() ?: return emptyList()
        val periodLength = typicalPeriodLength().coerceIn(1, 10)
        return (0 until periodLength).map { prediction.predictedNextPeriod.plusDays(it.toLong()) }
    }

    override fun getLoggedDays(): List<CycleDay> =
        loggedDays().map { it.copy(cycleDayNumber = cycleDayNumber(it.date)) }

    override fun saveCycleDay(day: CycleDay) {
        scope.launch {
            writeLock.withLock {
                val current = snapshot.filterNotNull().first()
                val others = current.loggedDays.filterNot { it.date == day.date }
                val updated = if (day.hasData()) {
                    (others + day.copy(cycleDayNumber = null)).sortedBy { it.date }
                } else {
                    others
                }
                snapshot.value = buildSnapshot(current.profile, updated)
                userPrefs.saveLoggedCycleDays(updated)
            }
        }
    }

    private fun buildOnboardingBleedingDays(profile: UserProfile): List<CycleDay> {
        val start = profile.lastPeriodDate ?: return emptyList()
        val scores = profile.lastPeriodDailyScores
        if (scores.isEmpty()) {
            // Only create days the user explicitly configured via period length.
            val length = profile.averagePeriodLength.takeIf { it > 0 } ?: return emptyList()
            return (0 until length).map { offset ->
                CycleDay(
                    date = start.plusDays(offset.toLong()),
                    bleeding = BleedingIntensity.MEDIUM,
                )
            }
        }
        return scores.mapIndexedNotNull { index, score ->
            val intensity = score.toBleedingIntensity() ?: return@mapIndexedNotNull null
            CycleDay(
                date = start.plusDays(index.toLong()),
                bleeding = intensity,
            )
        }
    }
}

private fun Double.roundTo(decimals: Int): Double {
    var factor = 1.0
    repeat(decimals) { factor *= 10 }
    return (this * factor).roundToInt() / factor
}

private fun Int.toBleedingIntensity(): BleedingIntensity? = when (this) {
    1 -> BleedingIntensity.SPOTTING
    2 -> BleedingIntensity.LIGHT
    3 -> BleedingIntensity.MEDIUM
    4 -> BleedingIntensity.HEAVY
    else -> null
}
