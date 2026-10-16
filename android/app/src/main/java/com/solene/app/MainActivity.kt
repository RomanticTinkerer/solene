package com.solene.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solene.app.data.datastore.UserPreferencesRepository
import com.solene.app.data.datastore.soleneDataStore
import com.solene.app.navigation.SoleneNavGraph
import com.solene.app.navigation.SoleneRoutes
import com.solene.app.notifications.ReminderNotifications
import com.solene.app.ui.components.SoleneBottomNav
import com.solene.app.ui.components.SoleneTopBar
import com.solene.app.ui.theme.SoleneTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

/**
 * Single-activity architecture for Solene.
 *
 * On first launch, displays the onboarding flow.
 * After completion, shows the main app with Scaffold,
 * CenterAlignedTopAppBar, NavHost, and 5-tab NavigationBar.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Set when a reminder asks to open today's log; cleared once handled. */
    private var logTodayRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userPrefsRepo = UserPreferencesRepository(applicationContext.soleneDataStore)
        if (savedInstanceState == null) logTodayRequested = intent.requestsLogToday()

        setContent {
            SoleneTheme {
                SoleneApp(
                    userPrefsRepo = userPrefsRepo,
                    logTodayRequested = logTodayRequested,
                    onLogTodayHandled = { logTodayRequested = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.requestsLogToday()) logTodayRequested = true
    }
}

private fun Intent?.requestsLogToday(): Boolean = this?.action == ReminderNotifications.ACTION_LOG_TODAY

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoleneApp(
    userPrefsRepo: UserPreferencesRepository,
    logTodayRequested: Boolean,
    onLogTodayHandled: () -> Unit,
) {
    val navController: NavHostController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Observe onboarding completion state
    val isOnboardingCompleted by userPrefsRepo.isOnboardingCompleted
        .collectAsStateWithLifecycle(initialValue = null)

    // Don't render anything until we know the onboarding state
    if (isOnboardingCompleted == null) return

    val startDestination = if (isOnboardingCompleted == true) {
        SoleneRoutes.HOME
    } else {
        SoleneRoutes.ONBOARDING
    }

    // Hide shell chrome during onboarding
    val isOnboarding = currentRoute == SoleneRoutes.ONBOARDING
    val isLogging = currentRoute == SoleneRoutes.LOG

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val screenTitle = when (currentRoute) {
        SoleneRoutes.HOME -> "Solene"
        SoleneRoutes.CALENDAR -> "Calendar"
        SoleneRoutes.STATS -> "Statistics"
        SoleneRoutes.YOU -> "You"
        SoleneRoutes.SETTINGS -> "Settings"
        SoleneRoutes.LOG -> "Log day"
        else -> "Solene"
    }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!isOnboarding) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                else Modifier
            ),
        topBar = {
            if (!isOnboarding) {
                SoleneTopBar(
                    title = screenTitle,
                    showBack = isLogging,
                    // Through the dispatcher so the day log can ask before dropping edits.
                    onBackClick = { backDispatcher?.onBackPressed() },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        bottomBar = {
            if (!isOnboarding && !isLogging) {
                SoleneBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(SoleneRoutes.HOME) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        SoleneNavGraph(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        )

        // Must sit after the NavHost in this slot so its graph is set before navigating.
        LaunchedEffect(logTodayRequested) {
            if (!logTodayRequested) return@LaunchedEffect
            if (isOnboardingCompleted == true) {
                navController.navigate(SoleneRoutes.log(LocalDate.now())) { launchSingleTop = true }
            }
            onLogTodayHandled()
        }
    }
}
