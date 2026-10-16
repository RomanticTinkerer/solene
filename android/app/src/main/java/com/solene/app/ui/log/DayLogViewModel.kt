package com.solene.app.ui.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solene.app.domain.model.BleedingIntensity
import com.solene.app.domain.model.CervixFirmness
import com.solene.app.domain.model.CervixOpening
import com.solene.app.domain.model.CervixPosition
import com.solene.app.domain.model.CervixState
import com.solene.app.domain.model.CycleDay
import com.solene.app.domain.model.DesireLevel
import com.solene.app.domain.model.Mood
import com.solene.app.domain.model.MucusQuality
import com.solene.app.domain.model.PainType
import com.solene.app.domain.repository.CycleRepository
import com.solene.app.navigation.SoleneRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

data class DayLogUiState(
    val date: LocalDate = LocalDate.now(),
    val cycleDayNumber: Int? = null,
    val bleeding: BleedingIntensity? = null,
    val temperature: String = "",
    val temperatureError: String? = null,
    val temperatureExcluded: Boolean = false,
    val mucus: MucusQuality? = null,
    val cervixOpening: CervixOpening? = null,
    val cervixFirmness: CervixFirmness? = null,
    val cervixPosition: CervixPosition? = null,
    val mood: Mood? = null,
    val pain: Set<PainType> = emptySet(),
    val desire: DesireLevel? = null,
    val note: String = "",
    val hasSavedEntry: Boolean = false,
    val isLoading: Boolean = true,
    val isDone: Boolean = false,
)

/**
 * Edits everything logged for one day. Future dates are not loggable, so a
 * date after today opens today instead.
 */
@HiltViewModel
class DayLogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cycleRepository: CycleRepository,
) : ViewModel() {

    private val date: LocalDate = run {
        val today = LocalDate.now()
        val requested = savedStateHandle.get<String>(SoleneRoutes.LOG_DATE_ARG)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (requested == null || requested.isAfter(today)) today else requested
    }

    private val _uiState = MutableStateFlow(DayLogUiState(date = date))
    val uiState: StateFlow<DayLogUiState> = _uiState.asStateFlow()

    /** What was stored when the screen opened, to tell whether leaving would drop edits. */
    private var loaded: DayLogUiState? = null

    init {
        viewModelScope.launch {
            cycleRepository.updates.first()
            val day = cycleRepository.getCycleDay(date)
            val state = DayLogUiState(
                date = date,
                cycleDayNumber = day?.cycleDayNumber,
                bleeding = day?.bleeding,
                temperature = day?.temperature?.toString().orEmpty(),
                temperatureExcluded = day?.temperatureExcluded == true,
                mucus = day?.mucus,
                cervixOpening = day?.cervix?.opening,
                cervixFirmness = day?.cervix?.firmness,
                cervixPosition = day?.cervix?.position,
                mood = day?.mood,
                pain = day?.pain.orEmpty().toSet(),
                desire = day?.desire,
                note = day?.note.orEmpty(),
                hasSavedEntry = day?.hasData() == true,
                isLoading = false,
            )
            loaded = state
            _uiState.value = state
        }
    }

    fun hasUnsavedChanges(): Boolean {
        val state = _uiState.value
        return !state.isDone && loaded?.let { it != state.copy(temperatureError = null) } == true
    }

    private fun update(transform: DayLogUiState.() -> DayLogUiState) {
        _uiState.value = _uiState.value.transform()
    }

    fun toggleBleeding(value: BleedingIntensity) = update { copy(bleeding = value.unlessSame(bleeding)) }

    fun toggleMucus(value: MucusQuality) = update { copy(mucus = value.unlessSame(mucus)) }

    fun toggleCervixOpening(value: CervixOpening) =
        update { copy(cervixOpening = value.unlessSame(cervixOpening)) }

    fun toggleCervixFirmness(value: CervixFirmness) =
        update { copy(cervixFirmness = value.unlessSame(cervixFirmness)) }

    fun toggleCervixPosition(value: CervixPosition) =
        update { copy(cervixPosition = value.unlessSame(cervixPosition)) }

    fun toggleMood(value: Mood) = update { copy(mood = value.unlessSame(mood)) }

    fun togglePain(value: PainType) = update { copy(pain = if (value in pain) pain - value else pain + value) }

    fun toggleDesire(value: DesireLevel) = update { copy(desire = value.unlessSame(desire)) }

    fun setTemperature(text: String) = update { copy(temperature = text, temperatureError = null) }

    fun setTemperatureExcluded(excluded: Boolean) = update { copy(temperatureExcluded = excluded) }

    fun setNote(text: String) = update { copy(note = text) }

    fun save() {
        val state = _uiState.value
        val temperature = parseTemperature(state.temperature).getOrElse {
            update { copy(temperatureError = TEMPERATURE_ERROR) }
            return
        }
        cycleRepository.saveCycleDay(
            CycleDay(
                date = date,
                bleeding = state.bleeding,
                temperature = temperature,
                temperatureExcluded = temperature != null && state.temperatureExcluded,
                mucus = state.mucus,
                cervix = CervixState(state.cervixOpening, state.cervixFirmness, state.cervixPosition)
                    .takeUnless { it.isEmpty() },
                mood = state.mood,
                pain = PainType.entries.filter { it in state.pain },
                desire = state.desire,
                note = state.note.trim().ifEmpty { null },
            ),
        )
        update { copy(isDone = true) }
    }

    fun clear() {
        cycleRepository.saveCycleDay(CycleDay(date = date))
        update { copy(isDone = true) }
    }

    companion object {
        const val MIN_TEMPERATURE = 35.0
        const val MAX_TEMPERATURE = 42.0
        private const val TEMPERATURE_ERROR = "Enter a temperature between 35 and 42 °C"

        /** Celsius rounded to 0.01, null for blank input, failure when out of range. */
        fun parseTemperature(text: String): Result<Double?> {
            val normalized = text.trim().replace(',', '.')
            if (normalized.isEmpty()) return Result.success(null)
            val value = normalized.toDoubleOrNull()
                ?.takeIf { it in MIN_TEMPERATURE..MAX_TEMPERATURE }
                ?: return Result.failure(IllegalArgumentException(TEMPERATURE_ERROR))
            return Result.success((value * 100).roundToInt() / 100.0)
        }
    }
}

private fun <T> T.unlessSame(current: T?): T? = takeUnless { it == current }
