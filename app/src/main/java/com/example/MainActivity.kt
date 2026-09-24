package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.apps.AppsScreen
import com.example.ui.apps.AppsViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.gate.PinGateScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.schedules.SchedulesScreen
import com.example.ui.schedules.SchedulesViewModel
import com.example.ui.insights.InsightsScreen
import com.example.ui.insights.InsightsViewModel
import com.example.ui.permissions.PermissionsScreen
import com.example.ui.permissions.currentPermissions
import com.example.usage.PermissionStatus
import com.example.ui.theme.DetoxTheme

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    APPS("Apps", Icons.Filled.Apps),
    SCHEDULES("Schedules", Icons.Filled.Schedule),
    INSIGHTS("Insights", Icons.Filled.BarChart),
    SETTINGS("Settings", Icons.Filled.Settings),
}

private enum class Root { ONBOARDING, GATE, MAIN }

private const val RELOCK_AFTER_MS = 30_000L

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private val appsViewModel: AppsViewModel by viewModels()
    private val insightsViewModel: InsightsViewModel by viewModels()
    private val schedulesViewModel: SchedulesViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val unlocked = androidx.compose.runtime.mutableStateOf(false)
    private var stoppedAt = 0L
    private val permissionStatuses = androidx.compose.runtime.mutableStateOf<List<PermissionStatus>>(emptyList())

    private fun refreshPermissions() {
        permissionStatuses.value = currentPermissions(this)
        homeViewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = container.settings
        setContent {
            val appSettings by settings.settings.collectAsStateWithLifecycle()
            var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
            var showPermissions by rememberSaveable { mutableStateOf(false) }
            BackHandler(enabled = showPermissions) { showPermissions = false }
            val pinSet by container.pinManager.isPinSet.collectAsStateWithLifecycle()
            val screen = when {
                !appSettings.onboardingDone -> Root.ONBOARDING
                pinSet && appSettings.requirePinOnOpen && !unlocked.value -> Root.GATE
                else -> Root.MAIN
            }
            DetoxTheme(mode = appSettings.themeMode) {
              AnimatedContent(
                targetState = screen,
                transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
                label = "root",
              ) { root ->
                when (root) {
                Root.ONBOARDING -> OnboardingScreen(
                    pinManager = container.pinManager,
                    permissions = permissionStatuses.value,
                    onPermissionsChanged = ::refreshPermissions,
                    onFinished = {
                        unlocked.value = true
                        settings.update { it.copy(onboardingDone = true) }
                    },
                )
                Root.GATE -> PinGateScreen(container.pinManager) { unlocked.value = true }
                Root.MAIN -> Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Tab.entries.forEach { t ->
                                NavigationBarItem(
                                    selected = tab == t,
                                    onClick = { tab = t; showPermissions = false },
                                    icon = { Icon(t.icon, contentDescription = null) },
                                    label = { Text(t.label) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    if (showPermissions) {
                        PermissionsScreen(permissionStatuses.value, padding, onChanged = ::refreshPermissions)
                        return@Scaffold
                    }
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                            (slideInHorizontally(tween(320)) { it / 6 * dir } + fadeIn(tween(320))) togetherWith
                                (slideOutHorizontally(tween(240)) { -it / 6 * dir } + fadeOut(tween(200)))
                        },
                        label = "tabs",
                    ) { current ->
                        when (current) {
                            Tab.HOME -> {
                                val home by homeViewModel.state.collectAsStateWithLifecycle()
                                HomeScreen(
                                    state = home,
                                    contentPadding = padding,
                                    onStartFocus = homeViewModel::startFocus,
                                    onStopFocus = homeViewModel::stopFocus,
                                    onFixPermissions = { refreshPermissions(); showPermissions = true },
                                )
                            }
                            Tab.APPS -> AppsScreen(appsViewModel, padding)
                            Tab.SCHEDULES -> SchedulesScreen(schedulesViewModel, padding)
                            Tab.INSIGHTS -> InsightsScreen(insightsViewModel, padding)
                            Tab.SETTINGS -> SettingsScreen(settingsViewModel, padding) {
                                refreshPermissions(); showPermissions = true
                            }
                        }
                    }
                }
                }
              }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Ask for the PIN again if the app was in the background for a while
        // (short trips, e.g. to a permission page, don't count).
        if (stoppedAt != 0L && System.currentTimeMillis() - stoppedAt > RELOCK_AFTER_MS) unlocked.value = false
        stoppedAt = 0L
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) stoppedAt = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
        appsViewModel.refreshUsage()
        insightsViewModel.refresh()
    }
}
