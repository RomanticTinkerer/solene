package com.solene.app.data.fertility

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Every case holds drip's sympto output for the same cycles. Regenerate with
 * `node scripts/make-sympto-fixture.js`.
 */
class SymptoReferenceTest {

    private val cases = Json.parseToJsonElement(
        File("../../test/fixtures/sympto-reference.json").readText(),
    ).jsonArray.map { it.jsonObject }

    @Test
    fun matchesSymptoOnEveryReferenceCase() {
        val failures = cases.mapNotNull { case ->
            val cycles = case.getValue("cycles").jsonArray.map { cycle ->
                cycle.jsonArray.map { day(it.jsonObject) }
            }
            val status = Sympto.status(
                cycle = cycles.last(),
                previousCycle = cycles.getOrNull(cycles.size - 2),
                earlierCycles = cycles.dropLast(2),
                secondary = SecondarySymptom.valueOf(case.text("secondary").uppercase()),
            )
            val expected = expected(case.getValue("expected").jsonObject)
            val actual = actual(status)
            if (actual == expected) null else "${case.text("name")}\n  expected $expected\n  actual   $actual"
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }

    private fun day(json: JsonObject) = SymptoDay(
        date = LocalDate.parse(json.text("date")),
        bleeding = json["bleeding"]?.jsonPrimitive?.int,
        temperature = json["temperature"]?.jsonPrimitive?.double,
        mucus = json["mucus"]?.jsonPrimitive?.int,
        cervix = (json["cervix"] as? JsonObject)?.let {
            SymptoCervix(it.getValue("opening").jsonPrimitive.int, it.getValue("firmness").jsonPrimitive.int)
        },
    )

    private fun actual(status: SymptoStatus): List<String?> {
        fun SymptoPhase?.text() =
            this?.run { "$start..$end ${days.size} ${days.firstOrNull()?.date}..${days.lastOrNull()?.date}" }
        return listOf(
            status.preOvulatory.text(),
            status.periOvulatory.text(),
            status.postOvulatory.text(),
            status.temperatureShift?.let {
                "${it.rule} ${it.ltl} ${it.firstHighMeasurementDay.date} ${it.evaluationCompleteDay.date}"
            },
            status.secondaryShift?.let { "${it.peakDay.date} ${it.evaluationCompleteDay.date}" },
        )
    }

    private fun expected(json: JsonObject): List<String?> {
        fun part(key: String) = json.getValue(key) as? JsonObject
        fun phase(key: String) = part(key)?.run {
            "${text("start")}..${text("end")} ${text("days")} ${text("first")}..${text("last")}"
        }
        return listOf(
            phase("pre"),
            phase("peri"),
            phase("post"),
            part("temperature")?.run {
                "${text("rule")} ${getValue("ltl").jsonPrimitive.double} ${text("firstHigh")} ${text("complete")}"
            },
            part("secondary")?.run { "${text("peak")} ${text("complete")}" },
        )
    }

    /** JSON null reads as "null", matching how Kotlin prints a missing date. */
    private fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content
}
