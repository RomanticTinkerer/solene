package com.solene.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solene.app.domain.model.FertilityStatus
import com.solene.app.domain.repository.CycleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * UI state for the Home screen.
 */
data class HomeUiState(
    val todayFormatted: String = "",
    val cycleDayNumber: Int = 0,
    val cycleLength: Int = 0,
    val cycleProgress: Float = 0f,
    val fertilityStatus: FertilityStatus? = null,
    val daysUntilNextPeriod: Int? = null,
    val predictionNote: String? = null,
    val todayLogged: Boolean = false,
    val isLoading: Boolean = false,
)

/**
 * ViewModel for the Home screen — sources data from [CycleRepository].
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cycleRepository.updates.collect { loadHomeData() }
        }
    }

    private fun loadHomeData() {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        val dayNumber = cycleRepository.getCurrentCycleDayNumber() ?: 0
        val cycleLength = cycleRepository.getCurrentCycleLength()
        val progress = cycleRepository.getCycleProgress()
        val fertility = cycleRepository.getFertilityStatus()

        _uiState.value = HomeUiState(
            todayFormatted = today.format(formatter),
            cycleDayNumber = dayNumber,
            cycleLength = cycleLength,
            cycleProgress = progress,
            fertilityStatus = fertility,
            daysUntilNextPeriod = fertility.daysUntilNextPeriod,
            predictionNote = fertility.statusText,
            todayLogged = cycleRepository.getTodayCycleDay()?.hasData() == true,
            isLoading = false,
        )
    }
}
