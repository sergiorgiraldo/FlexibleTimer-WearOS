package com.flexibletimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.flexibletimer.data.model.TimerRunState
import com.flexibletimer.presentation.group.GroupViewModel
import com.flexibletimer.presentation.sequential.SequentialViewModel
import com.flexibletimer.service.TimerService
import com.flexibletimer.ui.Screen
import com.flexibletimer.ui.screens.*
import com.flexibletimer.ui.theme.FlexibleTimerTheme
import dagger.hilt.android.AndroidEntryPoint
import android.Manifest
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.flexibletimer.service.TimerAlarmReceiver.Companion.ALERT_CHANNEL_ID

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val channel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Timer Alerts",
            NotificationManager.IMPORTANCE_HIGH
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            FlexibleTimerTheme {
                FlexibleTimerNavHost()
            }
        }
    }
}

@Composable
fun FlexibleTimerNavHost() {
    val navController = rememberSwipeDismissableNavController()
    val runState by TimerService.runState.collectAsStateWithLifecycle()

    // Automatically navigate to running screens when timer starts
    LaunchedEffect(runState) {
        when (runState) {
            is TimerRunState.SequentialRunning -> {
                if (navController.currentDestination?.route != Screen.RunningSequential.route) {
                    navController.navigate(Screen.RunningSequential.route) {
                        launchSingleTop = true
                    }
                }
            }
            is TimerRunState.GroupRunning -> {
                if (navController.currentDestination?.route != Screen.RunningGroup.route) {
                    navController.navigate(Screen.RunningGroup.route) {
                        launchSingleTop = true
                    }
                }
            }
            is TimerRunState.Finished -> { /* RunningScreen shows 'Done' */ }
            is TimerRunState.Idle -> {
                val cur = navController.currentDestination?.route
                if (cur == Screen.RunningSequential.route || cur == Screen.RunningGroup.route) {
                    navController.popBackStack()
                }
            }
        }
    }

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // ── Home ──────────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onSequentialClick = { navController.navigate(Screen.SequentialMenu.route) },
                onGroupClick = { navController.navigate(Screen.GroupMenu.route) }
            )
        }

        // ── Sequential menu ───────────────────────────────────────────────────
        composable(Screen.SequentialMenu.route) {
            val vm: SequentialViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            SequentialMenuScreen(
                hasSaved = state.savedSequences.isNotEmpty(),
                onNewClick = { navController.navigate(Screen.SequentialNew.route) },
                onSavedClick = { navController.navigate(Screen.SequentialSaved.route) }
            )
        }

        // ── Sequential new ────────────────────────────────────────────────────
        composable(Screen.SequentialNew.route) {
            val vm: SequentialViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            SequentialNewScreen(
                uiState = state,
                onTimerCountChange = vm::setTimerCount,
                onUpdateTimer = vm::updateTimer,
                onSave = vm::saveSequence,
                onStart = { timers ->
                    vm.startSequential(timers)
                }
            )
        }

        // ── Sequential saved ──────────────────────────────────────────────────
        composable(Screen.SequentialSaved.route) {
            val vm: SequentialViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            SequentialSavedScreen(
                sequences = state.savedSequences,
                onSelect = { seq -> vm.startSequential(seq.timers) },
                onDelete = { seq -> vm.deleteSequence(seq) }
            )
        }

        // ── Group menu ────────────────────────────────────────────────────────
        composable(Screen.GroupMenu.route) {
            val vm: GroupViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            GroupMenuScreen(
                hasSaved = state.savedGroups.isNotEmpty(),
                onNewClick = { navController.navigate(Screen.GroupNew.route) },
                onSavedClick = { navController.navigate(Screen.GroupSaved.route) }
            )
        }

        // ── Group new ─────────────────────────────────────────────────────────
        composable(Screen.GroupNew.route) {
            val vm: GroupViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            GroupNewScreen(
                uiState = state,
                onTimerCountChange = vm::setTimerCount,
                onUpdateTimer = vm::updateTimer,
                onSave = vm::saveGroup,
                onStart = { timers -> vm.startGroup(timers) }
            )
        }

        // ── Group saved ───────────────────────────────────────────────────────
        composable(Screen.GroupSaved.route) {
            val vm: GroupViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            GroupSavedScreen(
                groups = state.savedGroups,
                onSelect = { group -> vm.startGroup(group.timers) },
                onDelete = { group -> vm.deleteGroup(group) }
            )
        }

        // ── Running sequential ────────────────────────────────────────────────
        composable(Screen.RunningSequential.route) {
            val vm: SequentialViewModel = hiltViewModel()
            when (val s = runState) {
                is TimerRunState.SequentialRunning -> RunningSequentialScreen(
                    state = s,
                    onDoubleTap = {
                        vm.stopTimer()
                        navController.popBackStack(Screen.Home.route, false)
                    }
                )
                is TimerRunState.Finished -> FinishedScreen()
                else -> {}
            }
        }

        // ── Running group ─────────────────────────────────────────────────────
        composable(Screen.RunningGroup.route) {
            val vm: GroupViewModel = hiltViewModel()
            when (val s = runState) {
                is TimerRunState.GroupRunning -> RunningGroupScreen(
                    state = s,
                    onDoubleTap = {
                        vm.stopTimer()
                        navController.popBackStack(Screen.Home.route, false)
                    }
                )
                is TimerRunState.Finished -> FinishedScreen()
                else -> {}
            }
        }
    }
}
