package com.solene.app.data.prediction

import com.solene.app.data.datastore.UserProfile
import com.solene.app.domain.model.BleedingIntensity
import com.solene.app.domain.model.CycleDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CyclePredictionEngineTest {

    private val engine = TestModels.engine()
    private val start = LocalDate.of(2026, 9, 1)
    private val today = LocalDate.of(2026, 9, 10)

    private fun profile(
        average: Int = 30,
        history: List<Int> = emptyList(),
        ovulationDay: Int? = null,
        lutealLength: Int? = null,
    ) = UserProfile(
        onboardingCompleted = true,
        lastPeriodDate = start,
        averageCycleLength = average,
        averagePeriodLength = 5,
        priorCycleLengths = history,
        typicalOvulationDay = ovulationDay,
        typicalLutealLength = lutealLength,
    )

    private fun predict(profile: UserProfile) =
        requireNotNull(engine.predict(profile, emptyList(), start, today))

    private fun bleedingOn(offset: Int, intensity: BleedingIntensity = BleedingIntensity.MEDIUM) =
        CycleDay(date = start.plusDays(offset.toLong()), bleeding = intensity)

    @Test
    fun keepsTheUsersOwnLengthWithoutEnoughHistory() {
        val prediction = predict(profile(average = 40))
        assertEquals(40, prediction.predictedCycleLength)
        assertEquals(PredictionSource.USER_AVERAGE, prediction.lengthSource)
        assertEquals(start.plusDays(40), prediction.predictedNextPeriod)
    }

    @Test
    fun usesThePersonalMeanFromThreeCyclesEvenWhenTheyVary() {
        val prediction = predict(profile(average = 49, history = listOf(40, 58, 42, 60, 41, 55)))
        assertEquals(PredictionSource.USER_AVERAGE, prediction.lengthSource)
        assertEquals(49, prediction.predictedCycleLength)
    }

    @Test
    fun usesTheModelWithOneOrTwoCycles() {
        for (history in listOf(listOf(40), listOf(26, 31))) {
            val prediction = predict(profile(average = 30, history = history))
            assertEquals(PredictionSource.MODEL, prediction.lengthSource)
            assertEquals(PredictionSource.MODEL, prediction.ovulationSource)
            assertTrue(prediction.predictedCycleLength in CyclePredictionEngine.MIN_CYCLE_LENGTH..CyclePredictionEngine.MAX_CYCLE_LENGTH)
        }
    }

    @Test
    fun anEnteredOvulationDayBeatsTheModel() {
        val prediction = predict(profile(average = 32, history = listOf(32), ovulationDay = 16))
        assertEquals(16, prediction.predictedOvulationDay)
        assertEquals(PredictionSource.USER_ENTERED, prediction.ovulationSource)
    }

    @Test
    fun usesTheEnteredOvulationDayBeforeTheLutealRule() {
        val prediction = predict(profile(average = 32, ovulationDay = 16))
        assertEquals(16, prediction.predictedOvulationDay)
        assertEquals(PredictionSource.USER_ENTERED, prediction.ovulationSource)
    }

    @Test
    fun derivesOvulationFromAnEnteredLutealLength() {
        assertEquals(18, predict(profile(average = 30, lutealLength = 12)).predictedOvulationDay)
    }

    @Test
    fun fallsBackToFourteenDaysBeforeThePeriod() {
        val prediction = predict(profile(average = 32))
        assertEquals(18, prediction.predictedOvulationDay)
        assertEquals(PredictionSource.LUTEAL_ESTIMATE, prediction.ovulationSource)
    }

    @Test
    fun encodesProfileAndBleedingOnTheTrainingScales() {
        val days = listOf(
            BleedingIntensity.HEAVY,
            BleedingIntensity.MEDIUM,
            BleedingIntensity.LIGHT,
            BleedingIntensity.SPOTTING,
        ).mapIndexed { offset, intensity -> bleedingOn(offset, intensity) }
        val features = engine.buildFeatures(
            profile().copy(heightCm = 165, weightKg = 60f),
            days,
            start,
            today,
        )

        assertEquals(65.0, features.getValue("Height")!!, 0.1)
        assertEquals(132.3, features.getValue("Weight")!!, 0.1)
        assertEquals(22.0, features.getValue("BMI")!!, 0.1)
        assertEquals(3.0, features.getValue("MensesScoreDayOne")!!, 0.0)
        assertEquals(2.0, features.getValue("MensesScoreDayTwo")!!, 0.0)
        assertEquals(1.0, features.getValue("MensesScoreDayThree")!!, 0.0)
        assertEquals(1.0, features.getValue("MensesScoreDayFour")!!, 0.0)
        assertEquals(7.0, features.getValue("TotalMensesScore")!!, 0.0)
        assertEquals(7.0, features.getValue("MeanBleedingIntensity")!!, 0.0)
        assertEquals(4.0, features.getValue("LengthofMenses")!!, 0.0)
    }

    @Test
    fun trackedHistoryWinsOverTheStatedAverage() {
        val prediction = requireNotNull(
            engine.predict(profile(average = 28), emptyList(), start, today, history = listOf(35, 36, 35)),
        )
        assertEquals(35, prediction.predictedCycleLength)
        assertEquals(PredictionSource.USER_AVERAGE, prediction.lengthSource)
    }

    @Test
    fun averagesMensesFeaturesOverEveryLoggedPeriod() {
        val earlier = start.minusDays(30)
        val days = listOf(
            CycleDay(date = earlier, bleeding = BleedingIntensity.HEAVY),
            CycleDay(date = earlier.plusDays(1), bleeding = BleedingIntensity.HEAVY),
            bleedingOn(0, BleedingIntensity.LIGHT),
        )
        val features = engine.buildFeatures(
            profile(),
            days,
            start,
            today,
            history = listOf(30),
            periodStarts = listOf(earlier, start),
        )

        assertEquals(1.0, features.getValue("LengthofMenses")!!, 0.0)
        assertEquals(1.5, features.getValue("MeanMensesLength")!!, 0.0)
        assertEquals(1.0, features.getValue("TotalMensesScore")!!, 0.0)
        assertEquals(3.5, features.getValue("MeanBleedingIntensity")!!, 0.0)
    }
}
