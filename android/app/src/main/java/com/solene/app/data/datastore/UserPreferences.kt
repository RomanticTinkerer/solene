package com.solene.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * DataStore-backed user preferences for Solene.
 *
 * Persists onboarding state, personal profile data, logged cycle days,
 * and app settings. All reads are Flow-based; writes are suspend functions.
 */

val Context.soleneDataStore: DataStore<Preferences> by preferencesDataStore(name = "solene_prefs")

data class UserProfile(
    val onboardingCompleted: Boolean = false,
    val lastPeriodDate: LocalDate? = null,
    val averageCycleLength: Int = 0,
    val averagePeriodLength: Int = 0,
    val birthDate: LocalDate? = null,
    val heightCm: Int? = null,
    val weightKg: Float? = null,
    val nfpMethod: String = "Symptothermal",
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "08:00 AM",
    val periodReminderEnabled: Boolean = false,
    /** Start of the cycle the period reminder was last shown for; it fires once per cycle. */
    val periodReminderSentFor: LocalDate? = null,
    val exportFormat: String = "CSV",
    /** Daily bleeding scores for the most recent period (1=spotting … 4=heavy, 0=none). */
    val lastPeriodDailyScores: List<Int> = emptyList(),
    /** User-entered prior completed cycle lengths — never stock/demo data. */
    val priorCycleLengths: List<Int> = emptyList(),
    val typicalOvulationDay: Int? = null,
    val typicalLutealLength: Int? = null,
    val reproductiveCategory: Int = 0,
    val numberOfPregnancies: Int? = null,
    val livingChildren: Int? = null,
    val breastfeeding: Boolean = false,
    val tracksPeakDays: Boolean = true,
)

object PreferenceKeys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val LAST_PERIOD_DATE = stringPreferencesKey("last_period_date")
    val AVG_CYCLE_LENGTH = intPreferencesKey("avg_cycle_length")
    val AVG_PERIOD_LENGTH = intPreferencesKey("avg_period_length")
    val BIRTH_DATE = stringPreferencesKey("birth_date")
    val HEIGHT_CM = intPreferencesKey("height_cm")
    val WEIGHT_KG = floatPreferencesKey("weight_kg")
    val NFP_METHOD = stringPreferencesKey("nfp_method")
    val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    val REMINDER_TIME = stringPreferencesKey("reminder_time")
    val PERIOD_REMINDER_ENABLED = booleanPreferencesKey("period_reminder_enabled")
    val PERIOD_REMINDER_SENT_FOR = stringPreferencesKey("period_reminder_sent_for")
    val EXPORT_FORMAT = stringPreferencesKey("export_format")
    val LAST_PERIOD_DAILY_SCORES = stringPreferencesKey("last_period_daily_scores")
    val PRIOR_CYCLE_LENGTHS = stringPreferencesKey("prior_cycle_lengths")
    val TYPICAL_OVULATION_DAY = intPreferencesKey("typical_ovulation_day")
    val TYPICAL_LUTEAL_LENGTH = intPreferencesKey("typical_luteal_length")
    val REPRODUCTIVE_CATEGORY = intPreferencesKey("reproductive_category")
    val NUMBER_PREGNANCIES = intPreferencesKey("number_pregnancies")
    val LIVING_CHILDREN = intPreferencesKey("living_children")
    val BREASTFEEDING = booleanPreferencesKey("breastfeeding")
    val TRACKS_PEAK_DAYS = booleanPreferencesKey("tracks_peak_days")
    val LOGGED_CYCLE_DAYS = stringPreferencesKey("logged_cycle_days")
}

private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class StoredCycleDay(
    val date: String,
    val cycleDayNumber: Int? = null,
    val bleeding: String? = null,
    val temperature: Double? = null,
    val temperatureExcluded: Boolean = false,
    val mucus: String? = null,
    val cervixOpening: String? = null,
    val cervixFirmness: String? = null,
    val cervixPosition: String? = null,
    val mood: String? = null,
    val pain: List<String> = emptyList(),
    val desire: String? = null,
    val note: String? = null,
)

private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } }

private fun StoredCycleDay.toCycleDay(): CycleDay? {
    val parsedDate = runCatching { LocalDate.parse(date, dateFormatter) }.getOrNull() ?: return null
    val opening = cervixOpening.toEnumOrNull<CervixOpening>()
    val firmness = cervixFirmness.toEnumOrNull<CervixFirmness>()
    val position = cervixPosition.toEnumOrNull<CervixPosition>()
    return CycleDay(
        date = parsedDate,
        cycleDayNumber = cycleDayNumber,
        bleeding = bleeding.toEnumOrNull<BleedingIntensity>(),
        temperature = temperature,
        temperatureExcluded = temperatureExcluded,
        mucus = mucus.toEnumOrNull<MucusQuality>(),
        cervix = CervixState(opening, firmness, position).takeUnless { it.isEmpty() },
        mood = mood.toEnumOrNull<Mood>(),
        pain = pain.mapNotNull { it.toEnumOrNull<PainType>() },
        desire = desire.toEnumOrNull<DesireLevel>(),
        note = note,
    )
}

private fun CycleDay.toStored() = StoredCycleDay(
    date = date.format(dateFormatter),
    cycleDayNumber = cycleDayNumber,
    bleeding = bleeding?.name,
    temperature = temperature,
    temperatureExcluded = temperatureExcluded,
    mucus = mucus?.name,
    cervixOpening = cervix?.opening?.name,
    cervixFirmness = cervix?.firmness?.name,
    cervixPosition = cervix?.position?.name,
    mood = mood?.name,
    pain = pain.map { it.name },
    desire = desire?.name,
    note = note,
)

/**
 * Repository for reading and writing user preferences via DataStore.
 */
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val userProfile: Flow<UserProfile> = dataStore.data.map { prefs ->
        UserProfile(
            onboardingCompleted = prefs[PreferenceKeys.ONBOARDING_COMPLETED] ?: false,
            lastPeriodDate = prefs[PreferenceKeys.LAST_PERIOD_DATE]?.let {
                LocalDate.parse(it, dateFormatter)
            },
            averageCycleLength = prefs[PreferenceKeys.AVG_CYCLE_LENGTH] ?: 0,
            averagePeriodLength = prefs[PreferenceKeys.AVG_PERIOD_LENGTH] ?: 0,
            birthDate = prefs[PreferenceKeys.BIRTH_DATE]?.let {
                LocalDate.parse(it, dateFormatter)
            },
            heightCm = prefs[PreferenceKeys.HEIGHT_CM],
            weightKg = prefs[PreferenceKeys.WEIGHT_KG],
            nfpMethod = prefs[PreferenceKeys.NFP_METHOD] ?: "Symptothermal",
            reminderEnabled = prefs[PreferenceKeys.REMINDER_ENABLED] ?: false,
            reminderTime = prefs[PreferenceKeys.REMINDER_TIME] ?: "08:00 AM",
            periodReminderEnabled = prefs[PreferenceKeys.PERIOD_REMINDER_ENABLED] ?: false,
            periodReminderSentFor = prefs[PreferenceKeys.PERIOD_REMINDER_SENT_FOR]?.let {
                runCatching { LocalDate.parse(it, dateFormatter) }.getOrNull()
            },
            exportFormat = prefs[PreferenceKeys.EXPORT_FORMAT] ?: "CSV",
            lastPeriodDailyScores = prefs[PreferenceKeys.LAST_PERIOD_DAILY_SCORES]
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                .orEmpty(),
            priorCycleLengths = prefs[PreferenceKeys.PRIOR_CYCLE_LENGTHS]
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                .orEmpty(),
            typicalOvulationDay = prefs[PreferenceKeys.TYPICAL_OVULATION_DAY],
            typicalLutealLength = prefs[PreferenceKeys.TYPICAL_LUTEAL_LENGTH],
            reproductiveCategory = prefs[PreferenceKeys.REPRODUCTIVE_CATEGORY] ?: 0,
            numberOfPregnancies = prefs[PreferenceKeys.NUMBER_PREGNANCIES],
            livingChildren = prefs[PreferenceKeys.LIVING_CHILDREN],
            breastfeeding = prefs[PreferenceKeys.BREASTFEEDING] ?: false,
            tracksPeakDays = prefs[PreferenceKeys.TRACKS_PEAK_DAYS] ?: true,
        )
    }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferenceKeys.ONBOARDING_COMPLETED] ?: false
    }

    /** Null until days have been saved once; an empty list means the user cleared them. */
    val loggedCycleDays: Flow<List<CycleDay>?> = dataStore.data.map { prefs ->
        val raw = prefs[PreferenceKeys.LOGGED_CYCLE_DAYS] ?: return@map null
        runCatching { json.decodeFromString<List<StoredCycleDay>>(raw) }
            .getOrDefault(emptyList())
            .mapNotNull { it.toCycleDay() }
    }

    suspend fun completeOnboarding(
        lastPeriodDate: LocalDate,
        avgCycleLength: Int,
        avgPeriodLength: Int,
        birthDate: LocalDate? = null,
        heightCm: Int? = null,
        weightKg: Float? = null,
        lastPeriodDailyScores: List<Int> = emptyList(),
        priorCycleLengths: List<Int> = emptyList(),
        typicalOvulationDay: Int? = null,
        typicalLutealLength: Int? = null,
        reproductiveCategory: Int = 0,
        numberOfPregnancies: Int? = null,
        livingChildren: Int? = null,
        breastfeeding: Boolean = false,
        tracksPeakDays: Boolean = true,
    ) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.ONBOARDING_COMPLETED] = true
            prefs[PreferenceKeys.LAST_PERIOD_DATE] = lastPeriodDate.format(dateFormatter)
            prefs[PreferenceKeys.AVG_CYCLE_LENGTH] = avgCycleLength
            prefs[PreferenceKeys.AVG_PERIOD_LENGTH] = avgPeriodLength
            birthDate?.let { prefs[PreferenceKeys.BIRTH_DATE] = it.format(dateFormatter) }
            heightCm?.let { prefs[PreferenceKeys.HEIGHT_CM] = it }
            weightKg?.let { prefs[PreferenceKeys.WEIGHT_KG] = it }
            prefs[PreferenceKeys.LAST_PERIOD_DAILY_SCORES] =
                lastPeriodDailyScores.joinToString(",")
            prefs[PreferenceKeys.PRIOR_CYCLE_LENGTHS] =
                priorCycleLengths.joinToString(",")
            typicalOvulationDay?.let { prefs[PreferenceKeys.TYPICAL_OVULATION_DAY] = it }
            typicalLutealLength?.let { prefs[PreferenceKeys.TYPICAL_LUTEAL_LENGTH] = it }
            prefs[PreferenceKeys.REPRODUCTIVE_CATEGORY] = reproductiveCategory
            numberOfPregnancies?.let { prefs[PreferenceKeys.NUMBER_PREGNANCIES] = it }
            livingChildren?.let { prefs[PreferenceKeys.LIVING_CHILDREN] = it }
            prefs[PreferenceKeys.BREASTFEEDING] = breastfeeding
            prefs[PreferenceKeys.TRACKS_PEAK_DAYS] = tracksPeakDays
        }
    }

    suspend fun saveLoggedCycleDays(days: List<CycleDay>) {
        val stored = days.map { it.toStored() }
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.LOGGED_CYCLE_DAYS] = json.encodeToString(stored)
        }
    }

    suspend fun updatePersonalInfo(
        heightCm: Int? = null,
        weightKg: Float? = null,
        birthDate: LocalDate? = null,
    ) {
        dataStore.edit { prefs ->
            heightCm?.let { prefs[PreferenceKeys.HEIGHT_CM] = it }
            weightKg?.let { prefs[PreferenceKeys.WEIGHT_KG] = it }
            birthDate?.let { prefs[PreferenceKeys.BIRTH_DATE] = it.format(dateFormatter) }
        }
    }

    suspend fun updateCyclePreferences(
        avgCycleLength: Int,
        avgPeriodLength: Int,
    ) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.AVG_CYCLE_LENGTH] = avgCycleLength
            prefs[PreferenceKeys.AVG_PERIOD_LENGTH] = avgPeriodLength
        }
    }

    suspend fun updateNfpMethod(method: String) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.NFP_METHOD] = method
        }
    }

    suspend fun updateReminder(enabled: Boolean, time: String) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.REMINDER_ENABLED] = enabled
            prefs[PreferenceKeys.REMINDER_TIME] = time
        }
    }

    suspend fun updatePeriodReminder(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.PERIOD_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun markPeriodReminderSent(cycleStart: LocalDate) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.PERIOD_REMINDER_SENT_FOR] = cycleStart.format(dateFormatter)
        }
    }

    suspend fun updateExportFormat(format: String) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.EXPORT_FORMAT] = format
        }
    }
}
