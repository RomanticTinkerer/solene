package com.solene.app.data.prediction

import java.io.File

/** Loads the shipped model assets straight from disk; unit tests run in the module dir. */
object TestModels {
    private val dir = File("src/main/assets/models")

    fun load(name: String): XgbOnDeviceModel = XgbOnDeviceModel.fromJson(
        File(dir, "$name.json").readText(),
        File(dir, "${name}_meta.json").readText(),
    )

    fun engine() = CyclePredictionEngine(
        { load("cycle_length") },
        { load("ovulation_day") },
    )
}
