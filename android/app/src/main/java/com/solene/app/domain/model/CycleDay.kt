package com.solene.app.domain.model

import java.time.LocalDate

/**
 * Core domain model representing a single day in the menstrual cycle.
 *
 * Maps to the original drip. app's cycle-day concept, tracking symptoms
 * like bleeding intensity, mucus quality, temperature, and mood.
 */
data class CycleDay(
    val date: LocalDate,
    val cycleDayNumber: Int? = null,
    val bleeding: BleedingIntensity? = null,
    val temperature: Double? = null,
    /** Kept and shown, but left out of the fertility rules (fever, late or disturbed reading). */
    val temperatureExcluded: Boolean = false,
    val mucus: MucusQuality? = null,
    val cervix: CervixState? = null,
    val mood: Mood? = null,
    val pain: List<PainType> = emptyList(),
    val desire: DesireLevel? = null,
    val note: String? = null,
) {
    /** False when nothing was recorded, so the day can be dropped from storage. */
    fun hasData(): Boolean =
        bleeding != null || temperature != null || mucus != null ||
            (cervix != null && !cervix.isEmpty()) || mood != null || pain.isNotEmpty() ||
            desire != null || !note.isNullOrBlank()
}

enum class BleedingIntensity {
    SPOTTING, LIGHT, MEDIUM, HEAVY
}

/**
 * Sensiplan's categories, ordered by fertility; [nfpValue] is the 0–4 score the
 * symptothermal rules use. Dry and nothing differ by feeling, not by sight.
 */
enum class MucusQuality(val nfpValue: Int) {
    DRY(0), NOTHING(1), MOIST(2), STICKY(3), CREAMY(3), EGG_WHITE(4)
}

data class CervixState(
    val opening: CervixOpening? = null,
    val firmness: CervixFirmness? = null,
    val position: CervixPosition? = null,
) {
    fun isEmpty(): Boolean = opening == null && firmness == null && position == null
}

enum class CervixOpening { CLOSED, MEDIUM, OPEN }
enum class CervixFirmness { FIRM, MEDIUM, SOFT }
enum class CervixPosition { LOW, MEDIUM, HIGH }

enum class Mood {
    HAPPY, SAD, ANXIOUS, ENERGETIC, TIRED, CALM, IRRITABLE
}

enum class PainType {
    CRAMPS, HEADACHE, BACKACHE, BREAST_TENDERNESS, BLOATING, NAUSEA
}

enum class DesireLevel {
    LOW, MEDIUM, HIGH
}
