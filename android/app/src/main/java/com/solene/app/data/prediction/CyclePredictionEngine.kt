package com.solene.app.data.prediction

import android.content.Context
import com.solene.app.data.datastore.UserProfile
import com.solene.app.domain.model.BleedingIntensity
import com.solene.app.domain.model.CycleDay
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class PredictionSource {
    /** On-device XGBoost model, used while the user has one or two completed cycles. */
    MODEL,
    /** The user's own average or prior cycle lengths. */
    USER_AVERAGE,
    /** A typical ovulation day or luteal length the user entered. */
    USER_ENTERED,
    /** Typical 14-day luteal phase counted back from the next period. */
    LUTEAL_ESTIMATE,
}

data class CyclePrediction(
    val predictedCycleLength: Int,
    val predictedOvulationDay: Int,
    val predictedNextPeriod: LocalDate,
    val lengthSource: PredictionSource,
    val ovulationSource: PredictionSource,
) {
    val usedOnDeviceModel: Boolean
        get() = lengthSource == PredictionSource.MODEL ||
            ovulationSource == PredictionSource.MODEL
}

/**
 * On-device personalized cycle prediction backbone.
 *
 * Person-grouped cross-validation (export_ondevice_models.py) shows the
 * models beat the user's own mean only while they have one or two completed
 * cycles; from MIN_CYCLES_FOR_PERSONAL_MEAN on, the mean is as good or
 * better, so it wins. With no completed cycle the stated average is used.
 */
@Singleton
class CyclePredictionEngine internal constructor(
    loadCycleLengthModel: () -> XgbOnDeviceModel?,
    loadOvulationModel: () -> XgbOnDeviceModel?,
) {
    @Inject constructor(@ApplicationContext context: Context) : this(
        { loadModel(context, "models/cycle_length.json", "models/cycle_length_meta.json") },
        { loadModel(context, "models/ovulation_day.json", "models/ovulation_day_meta.json") },
    )

    private val cycleLengthModel: XgbOnDeviceModel? by lazy(loadCycleLengthModel)
    private val ovulationModel: XgbOnDeviceModel? by lazy(loadOvulationModel)

    /**
     * @param history completed cycle lengths, oldest first: the ones entered at
     *   setup followed by the ones logged since.
     * @param periodStarts first day of every known period, oldest first.
     */
    fun predict(
        profile: UserProfile,
        loggedDays: List<CycleDay>,
        cycleStart: LocalDate? = profile.lastPeriodDate,
        today: LocalDate = LocalDate.now(),
        history: List<Int> = profile.priorCycleLengths,
        periodStarts: List<LocalDate> = listOfNotNull(cycleStart),
    ): CyclePrediction? {
        val start = cycleStart ?: return null
        val baseLength = history.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
            ?: profile.averageCycleLength.takeIf { it > 0 }
            ?: return null
        val features = buildFeatures(profile, loggedDays, start, today, history, periodStarts)

        val modelLength = cycleLengthModel
            ?.takeIf { usesModel(history) }
            ?.predict(features)
            ?.roundToInt()
        val cycleLength = (modelLength ?: baseLength).coerceIn(MIN_CYCLE_LENGTH, MAX_CYCLE_LENGTH)
        val lengthSource = if (modelLength != null) PredictionSource.MODEL else PredictionSource.USER_AVERAGE

        val (ovulationDay, ovulationSource) = estimateOvulation(profile, history, features, cycleLength)

        return CyclePrediction(
            predictedCycleLength = cycleLength,
            predictedOvulationDay = ovulationDay,
            predictedNextPeriod = start.plusDays(cycleLength.toLong()),
            lengthSource = lengthSource,
            ovulationSource = ovulationSource,
        )
    }

    private fun estimateOvulation(
        profile: UserProfile,
        history: List<Int>,
        features: Map<String, Double?>,
        cycleLength: Int,
    ): Pair<Int, PredictionSource> {
        val latest = (cycleLength - 1).coerceAtLeast(MIN_OVULATION_DAY)

        profile.typicalOvulationDay?.let {
            return it.coerceIn(MIN_OVULATION_DAY, latest) to PredictionSource.USER_ENTERED
        }
        profile.typicalLutealLength?.let {
            return (cycleLength - it).coerceIn(MIN_OVULATION_DAY, latest) to
                PredictionSource.USER_ENTERED
        }

        val modelDay = ovulationModel
            ?.takeIf { usesModel(history) }
            ?.predict(features)
            ?.roundToInt()
        if (modelDay != null) {
            return modelDay.coerceIn(MIN_OVULATION_DAY, min(MAX_OVULATION_DAY, latest)) to
                PredictionSource.MODEL
        }
        return (cycleLength - TYPICAL_LUTEAL_LENGTH).coerceIn(MIN_OVULATION_DAY, latest) to
            PredictionSource.LUTEAL_ESTIMATE
    }

    private fun usesModel(history: List<Int>) = history.size in 1 until MIN_CYCLES_FOR_PERSONAL_MEAN

    fun buildFeatures(
        profile: UserProfile,
        loggedDays: List<CycleDay>,
        cycleStart: LocalDate,
        today: LocalDate,
        history: List<Int> = profile.priorCycleLengths,
        periodStarts: List<LocalDate> = listOf(cycleStart),
    ): Map<String, Double?> {
        val menses = CycleHistory.mensesRun(loggedDays, cycleStart)
        val mensesScores = (0 until 7).map { offset ->
            val date = cycleStart.plusDays(offset.toLong())
            menses.find { it.date == date }?.bleeding.toMensesScore()
        }

        val lengthOfMenses = when {
            menses.isNotEmpty() -> menses.size.toDouble()
            profile.averagePeriodLength > 0 -> profile.averagePeriodLength.toDouble()
            else -> null
        }
        val totalMensesScore = totalScore(menses)

        val pastPeriods = periodStarts
            .map { CycleHistory.mensesRun(loggedDays, it) }
            .filter { it.isNotEmpty() }
        val meanMensesLength = pastPeriods.map { it.size }.takeIf { it.isNotEmpty() }?.average()
            ?: lengthOfMenses
        // The dataset's MeanBleedingIntensity is the mean of per-cycle total scores.
        val meanBleedingIntensity = pastPeriods.mapNotNull { totalScore(it) }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?: totalMensesScore

        val age = profile.birthDate?.let {
            Period.between(it, today).years.toDouble()
        }

        // The NFP dataset records height in inches and weight in pounds.
        val heightCm = profile.heightCm?.toDouble()
        val weightKg = profile.weightKg?.toDouble()
        val bmi = if (heightCm != null && weightKg != null && heightCm > 0) {
            val meters = heightCm / 100.0
            weightKg / (meters * meters)
        } else {
            null
        }

        val statedLength = profile.averageCycleLength.takeIf { it > 0 }?.toDouble()
        val lastLength = history.lastOrNull()?.toDouble() ?: statedLength
        val meanLength = history.takeIf { it.isNotEmpty() }?.average() ?: statedLength

        val lutealGuess = profile.typicalLutealLength?.toDouble()
        val ovulationGuess = profile.typicalOvulationDay?.toDouble()
            ?: lastLength?.let { it - TYPICAL_LUTEAL_LENGTH }

        return mapOf(
            "LengthofCycle" to lastLength,
            "MeanCycleLength" to meanLength,
            "EstimatedDayofOvulation" to ovulationGuess,
            "LengthofLutealPhase" to lutealGuess,
            "LengthofMenses" to lengthOfMenses,
            "MeanMensesLength" to meanMensesLength,
            "TotalMensesScore" to totalMensesScore,
            "MeanBleedingIntensity" to meanBleedingIntensity,
            "MensesScoreDayOne" to mensesScores[0],
            "MensesScoreDayTwo" to mensesScores[1],
            "MensesScoreDayThree" to mensesScores[2],
            "MensesScoreDayFour" to mensesScores[3],
            "MensesScoreDayFive" to mensesScores[4],
            "MensesScoreDaySix" to mensesScores[5],
            "MensesScoreDaySeven" to mensesScores[6],
            "Age" to age,
            "Height" to heightCm?.let { it * INCHES_PER_CM },
            "Weight" to weightKg?.let { it * POUNDS_PER_KG },
            "BMI" to bmi,
            "CycleWithPeakorNot" to if (profile.tracksPeakDays) 1.0 else 0.0,
            "ReproductiveCategory" to profile.reproductiveCategory.toDouble(),
            "Numberpreg" to profile.numberOfPregnancies?.toDouble(),
            "Livingkids" to profile.livingChildren?.toDouble(),
            "Breastfeeding" to if (profile.breastfeeding) 1.0 else 0.0,
        )
    }

    companion object {
        const val MIN_CYCLES_FOR_PERSONAL_MEAN = 3
        const val TYPICAL_LUTEAL_LENGTH = 14
        const val MIN_CYCLE_LENGTH = 18
        const val MAX_CYCLE_LENGTH = 60
        const val MIN_OVULATION_DAY = 8
        const val MAX_OVULATION_DAY = 40
        private const val INCHES_PER_CM = 0.393701
        private const val POUNDS_PER_KG = 2.20462

        private fun loadModel(context: Context, modelAsset: String, metaAsset: String): XgbOnDeviceModel? {
            return try {
                val modelJson = context.assets.open(modelAsset).bufferedReader().use { it.readText() }
                val metaJson = context.assets.open(metaAsset).bufferedReader().use { it.readText() }
                XgbOnDeviceModel.fromJson(modelJson, metaJson)
            } catch (_: Exception) {
                null
            }
        }

        fun sampleStandardDeviation(values: List<Int>): Double {
            if (values.size < 2) return 0.0
            val mean = values.average()
            val sumOfSquares = values.sumOf { (it - mean) * (it - mean) }
            return sqrt(sumOfSquares / (values.size - 1))
        }
    }
}

private fun totalScore(period: List<CycleDay>): Double? =
    period.mapNotNull { it.bleeding.toMensesScore() }.takeIf { it.isNotEmpty() }?.sum()

/** drip's four bleeding levels on the dataset's 1 (light) to 3 (heavy) scale. */
private fun BleedingIntensity?.toMensesScore(): Double? = when (this) {
    BleedingIntensity.SPOTTING -> 1.0
    BleedingIntensity.LIGHT -> 1.0
    BleedingIntensity.MEDIUM -> 2.0
    BleedingIntensity.HEAVY -> 3.0
    null -> null
}
