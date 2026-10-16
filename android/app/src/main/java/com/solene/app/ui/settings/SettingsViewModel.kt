package com.solene.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solene.app.data.datastore.UserPreferencesRepository
import com.solene.app.data.fertility.FertilitySigns
import com.solene.app.domain.model.CycleDay
import com.solene.app.domain.repository.CycleRepository
import com.solene.app.notifications.ReminderTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class SettingsUiState(
    val dailyReminderEnabled: Boolean = false,
    val periodReminderEnabled: Boolean = false,
    val exportFormat: ExportFormat = ExportFormat.CSV,
    val isExportDropdownExpanded: Boolean = false,
    val reminderTime: LocalTime = ReminderTime.DEFAULT,
    val showTimePicker: Boolean = false,
    val nfpMethod: String = FertilitySigns.MUCUS.label,
    val isNfpDropdownExpanded: Boolean = false,
    val appVersion: String = "1.0.0",
    // Dialogs
    val showAboutDialog: Boolean = false,
    val showLicenseDialog: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    // Export trigger
    val exportDataReady: String? = null,
)

enum class ExportFormat(val label: String) {
    CSV("CSV"),
    JSON("JSON"),
}

val nfpMethods = FertilitySigns.entries.map { it.label }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPreferencesRepository,
    private val cycleRepository: CycleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPrefs.userProfile.collect { profile ->
                _uiState.value = _uiState.value.copy(
                    dailyReminderEnabled = profile.reminderEnabled,
                    periodReminderEnabled = profile.periodReminderEnabled,
                    reminderTime = ReminderTime.parse(profile.reminderTime),
                    nfpMethod = FertilitySigns.fromSetting(profile.nfpMethod).label,
                    exportFormat = ExportFormat.entries.find { it.label == profile.exportFormat }
                        ?: ExportFormat.CSV,
                )
            }
        }
    }

    fun setDailyReminder(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(dailyReminderEnabled = enabled)
        viewModelScope.launch {
            userPrefs.updateReminder(enabled, ReminderTime.format(_uiState.value.reminderTime))
        }
    }

    fun setPeriodReminder(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(periodReminderEnabled = enabled)
        viewModelScope.launch {
            userPrefs.updatePeriodReminder(enabled)
        }
    }

    fun toggleExportDropdown() {
        _uiState.value = _uiState.value.copy(
            isExportDropdownExpanded = !_uiState.value.isExportDropdownExpanded,
        )
    }

    fun selectExportFormat(format: ExportFormat) {
        _uiState.value = _uiState.value.copy(
            exportFormat = format,
            isExportDropdownExpanded = false,
        )
        viewModelScope.launch {
            userPrefs.updateExportFormat(format.label)
        }
    }

    fun triggerExport() {
        viewModelScope.launch {
            cycleRepository.updates.first()
            val days = cycleRepository.getLoggedDays()
            val data = when (_uiState.value.exportFormat) {
                ExportFormat.CSV -> buildCsvExport(days)
                ExportFormat.JSON -> buildJsonExport(days)
            }
            _uiState.value = _uiState.value.copy(exportDataReady = data)
        }
    }

    fun clearExportData() {
        _uiState.value = _uiState.value.copy(exportDataReady = null)
    }

    fun toggleTimePicker() {
        _uiState.value = _uiState.value.copy(showTimePicker = !_uiState.value.showTimePicker)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        val time = LocalTime.of(hour, minute)
        _uiState.value = _uiState.value.copy(reminderTime = time, showTimePicker = false)
        viewModelScope.launch {
            userPrefs.updateReminder(_uiState.value.dailyReminderEnabled, ReminderTime.format(time))
        }
    }

    fun toggleNfpDropdown() {
        _uiState.value = _uiState.value.copy(
            isNfpDropdownExpanded = !_uiState.value.isNfpDropdownExpanded,
        )
    }

    fun selectNfpMethod(method: String) {
        _uiState.value = _uiState.value.copy(nfpMethod = method, isNfpDropdownExpanded = false)
        viewModelScope.launch {
            userPrefs.updateNfpMethod(method)
        }
    }

    fun toggleAboutDialog() {
        _uiState.value = _uiState.value.copy(showAboutDialog = !_uiState.value.showAboutDialog)
    }

    fun toggleLicenseDialog() {
        _uiState.value = _uiState.value.copy(showLicenseDialog = !_uiState.value.showLicenseDialog)
    }

    fun togglePrivacyDialog() {
        _uiState.value = _uiState.value.copy(showPrivacyDialog = !_uiState.value.showPrivacyDialog)
    }

    private fun buildCsvExport(days: List<CycleDay>): String {
        return buildString {
            appendLine("date,cycle_day,bleeding,temperature,temperature_excluded,mucus,cervix,mood,pain,desire,note")
            days.forEach { day ->
                // opening/firmness/position, with an empty slot for anything not recorded
                val cervix = day.cervix?.let {
                    listOf(it.opening, it.firmness, it.position)
                        .joinToString("/") { part -> part?.name?.lowercase().orEmpty() }
                }
                val columns = listOf(
                    day.date.toString(),
                    day.cycleDayNumber?.toString(),
                    day.bleeding?.name?.lowercase(),
                    day.temperature?.toString(),
                    "true".takeIf { day.temperatureExcluded },
                    day.mucus?.name?.lowercase(),
                    cervix,
                    day.mood?.name?.lowercase(),
                    day.pain.joinToString(";") { it.name.lowercase() }.ifEmpty { null },
                    day.desire?.name?.lowercase(),
                    day.note,
                )
                appendLine(columns.joinToString(",") { csvField(it) })
            }
        }
    }

    private fun csvField(value: String?): String {
        if (value == null) return ""
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"${value.replace("\"", "\"\"")}\"" else value
    }

    private fun buildJsonExport(days: List<CycleDay>): String {
        val export = buildJsonObject {
            put("export_date", LocalDate.now().toString())
            put("app", "Solene")
            put("version", _uiState.value.appVersion)
            putJsonArray("cycle_days") {
                days.forEach { day ->
                    addJsonObject {
                        put("date", day.date.toString())
                        day.cycleDayNumber?.let { put("cycle_day", it) }
                        day.bleeding?.let { put("bleeding", it.name.lowercase()) }
                        day.temperature?.let { put("temperature", it) }
                        if (day.temperatureExcluded) put("temperature_excluded", true)
                        day.mucus?.let { put("mucus", it.name.lowercase()) }
                        day.cervix?.let {
                            putJsonObject("cervix") {
                                it.opening?.let { value -> put("opening", value.name.lowercase()) }
                                it.firmness?.let { value -> put("firmness", value.name.lowercase()) }
                                it.position?.let { value -> put("position", value.name.lowercase()) }
                            }
                        }
                        day.mood?.let { put("mood", it.name.lowercase()) }
                        if (day.pain.isNotEmpty()) {
                            putJsonArray("pain") { day.pain.forEach { add(it.name.lowercase()) } }
                        }
                        day.desire?.let { put("desire", it.name.lowercase()) }
                        day.note?.let { put("note", it) }
                    }
                }
            }
        }
        return prettyJson.encodeToString(JsonObject.serializer(), export)
    }

    private companion object {
        val prettyJson = Json { prettyPrint = true }
    }
}
