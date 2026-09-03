package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppLimit
import com.example.ui.blocker.OverlayBlockActivity
import com.example.ui.components.AppPickerModal
import com.example.ui.components.EditLimitDialog
import com.example.ui.theme.DetoxMint
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.LimitRose
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DetoxViewModel

@Composable
fun LimitsScreen(
    viewModel: DetoxViewModel,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val limits by viewModel.appLimits.collectAsStateWithLifecycle()
    val isFocusActive by viewModel.isFocusActive.collectAsStateWithLifecycle()
    val focusRemainingSec by viewModel.focusRemainingSeconds.collectAsStateWithLifecycle()
    val availableApps by viewModel.availableApps.collectAsStateWithLifecycle()

    var showPickerModal by remember { mutableStateOf(false) }
    var appToEdit by remember { mutableStateOf<AppLimit?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(contentPadding)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // Focus Mode Card (Requested feature #5: manual 'block for next 2 hrs' button)
            item {
                FocusModeHeroCard(
                    isFocusActive = isFocusActive,
                    remainingSeconds = focusRemainingSec,
                    onStartFocus = { duration -> viewModel.startFocusSession(duration) },
                    onStopFocus = { viewModel.stopFocusSession() }
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Daily Limits",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "${limits.count { it.isRestricted }} Protected",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = DetoxMint,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            items(limits, key = { it.packageName }) { appLimit ->
                AppLimitCard(
                    appLimit = appLimit,
                    onToggle = { isChecked -> viewModel.toggleAppRestriction(appLimit.packageName, isChecked) },
                    onEdit = { appToEdit = appLimit },
                    onSimulateUsage = { viewModel.simulateUsageMinute(appLimit.packageName) },
                    onTestBlock = {
                        viewModel.previewBlockScreen(
                            context = context,
                            appName = appLimit.appName,
                            packageName = appLimit.packageName,
                            mode = OverlayBlockActivity.MODE_ADULT
                        )
                    }
                )
            }
        }

        // FAB to add new app limit
        FloatingActionButton(
            onClick = { showPickerModal = true },
            containerColor = DetoxMint,
            contentColor = Color(0xFF042F2E),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_app_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add App Limit")
        }
    }

    if (showPickerModal) {
        val existingPackageNames = remember(limits) { limits.map { it.packageName }.toSet() }
        AppPickerModal(
            availableApps = availableApps,
            existingPackageNames = existingPackageNames,
            onDismiss = { showPickerModal = false },
            onAppSelected = { appInfo ->
                viewModel.addNewAppLimit(appInfo, appInfo.defaultLimitMinutes)
                showPickerModal = false
            }
        )
    }

    appToEdit?.let { limit ->
        EditLimitDialog(
            appLimit = limit,
            onDismiss = { appToEdit = null },
            onSave = { newLimit ->
                viewModel.setAppDailyLimit(limit.packageName, newLimit)
                appToEdit = null
            }
        )
    }
}

@Composable
fun FocusModeHeroCard(
    isFocusActive: Boolean,
    remainingSeconds: Long,
    onStartFocus: (Int) -> Unit,
    onStopFocus: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isFocusActive) FocusIndigo else Color(0xFF334155),
                RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFocusActive) FocusIndigo.copy(alpha = 0.2f) else Color(0xFF334155)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isFocusActive) FocusIndigo else DetoxMint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Focus Session",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (isFocusActive) "Deep work active • Distractions locked" else "Instant distraction blocker",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isFocusActive) DetoxMint else Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isFocusActive) {
                val mins = (remainingSeconds / 60).toInt()
                val secs = (remainingSeconds % 60).toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TIME REMAINING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFA5B4FC),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = String.format("%02d:%02d", mins, secs),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                    }

                    Button(
                        onClick = onStopFocus,
                        colors = ButtonDefaults.buttonColors(containerColor = LimitRose),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("stop_focus_button")
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop Session", color = Color.White)
                    }
                }
            } else {
                // Requested specific 2-hr focus session button + quick options
                Button(
                    onClick = { onStartFocus(120) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_focus_2hrs_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = FocusIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Block For Next 2 Hours",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15 to "15m", 30 to "30m", 60 to "1h").forEach { (duration, label) ->
                        OutlinedButton(
                            onClick = { onStartFocus(duration) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1))
                        ) {
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLimitCard(
    appLimit: AppLimit,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onSimulateUsage: () -> Unit,
    onTestBlock: () -> Unit
) {
    val progressColor = when {
        appLimit.isLimitExceeded -> LimitRose
        appLimit.progressPercent >= 0.8f -> WarningAmber
        else -> DetoxMint
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_card_${appLimit.packageName}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appLimit.appName.take(1).uppercase(),
                            color = DetoxMint,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = appLimit.appName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "${appLimit.category} • ${appLimit.dailyLimitMinutes} min/day limit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                Switch(
                    checked = appLimit.isRestricted,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF042F2E),
                        checkedTrackColor = DetoxMint,
                        uncheckedTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("toggle_${appLimit.packageName}")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Usage progress bar
            LinearProgressIndicator(
                progress = { appLimit.progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = Color(0xFF334155),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${appLimit.currentUsageMinutes}m used today",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = progressColor,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Text(
                    text = if (appLimit.isLimitExceeded) "Limit Exceeded" else "${appLimit.remainingMinutes}m remaining",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (appLimit.isLimitExceeded) LimitRose else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2E8F0)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change Limit", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onSimulateUsage,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("+5m Demo", fontSize = 12.sp)
                }

                Button(
                    onClick = onTestBlock,
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp), tint = DetoxMint)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test 5s Wall", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
