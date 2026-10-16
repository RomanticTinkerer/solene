package com.solene.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.solene.app.data.datastore.UserPreferencesRepository
import com.solene.app.domain.repository.CycleRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps one alarm per reminder in step with the settings and the latest
 * prediction. Alarms are inexact but allowed in Doze, so no exact-alarm
 * permission is needed; a reminder can arrive a few minutes late.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefs: UserPreferencesRepository,
    private val cycleRepository: CycleRepository,
    private val notifications: ReminderNotifications,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    /** Reschedules whenever settings or logged data change, for as long as the process lives. */
    fun start() {
        notifications.createChannels()
        scope.launch {
            cycleRepository.updates.collectLatest { reschedule() }
        }
    }

    suspend fun reschedule(now: ZonedDateTime = ZonedDateTime.now()) {
        cycleRepository.updates.first()
        val profile = userPrefs.userProfile.first()
        val time = ReminderTime.parse(profile.reminderTime)
        val active = profile.onboardingCompleted

        setAlarm(
            ACTION_DAILY,
            REQUEST_DAILY,
            if (active && profile.reminderEnabled) ReminderSchedule.nextDaily(now, time) else null,
        )
        val sentThisCycle = currentCycleStart()?.let { it == profile.periodReminderSentFor } == true
        setAlarm(
            ACTION_PERIOD,
            REQUEST_PERIOD,
            if (active && profile.periodReminderEnabled && !sentThisCycle) {
                ReminderSchedule.periodReminder(now, cycleRepository.getFertilityStatus().predictedNextPeriod, time)
            } else {
                null
            },
        )
    }

    /** Posts the reminder for a fired alarm unless it was turned off or is no longer needed. */
    suspend fun onAlarm(action: String) {
        cycleRepository.updates.first()
        val profile = userPrefs.userProfile.first()
        when (action) {
            ACTION_DAILY -> {
                val alreadyLogged = cycleRepository.getTodayCycleDay()?.hasData() == true
                if (profile.reminderEnabled && !alreadyLogged) notifications.showDailyReminder()
            }
            ACTION_PERIOD -> {
                val nextPeriod = cycleRepository.getFertilityStatus().predictedNextPeriod
                val cycleStart = currentCycleStart()
                // A later prediction can move the alarm forward again within the same cycle.
                if (profile.periodReminderEnabled && nextPeriod != null && cycleStart != profile.periodReminderSentFor) {
                    notifications.showPeriodReminder(nextPeriod)
                    cycleStart?.let { userPrefs.markPeriodReminderSent(it) }
                }
            }
        }
    }

    private fun currentCycleStart(): LocalDate? =
        cycleRepository.getCurrentCycleDayNumber()?.let { LocalDate.now().minusDays(it - 1L) }

    private fun setAlarm(action: String, requestCode: Int, at: ZonedDateTime?) {
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        if (at == null) {
            alarmManager.cancel(pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), pending)
        }
    }

    companion object {
        const val ACTION_DAILY = "com.solene.app.action.DAILY_REMINDER"
        const val ACTION_PERIOD = "com.solene.app.action.PERIOD_REMINDER"
        private const val REQUEST_DAILY = 1
        private const val REQUEST_PERIOD = 2
    }
}
