package com.solene.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.solene.app.ui.calendar.CalendarScreen
import com.solene.app.ui.home.HomeScreen
import com.solene.app.ui.log.DayLogScreen
import com.solene.app.ui.onboarding.OnboardingScreen
import com.solene.app.ui.settings.SettingsScreen
import com.solene.app.ui.stats.StatsScreen
import com.solene.app.ui.you.YouScreen
import java.time.LocalDate

/**
 * Solene navigation graph — five top-level tabs, onboarding, and the day log editor.
 */
object SoleneRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val STATS = "stats"
    const val YOU = "you"
    const val SETTINGS = "settings"
    const val LOG_DATE_ARG = "date"
    const val LOG = "log/{$LOG_DATE_ARG}"

    fun log(date: LocalDate) = "log/$date"
}

@Composable
fun SoleneNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(SoleneRoutes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(SoleneRoutes.HOME) {
                        popUpTo(SoleneRoutes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(SoleneRoutes.HOME) {
            HomeScreen(
                onLogDay = { navController.navigate(SoleneRoutes.log(it)) },
            )
        }

        composable(SoleneRoutes.CALENDAR) {
            CalendarScreen(
                onDayClick = { navController.navigate(SoleneRoutes.log(it)) },
            )
        }

        composable(
            route = SoleneRoutes.LOG,
            arguments = listOf(navArgument(SoleneRoutes.LOG_DATE_ARG) { type = NavType.StringType }),
        ) {
            DayLogScreen(
                onDone = { navController.popBackStack() },
            )
        }

        composable(SoleneRoutes.STATS) {
            StatsScreen()
        }

        composable(SoleneRoutes.YOU) {
            YouScreen()
        }

        composable(SoleneRoutes.SETTINGS) {
            SettingsScreen()
        }
    }
}
