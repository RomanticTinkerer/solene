package com.solene.app.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Health Connect integration for Solene.
 *
 * Reads vitals data (heart rate, SpO2, body temperature, respiratory rate,
 * blood pressure, resting heart rate, steps, sleep) from Health Connect
 * and exposes them as simple data classes for UI consumption.
 */

data class HealthVitals(
    val heartRate: Long? = null,
    val heartRateTimestamp: Instant? = null,
    val restingHeartRate: Long? = null,
    val oxygenSaturation: Double? = null,
    val bodyTemperature: Double? = null,
    val respiratoryRate: Double? = null,
    val systolicBp: Double? = null,
    val diastolicBp: Double? = null,
    val stepsToday: Long? = null,
    val sleepDurationMinutes: Long? = null,
)

enum class HealthConnectAvailability {
    AVAILABLE,
    NOT_INSTALLED,
    NOT_SUPPORTED,
}

class HealthConnectRepository(private val context: Context) {

    companion object {
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(BodyTemperatureRecord::class),
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
            HealthPermission.getReadPermission(RespiratoryRateRecord::class),
            HealthPermission.getReadPermission(BloodPressureRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
        )
    }

    fun checkAvailability(): HealthConnectAvailability {
        val status = HealthConnectClient.getSdkStatus(context)
        return when (status) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    private fun getClient(): HealthConnectClient = HealthConnectClient.getOrCreate(context)

    fun getPermissionContract(): androidx.activity.result.contract.ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    /** Users may grant only some vitals; whatever was granted is shown. */
    suspend fun hasAnyPermission(): Boolean {
        return try {
            val granted = getClient().permissionController.getGrantedPermissions()
            PERMISSIONS.any { it in granted }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reads the latest vitals from Health Connect, looking back up to 24 hours
     * for most records, and up to 7 days for steps and sleep.
     */
    suspend fun readVitals(): HealthVitals {
        val client = getClient()
        val now = Instant.now()
        val last24h = now.minus(24, ChronoUnit.HOURS)
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()

        val heartRate = readLatestHeartRate(client, last24h, now)
        val restingHr = readLatestRestingHeartRate(client, last24h, now)
        val spo2 = readLatestOxygenSaturation(client, last24h, now)
        val temp = readLatestBodyTemperature(client, last24h, now)
        val respRate = readLatestRespiratoryRate(client, last24h, now)
        val bp = readLatestBloodPressure(client, last24h, now)
        val steps = readStepsToday(client, startOfDay, now)
        val sleep = readLatestSleep(client, now.minus(1, ChronoUnit.DAYS), now)

        return HealthVitals(
            heartRate = heartRate?.first,
            heartRateTimestamp = heartRate?.second,
            restingHeartRate = restingHr,
            oxygenSaturation = spo2,
            bodyTemperature = temp,
            respiratoryRate = respRate,
            systolicBp = bp?.first,
            diastolicBp = bp?.second,
            stepsToday = steps,
            sleepDurationMinutes = sleep,
        )
    }

    private suspend fun readLatestHeartRate(
        client: HealthConnectClient, start: Instant, end: Instant,
    ): Pair<Long, Instant>? {
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            response.records
                .flatMap { record -> record.samples.map { it.beatsPerMinute to it.time } }
                .maxByOrNull { it.second }
        } catch (e: Exception) { null }
    }

    private suspend fun readLatestRestingHeartRate(
        client: HealthConnectClient, start: Instant, end: Instant,
    ): Long? {
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = RestingHeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            response.records.lastOrNull()?.beatsPerMinute
        } catch (e: Exception) { null }
    }

    private suspend fun readLatestOxygenSaturation(
        client: HealthConnectClient, start: Instant, end: Instant,
    ): Double? {
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = OxygenSaturationRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            response.records.lastOrNull()?.percentage?.value
        } catch (e: Exception) { null }
    }

    private suspend fun readLatestBodyTemperature(
        client: HealthConnectClient, start: Instant, end: Instant,
    ): Double? {
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = BodyTemperatureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            response.records.lastOrNull()?.temperature?.inCelsius
        } catch (e: Exception) { null }
    }

    private suspend fun readLatestRespiratoryRate(
        client: HealthConnectClient, start: Instant, end: Instant,
    ): Double? {
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = RespiratoryRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            response.records.lastOrNull()?.rate
        } catch (e: Exception) { null }
    }

    private suspend fun readLatestBloodPressure(
        client: HealthConnectClient, start: Instant, end: Instant,
    ): Pair<Double, Double>? {
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = BloodPressureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            response.records.lastOrNull()?.let {
                it.systolic.inMillimetersOfMercury to it.diastolic.inMillimetersOfMercury
            }
        } catch (e: Exception) { null }
    }

    private suspend fun readStepsToday(
        client: HealthConnectClient, start: Instant, end: Instant,
    ): Long? {
        return try {
            // aggregate() de-duplicates overlapping step sources (phone + watch).
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            response[StepsRecord.COUNT_TOTAL]?.takeIf { it > 0 }
        } catch (e: Exception) { null }
    }

    private suspend fun readLatestSleep(
        client: HealthConnectClient, start: Instant, end: Instant,
    ): Long? {
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            val latest = response.records.lastOrNull() ?: return null
            ChronoUnit.MINUTES.between(latest.startTime, latest.endTime)
        } catch (e: Exception) { null }
    }
}
