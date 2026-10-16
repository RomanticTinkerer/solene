package com.solene.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives reminder alarms, plus boot, clock, time zone and app-update
 * broadcasts, after which alarms have to be set again.
 */
class ReminderReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderEntryPoint {
        fun reminderScheduler(): ReminderScheduler
    }

    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = EntryPointAccessors
            .fromApplication(context.applicationContext, ReminderEntryPoint::class.java)
            .reminderScheduler()
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                intent.action?.let { scheduler.onAlarm(it) }
                scheduler.reschedule()
            } finally {
                pending.finish()
            }
        }
    }
}
