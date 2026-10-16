package com.solene.app.notifications

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.solene.app.MainActivity
import com.solene.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the reminder notifications. Lock screens only see a generic public
 * version, since cycle details are private.
 */
@Singleton
class ReminderNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        manager.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CHANNEL_DAILY, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName("Daily log reminder")
                    .setDescription("A daily nudge to log bleeding, temperature and symptoms")
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_PERIOD, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName("Period reminder")
                    .setDescription("A heads-up a few days before your predicted period")
                    .build(),
            ),
        )
    }

    fun showDailyReminder() {
        val open = Intent(context, MainActivity::class.java)
            .setAction(ACTION_LOG_TODAY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        post(
            id = ID_DAILY,
            notification = builder(CHANNEL_DAILY, open)
                .setContentTitle("Time to log today")
                .setContentText("Bleeding, temperature or how you feel — it only takes a moment.")
                .build(),
        )
    }

    fun showPeriodReminder(nextPeriod: LocalDate, today: LocalDate = LocalDate.now()) {
        val days = ChronoUnit.DAYS.between(today, nextPeriod)
        val title = when {
            days > 1 -> "Your period may start in $days days"
            days == 1L -> "Your period may start tomorrow"
            else -> "Your period may start today"
        }
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        post(
            id = ID_PERIOD,
            notification = builder(CHANNEL_PERIOD, open)
                .setContentTitle(title)
                .setContentText(
                    "Predicted for ${nextPeriod.format(DateTimeFormatter.ofPattern("MMM d"))}. This is an estimate.",
                )
                .build(),
        )
    }

    private fun builder(channel: String, open: Intent): NotificationCompat.Builder {
        val contentIntent = PendingIntent.getActivity(
            context,
            channel.hashCode(),
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val publicVersion = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_solene)
            .setContentTitle("Solene")
            .setContentText("You have a reminder")
            .build()
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_solene)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
    }

    private fun post(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!manager.areNotificationsEnabled()) return
        manager.notify(id, notification)
    }

    companion object {
        const val ACTION_LOG_TODAY = "com.solene.app.action.LOG_TODAY"
        private const val CHANNEL_DAILY = "daily_log"
        private const val CHANNEL_PERIOD = "period"
        private const val ID_DAILY = 1
        private const val ID_PERIOD = 2
    }
}
