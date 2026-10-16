package com.solene.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solene.app.domain.model.CycleStats
import com.solene.app.domain.repository.CycleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * UI state for the Stats screen. [stats] is null when no completed cycle
 * falls inside the selected range.
 */
data class StatsUiState(
    val stats: CycleStats? = null,
    val selectedTimeRange: TimeRange = TimeRange.ALL_TIME,
    val isLoading: Boolean = false,
)

enum class TimeRange(val label: String, val months: Long?) {
    THREE_MONTHS("3 months", 3),
    SIX_MONTHS("6 months", 6),
    ALL_TIME("All time", null),
    ;

    fun since(today: LocalDate): LocalDate? = months?.let { today.minusMonths(it) }
}

/**
 * ViewModel for the Stats screen.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState(isLoading = true))
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cycleRepository.updates.collect { loadStats() }
        }
    }

    private fun loadStats(range: TimeRange = _uiState.value.selectedTimeRange) {
        _uiState.value = _uiState.value.copy(
            stats = cycleRepository.getCycleStats(range.since(LocalDate.now())),
            selectedTimeRange = range,
            isLoading = false,
        )
    }

    fun selectTimeRange(range: TimeRange) {
        if (_uiState.value.isLoading) {
            _uiState.value = _uiState.value.copy(selectedTimeRange = range)
            return
        }
        loadStats(range)
    }
}
