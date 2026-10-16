package com.solene.app.ui.you

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solene.app.data.datastore.UserPreferencesRepository
import com.solene.app.data.healthconnect.HealthConnectAvailability
import com.solene.app.data.healthconnect.HealthConnectRepository
import com.solene.app.data.healthconnect.HealthVitals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

data class YouUiState(
    val heightCm: String = "",
    val weightKg: String = "",
    val birthDate: LocalDate? = null,
    val averageCycleLength: String = "",
    val averagePeriodLength: String = "",
    val isLoading: Boolean = true,
    val showDatePicker: Boolean = false,
    val snackbarMessage: String? = null,
    // Health Connect
    val healthConnectAvailability: HealthConnectAvailability = HealthConnectAvailability.NOT_SUPPORTED,
    val healthPermissionsGranted: Boolean = false,
    val healthVitals: HealthVitals = HealthVitals(),
    val isLoadingHealth: Boolean = false,
    val healthError: String? = null,
)

@HiltViewModel
class YouViewModel @Inject constructor(
    private val userPrefs: UserPreferencesRepository,
    private val healthConnect: HealthConnectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(YouUiState())
    val uiState: StateFlow<YouUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Only the first emission fills the form, so later DataStore writes
            // don't wipe out fields the user is still editing.
            val profile = userPrefs.userProfile.first()
            _uiState.value = _uiState.value.copy(
                heightCm = profile.heightCm?.toString() ?: "",
                weightKg = profile.weightKg?.let { String.format(Locale.ROOT, "%.1f", it) } ?: "",
                birthDate = profile.birthDate,
                averageCycleLength = profile.averageCycleLength.takeIf { it > 0 }?.toString().orEmpty(),
                averagePeriodLength = profile.averagePeriodLength.takeIf { it > 0 }?.toString().orEmpty(),
                isLoading = false,
            )
        }
        checkHealthConnect()
    }

    private fun checkHealthConnect() {
        val availability = healthConnect.checkAvailability()
        _uiState.value = _uiState.value.copy(healthConnectAvailability = availability)

        if (availability == HealthConnectAvailability.AVAILABLE) {
            viewModelScope.launch {
                val hasPerms = healthConnect.hasAnyPermission()
                _uiState.value = _uiState.value.copy(healthPermissionsGranted = hasPerms)
                if (hasPerms) {
                    loadHealthData()
                }
            }
        }
    }

    fun onPermissionsResult(granted: Set<String>) {
        val hasPerms = HealthConnectRepository.PERMISSIONS.any { it in granted }
        _uiState.value = _uiState.value.copy(healthPermissionsGranted = hasPerms)
        if (hasPerms) {
            loadHealthData()
        }
    }

    fun refreshHealthData() {
        if (_uiState.value.healthPermissionsGranted) {
            loadHealthData()
        }
    }

    private fun loadHealthData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHealth = true, healthError = null)
            try {
                val vitals = healthConnect.readVitals()
                _uiState.value = _uiState.value.copy(
                    healthVitals = vitals,
                    isLoadingHealth = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingHealth = false,
                    healthError = "Could not read health data",
                )
            }
        }
    }

    fun getPermissionContract() = healthConnect.getPermissionContract()

    fun getRequiredPermissions() = HealthConnectRepository.PERMISSIONS

    fun updateHeight(value: String) {
        _uiState.value = _uiState.value.copy(heightCm = value)
    }

    fun updateWeight(value: String) {
        _uiState.value = _uiState.value.copy(weightKg = value)
    }

    fun updateBirthDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(birthDate = date, showDatePicker = false)
        savePersonalInfo()
    }

    fun updateCycleLength(value: String) {
        _uiState.value = _uiState.value.copy(averageCycleLength = value)
    }

    fun updatePeriodLength(value: String) {
        _uiState.value = _uiState.value.copy(averagePeriodLength = value)
    }

    fun toggleDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = !_uiState.value.showDatePicker)
    }

    fun savePersonalInfo() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.birthDate?.isAfter(LocalDate.now()) == true) {
                _uiState.value = _uiState.value.copy(snackbarMessage = "Birth date cannot be in the future")
                return@launch
            }
            userPrefs.updatePersonalInfo(
                heightCm = state.heightCm.trim().toIntOrNull(),
                weightKg = state.weightKg.trim().replace(',', '.').toFloatOrNull(),
                birthDate = state.birthDate,
            )
            _uiState.value = _uiState.value.copy(snackbarMessage = "Saved")
        }
    }

    fun saveCyclePreferences() {
        viewModelScope.launch {
            val state = _uiState.value
            val cycleLen = state.averageCycleLength.trim().toIntOrNull()
            val periodLen = state.averagePeriodLength.trim().toIntOrNull()
            if (cycleLen == null || periodLen == null) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Enter your real cycle and period lengths first",
                )
                return@launch
            }
            if (cycleLen !in 18..60 || periodLen !in 1..10) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Cycle length must be 18–60 days and period length 1–10 days",
                )
                return@launch
            }
            userPrefs.updateCyclePreferences(cycleLen, periodLen)
            _uiState.value = _uiState.value.copy(snackbarMessage = "Saved")
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
