package com.solene.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solene.app.domain.repository.CycleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * UI state for the Calendar screen.
 */
data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val bleedingDays: Set<LocalDate> = emptySet(),
    /** Days with anything logged, bleeding or not. */
    val loggedDays: Set<LocalDate> = emptySet(),
    val predictedBleedingDays: Set<LocalDate> = emptySet(),
    val today: LocalDate = LocalDate.now(),
)

/**
 * ViewModel for the Calendar screen.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cycleRepository.updates.collect { loadCalendarData() }
        }
    }

    private fun loadCalendarData() {
        _uiState.value = _uiState.value.copy(
            bleedingDays = cycleRepository.getBleedingDays().toSet(),
            loggedDays = cycleRepository.getLoggedDays().map { it.date }.toSet(),
            predictedBleedingDays = cycleRepository.getPredictedBleedingDays().toSet(),
            today = LocalDate.now(),
        )
    }

    fun navigateMonth(delta: Int) {
        _uiState.value = _uiState.value.copy(
            currentMonth = _uiState.value.currentMonth.plusMonths(delta.toLong()),
        )
    }
}
