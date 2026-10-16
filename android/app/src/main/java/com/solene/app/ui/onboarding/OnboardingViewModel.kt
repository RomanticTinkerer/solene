package com.solene.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solene.app.data.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

enum class BleedingScore(val score: Int, val label: String) {
    NONE(0, "None"),
    SPOTTING(1, "Spotting"),
    LIGHT(2, "Light"),
    MEDIUM(3, "Medium"),
    HEAVY(4, "Heavy"),
}

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 6,
    // Page 1: last period start — null until the user picks a date (no stock offset).
    val lastPeriodDate: LocalDate? = null,
    val showDatePicker: Boolean = false,
    // Page 2: granular daily bleeding for that period.
    val periodDayScores: List<Int> = List(7) { 0 },
    // Page 3: cycle length — null until chosen; no silent 28-day default.
    val averageCycleLength: Int? = null,
    val priorCycleLengthsText: String = "",
    // Page 4: optional ovulation / luteal / reproductive context.
    val typicalOvulationDay: Int? = null,
    val typicalLutealLength: Int? = null,
    val reproductiveCategory: Int = 0,
    val numberOfPregnancies: String = "",
    val livingChildren: String = "",
    val breastfeeding: Boolean = false,
    val tracksPeakDays: Boolean = true,
    // Page 5: personal (optional)
    val heightCm: String = "",
    val weightKg: String = "",
    val birthDate: LocalDate? = null,
    val showBirthDatePicker: Boolean = false,
    val isCompleted: Boolean = false,
    val validationMessage: String? = null,
) {
    val averagePeriodLength: Int
        get() = periodDayScores.indexOfLast { it > 0 }.let { last ->
            if (last >= 0) last + 1 else 0
        }

    private val priorCycleLengthTokens: List<String>
        get() = priorCycleLengthsText
            .split(",", " ", ";", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    val priorCycleLengths: List<Int>
        get() = priorCycleLengthTokens
            .mapNotNull { it.toIntOrNull() }
            .filter { it in CYCLE_LENGTH_RANGE }

    val hasInvalidPriorCycleLengths: Boolean
        get() = priorCycleLengthTokens.any { it.toIntOrNull() !in CYCLE_LENGTH_RANGE }
}

private val CYCLE_LENGTH_RANGE = 18..60
private val OVULATION_DAY_RANGE = 8..40
private val LUTEAL_LENGTH_RANGE = 8..20

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPrefs: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextPage() {
        val state = _uiState.value
        val message = validationForPage(state.currentPage, state)
        if (message != null) {
            _uiState.value = state.copy(validationMessage = message)
            return
        }
        if (state.currentPage < state.totalPages - 1) {
            _uiState.value = state.copy(
                currentPage = state.currentPage + 1,
                validationMessage = null,
            )
        }
    }

    fun previousPage() {
        val current = _uiState.value.currentPage
        if (current > 0) {
            _uiState.value = _uiState.value.copy(
                currentPage = current - 1,
                validationMessage = null,
            )
        }
    }

    fun setLastPeriodDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            lastPeriodDate = date,
            showDatePicker = false,
            validationMessage = null,
        )
    }

    fun toggleDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = !_uiState.value.showDatePicker)
    }

    fun setPeriodDayScore(dayIndex: Int, score: Int) {
        val updated = _uiState.value.periodDayScores.toMutableList()
        if (dayIndex in updated.indices) {
            updated[dayIndex] = score.coerceIn(0, 4)
            _uiState.value = _uiState.value.copy(
                periodDayScores = updated,
                validationMessage = null,
            )
        }
    }

    fun setCycleLength(length: Int?) {
        _uiState.value = _uiState.value.copy(
            averageCycleLength = length,
            validationMessage = null,
        )
    }

    fun setPriorCycleLengthsText(value: String) {
        _uiState.value = _uiState.value.copy(priorCycleLengthsText = value)
    }

    fun setTypicalOvulationDay(day: Int?) {
        _uiState.value = _uiState.value.copy(typicalOvulationDay = day, validationMessage = null)
    }

    fun setTypicalLutealLength(days: Int?) {
        _uiState.value = _uiState.value.copy(typicalLutealLength = days, validationMessage = null)
    }

    fun setReproductiveCategory(value: Int) {
        _uiState.value = _uiState.value.copy(reproductiveCategory = value.coerceIn(0, 5))
    }

    fun setNumberOfPregnancies(value: String) {
        _uiState.value = _uiState.value.copy(numberOfPregnancies = value)
    }

    fun setLivingChildren(value: String) {
        _uiState.value = _uiState.value.copy(livingChildren = value)
    }

    fun setBreastfeeding(value: Boolean) {
        _uiState.value = _uiState.value.copy(breastfeeding = value)
    }

    fun setTracksPeakDays(value: Boolean) {
        _uiState.value = _uiState.value.copy(tracksPeakDays = value)
    }

    fun updateHeight(value: String) {
        _uiState.value = _uiState.value.copy(heightCm = value)
    }

    fun updateWeight(value: String) {
        _uiState.value = _uiState.value.copy(weightKg = value)
    }

    fun setBirthDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(birthDate = date, showBirthDatePicker = false)
    }

    fun toggleBirthDatePicker() {
        _uiState.value = _uiState.value.copy(
            showBirthDatePicker = !_uiState.value.showBirthDatePicker
        )
    }

    fun completeOnboarding() {
        val state = _uiState.value
        val message = validationForCompletion(state)
        if (message != null) {
            _uiState.value = state.copy(validationMessage = message)
            return
        }
        persist(state)
    }

    fun skipPersonalAndComplete() {
        completeOnboarding()
    }

    private fun persist(state: OnboardingUiState) {
        viewModelScope.launch {
            val periodLength = state.averagePeriodLength
            val cycleLength = state.averageCycleLength
                ?: state.priorCycleLengths.averageOrNull()?.roundToInt()
                ?: return@launch
            val start = state.lastPeriodDate ?: return@launch

            // Persist the granular period days as real logged data (not stock).
            // This must happen before completeOnboarding: flipping the flag swaps
            // the nav graph to Home, which clears this ViewModel and its scope.
            val logged = state.periodDayScores
                .take(periodLength)
                .mapIndexedNotNull { index, score ->
                    if (score <= 0) return@mapIndexedNotNull null
                    com.solene.app.domain.model.CycleDay(
                        date = start.plusDays(index.toLong()),
                        cycleDayNumber = index + 1,
                        bleeding = when (score) {
                            1 -> com.solene.app.domain.model.BleedingIntensity.SPOTTING
                            2 -> com.solene.app.domain.model.BleedingIntensity.LIGHT
                            3 -> com.solene.app.domain.model.BleedingIntensity.MEDIUM
                            4 -> com.solene.app.domain.model.BleedingIntensity.HEAVY
                            else -> null
                        },
                    )
                }
            userPrefs.saveLoggedCycleDays(logged)
            userPrefs.completeOnboarding(
                lastPeriodDate = start,
                avgCycleLength = cycleLength,
                avgPeriodLength = periodLength,
                birthDate = state.birthDate,
                heightCm = state.heightCm.trim().toIntOrNull(),
                weightKg = state.weightKg.trim().replace(',', '.').toFloatOrNull(),
                lastPeriodDailyScores = state.periodDayScores.take(periodLength),
                priorCycleLengths = state.priorCycleLengths,
                typicalOvulationDay = state.typicalOvulationDay,
                typicalLutealLength = state.typicalLutealLength,
                reproductiveCategory = state.reproductiveCategory,
                numberOfPregnancies = state.numberOfPregnancies.trim().toIntOrNull(),
                livingChildren = state.livingChildren.trim().toIntOrNull(),
                breastfeeding = state.breastfeeding,
                tracksPeakDays = state.tracksPeakDays,
            )
            _uiState.value = state.copy(isCompleted = true, validationMessage = null)
        }
    }

    private fun validationForPage(page: Int, state: OnboardingUiState): String? = when (page) {
        1 -> when {
            state.lastPeriodDate == null -> "Pick the real start date of your last period"
            state.lastPeriodDate.isAfter(LocalDate.now()) -> "Period start cannot be in the future"
            else -> null
        }
        2 -> if (state.periodDayScores.none { it > 0 }) {
            "Mark bleeding intensity for at least one day of your last period"
        } else null
        3 -> when {
            state.averageCycleLength == null && state.priorCycleLengths.isEmpty() ->
                "Enter your usual cycle length or at least one prior cycle length"
            state.averageCycleLength != null && state.averageCycleLength !in CYCLE_LENGTH_RANGE ->
                "Cycle length must be between 18 and 60 days"
            state.hasInvalidPriorCycleLengths ->
                "Prior cycle lengths must be whole numbers between 18 and 60"
            else -> null
        }
        4 -> when {
            state.typicalOvulationDay != null && state.typicalOvulationDay !in OVULATION_DAY_RANGE ->
                "Typical ovulation day must be between cycle day 8 and 40"
            state.typicalLutealLength != null && state.typicalLutealLength !in LUTEAL_LENGTH_RANGE ->
                "Typical luteal length must be between 8 and 20 days"
            else -> null
        }
        5 -> if (state.birthDate?.isAfter(LocalDate.now()) == true) {
            "Birth date cannot be in the future"
        } else null
        else -> null
    }

    private fun validationForCompletion(state: OnboardingUiState): String? {
        for (page in 1..5) {
            validationForPage(page, state)?.let { return it }
        }
        return null
    }
}

private fun List<Int>.averageOrNull(): Double? =
    if (isEmpty()) null else average()
