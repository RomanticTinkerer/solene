package com.solene.app.data.prediction

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.max
import kotlin.math.min

/**
 * Portable XGBoost dump format produced by
 * `Training Data (headstart)/export_ondevice_models.py`.
 *
 * Predictions are evaluated fully on-device by walking the exported trees.
 */
@Serializable
data class XgbModelPayload(
    val format: String,
    @SerialName("base_score") val baseScore: Double,
    @SerialName("feature_names") val featureNames: List<String>,
    val trees: List<JsonObject>,
)

@Serializable
data class XgbModelMeta(
    val target: String,
    @SerialName("feature_names") val featureNames: List<String>,
    @SerialName("feature_defaults") val featureDefaults: Map<String, Double?>,
    val clamps: ClampRange,
    val metrics: ModelMetrics? = null,
)

@Serializable
data class ClampRange(
    val min: Double,
    val max: Double,
)

@Serializable
data class ModelMetrics(
    val rmse: Double,
)

class XgbOnDeviceModel(
    private val payload: XgbModelPayload,
    private val meta: XgbModelMeta,
) {
    /** Held-out RMSE in days, or null for models exported without metrics. */
    val rmse: Double? get() = meta.metrics?.rmse

    /** Fills unknown features with the training medians and clamps the result. */
    fun predict(rawFeatures: Map<String, Double?>): Double {
        val features = LinkedHashMap<String, Double?>()
        for (name in payload.featureNames) {
            val provided = rawFeatures[name]?.takeUnless { it.isNaN() }
            features[name] = provided ?: meta.featureDefaults[name]
        }
        return min(meta.clamps.max, max(meta.clamps.min, rawMargin(features)))
    }

    /** Booster output as-is: null or NaN features take each node's missing branch. */
    fun rawMargin(features: Map<String, Double?>): Double {
        var total = payload.baseScore
        for (tree in payload.trees) {
            total += walk(tree, features)
        }
        return total
    }

    private fun walk(node: JsonObject, features: Map<String, Double?>): Double {
        val leaf = node["leaf"]?.jsonPrimitive?.doubleOrNull
        if (leaf != null) return leaf

        val split = node.getString("split")
            ?: error("Tree node missing split")
        val condition = node["split_condition"]?.jsonPrimitive?.doubleOrNull
            ?: error("Tree node missing split_condition")
        val yes = node["yes"]?.jsonPrimitive?.intOrNull
            ?: error("Tree node missing yes")
        val no = node["no"]?.jsonPrimitive?.intOrNull
            ?: error("Tree node missing no")
        val missing = node["missing"]?.jsonPrimitive?.intOrNull ?: no

        val value = features[split]
        val nextId = when {
            value == null || value.isNaN() -> missing
            value < condition -> yes
            else -> no
        }

        val children = node["children"]?.jsonArray
            ?: error("Tree node missing children")
        for (childElement in children) {
            val child = childElement.jsonObject
            if (child["nodeid"]?.jsonPrimitive?.intOrNull == nextId) {
                return walk(child, features)
            }
        }
        error("Missing child node $nextId for split $split")
    }

    private fun JsonObject.getString(key: String): String? {
        val element: JsonElement? = this[key]
        return element?.jsonPrimitive?.contentOrNull
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun fromJson(modelJson: String, metaJson: String): XgbOnDeviceModel {
            return XgbOnDeviceModel(
                payload = json.decodeFromString(XgbModelPayload.serializer(), modelJson),
                meta = json.decodeFromString(XgbModelMeta.serializer(), metaJson),
            )
        }
    }
}
