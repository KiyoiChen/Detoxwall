package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DetoxWallLogo
import com.example.ui.screens.ChildModeScreen
import com.example.ui.screens.LimitsScreen
import com.example.ui.screens.SchedulesScreen
import com.example.ui.screens.UsageScreen
import com.example.ui.theme.ChildPurple
import com.example.ui.theme.DetoxMint
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.viewmodel.DetoxViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DetoxViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                val isChildModeActive by viewModel.isChildModeActive.collectAsStateWithLifecycle()
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                var showInfoSheet by remember { mutableStateOf(false) }

                // Tabs matching the exact mockup
                val navItems = listOf(
                    Triple("Home", Icons.Default.Home, "nav_tab_home"),
                    Triple("App Limits", Icons.Default.HourglassEmpty, "nav_tab_limits"),
                    Triple("Usage", Icons.Default.BarChart, "nav_tab_usage"),
                    Triple("Schedules", Icons.Default.Schedule, "nav_tab_schedules")
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = ObsidianBg,
                    topBar = {
                        TopAppBar(
                            title = {
                                if (selectedTabIndex == 2) {
                                    // Usage Insights Header (Direct from phone 2 mockup)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { selectedTabIndex = 0 },
                                            modifier = Modifier.size(36.dp).testTag("back_to_home_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back to Home",
                                                tint = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Usage Insights",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    // Main DetoxWall Header (Direct from phone 1 mockup)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        DetoxWallLogo(
                                            size = 32.dp,
                                            primaryColor = DetoxMint
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "DetoxWall",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            },
                            actions = {
                                if (selectedTabIndex != 2) {
                                    // "Adult Mode" / "Child Mode" Pill Chip
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF0B2527))
                                            .border(1.dp, Color(0xFF0E4447), RoundedCornerShape(16.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isChildModeActive) "Child Mode" else "Adult Mode",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DetoxMint
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Hamburger Menu Icon
                                    IconButton(
                                        onClick = { showInfoSheet = true },
                                        modifier = Modifier.testTag("main_menu_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Menu",
                                            tint = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = ObsidianBg
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF0D121B),
                            tonalElevation = 0.dp,
                            modifier = Modifier.border(
                                width = 1.dp,
                                color = Color(0xFF161E2E)
                            )
                        ) {
                            navItems.forEachIndexed { index, (title, icon, testTag) ->
                                val selected = selectedTabIndex == index
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { selectedTabIndex = index },
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = title,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    modifier = Modifier.testTag(testTag),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DetoxMint,
                                        selectedTextColor = DetoxMint,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = Color(0xFF64748B),
                                        unselectedTextColor = Color(0xFF64748B)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (selectedTabIndex) {
                        0 -> ChildModeScreen(viewModel = viewModel, contentPadding = innerPadding)
                        1 -> LimitsScreen(viewModel = viewModel, contentPadding = innerPadding)
                        2 -> UsageScreen(
                            viewModel = viewModel,
                            contentPadding = innerPadding,
                            onBackToHome = { selectedTabIndex = 0 }
                        )
                        3 -> SchedulesScreen(viewModel = viewModel, contentPadding = innerPadding)
                        else -> ChildModeScreen(viewModel = viewModel, contentPadding = innerPadding)
                    }

                    // Informative Overview BottomSheet from Mockup
                    if (showInfoSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showInfoSheet = false },
                            containerColor = Color(0xFF121722),
                            contentColor = Color.White
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DetoxWallLogo(size = 36.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Calm. Simple. Protective.",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DetoxMint
                                        )
                                        Text(
                                            text = "A safe digital space for your child.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                val features = listOf(
                                    "Child Protection" to "Enables intelligent screen safety with custom limits.",
                                    "Screen Blackout" to "Automatically turns screen black when time is up.",
                                    "Smart Presets" to "Quick time presets with custom slider control.",
                                    "Instant Test" to "Test blackout screen effect anytime.",
                                    "Siren Alarm" to "Loud alarm alerts you if bypass is attempted."
                                )

                                features.forEach { (feat, desc) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(DetoxMint)
                                                .align(Alignment.CenterVertically)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = feat,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = desc,
                                                color = Color(0xFF8E9DAE),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
