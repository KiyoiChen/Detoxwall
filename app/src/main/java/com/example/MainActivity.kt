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

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private val appsViewModel: AppsViewModel by viewModels()
    private val insightsViewModel: InsightsViewModel by viewModels()
    private val schedulesViewModel: SchedulesViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
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
            DetoxTheme(mode = appSettings.themeMode) {
                Scaffold(
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

    override fun onResume() {
        super.onResume()
        refreshPermissions()
        appsViewModel.refreshUsage()
        insightsViewModel.refresh()
    }
}
