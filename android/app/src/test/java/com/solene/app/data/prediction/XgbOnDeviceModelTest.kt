package com.solene.app.data.prediction

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class XgbOnDeviceModelTest {

    /** Same fixture the JS runtime is checked against in test/prediction-xgboost.spec.js. */
    private val reference = Json.parseToJsonElement(
        File("../../test/fixtures/xgboost-reference.json").readText()
    ).jsonObject

    @Test
    fun matchesThePythonReferenceForBothModels() {
        val assets = mapOf("cycleLength" to "cycle_length", "ovulationDay" to "ovulation_day")
        for ((name, asset) in assets) {
            val model = TestModels.load(asset)
            val cases = reference.getValue(name).jsonObject.getValue("cases").jsonArray
            for (case in cases) {
                val entry = case.jsonObject
                val features = entry.getValue("features").jsonObject
                    .mapValues { it.value.jsonPrimitive.doubleOrNull }
                val expected = entry.getValue("expected").jsonPrimitive.double
                assertEquals(
                    "$name / ${entry["label"]}",
                    expected,
                    model.rawMargin(features),
                    1e-6,
                )
            }
        }
    }

    @Test
    fun readsTheHeldOutErrorFromTheMetadata() {
        assertNotNull(TestModels.load("cycle_length").rmse)
        assertNotNull(TestModels.load("ovulation_day").rmse)
    }
}
