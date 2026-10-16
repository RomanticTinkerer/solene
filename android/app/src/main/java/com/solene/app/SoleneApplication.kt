package com.solene.app

import android.app.Application
import com.solene.app.notifications.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Solene Application class — required for Hilt dependency injection.
 *
 * @HiltAndroidApp triggers Hilt's code generation, creating a base
 * class that serves as the application-level dependency container.
 */
@HiltAndroidApp
class SoleneApplication : Application() {

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        reminderScheduler.start()
    }
}
