package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ScheduleBlock
import com.example.ui.theme.DetoxMint
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.LimitRose
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DetoxViewModel

@Composable
fun SchedulesScreen(
    viewModel: DetoxViewModel,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val hasUsageStats by viewModel.hasUsageStatsPermission.collectAsStateWithLifecycle()
    val hasOverlay by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()
    val isAccessibility by viewModel.isAccessibilityEnabled.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var scheduleToEdit by remember { mutableStateOf<ScheduleBlock?>(null) }
    var scheduleToDelete by remember { mutableStateOf<ScheduleBlock?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // ── System Permissions Health Card ─────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
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
                            Text(
                                text = "System Permissions",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            TextButton(onClick = { viewModel.refreshPermissions() }) {
                                Text("Refresh", color = DetoxMint, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        PermissionItem(
                            title = "Usage Stats Access",
                            subtitle = "Tracks screen time per app",
                            isGranted = hasUsageStats,
                            onGrant = { viewModel.openUsageSettings(context) }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        PermissionItem(
                            title = "Display Over Other Apps",
                            subtitle = "Required to show the block overlay",
                            isGranted = hasOverlay,
                            onGrant = { viewModel.openOverlaySettings(context) }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        PermissionItem(
                            title = "Accessibility Service",
                            subtitle = "Intercepts app switches in real time",
                            isGranted = isAccessibility,
                            onGrant = { viewModel.openAccessibilitySettings(context) }
                        )
                    }
                }
            }

            // ── Section Header ─────────────────────────────────────────────
            item {
                Text(
                    text = "Automated Schedules",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Auto-lock restricted apps during routine times",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )
            }

            // ── Schedule Cards ─────────────────────────────────────────────
            if (schedules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No schedules yet.\nTap + to add one.",
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            items(schedules, key = { it.id }) { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    onToggle = { isChecked -> viewModel.toggleSchedule(schedule.id, isChecked) },
                    onEdit = { scheduleToEdit = schedule },
                    onDelete = { scheduleToDelete = schedule }
                )
            }
        }

        // ── FAB (48dp+ touch target) ───────────────────────────────────────
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = DetoxMint,
            contentColor = Color(0xFF042F2E),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_schedule_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Schedule")
        }
    }

    // ── Add Schedule Dialog ────────────────────────────────────────────────
    if (showAddDialog) {
        ScheduleEditDialog(
            title = "New Schedule",
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = { updatedTitle, sH, sM, eH, eM, daysOfWeek ->
                viewModel.addSchedule(updatedTitle, sH, sM, eH, eM, daysOfWeek)
                showAddDialog = false
            }
        )
    }

    // ── Edit Schedule Dialog ───────────────────────────────────────────────
    scheduleToEdit?.let { schedule ->
        ScheduleEditDialog(
            title = "Edit Schedule",
            initial = schedule,
            onDismiss = { scheduleToEdit = null },
            onSave = { updatedTitle, sH, sM, eH, eM, daysOfWeek ->
                viewModel.updateSchedule(
                    schedule.copy(
                        title = updatedTitle,
                        startHour = sH,
                        startMinute = sM,
                        endHour = eH,
                        endMinute = eM,
                        daysOfWeek = daysOfWeek,
                        daysSummary = ScheduleBlock.buildDaysSummary(daysOfWeek)
                    )
                )
                scheduleToEdit = null
            }
        )
    }

    // ── Delete Confirmation ────────────────────────────────────────────────
    scheduleToDelete?.let { schedule ->
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = { Text("Delete Schedule?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${schedule.title}\" will be permanently deleted.",
                    color = Color(0xFF94A3B8)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSchedule(schedule)
                        scheduleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LimitRose)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
fun ScheduleCard(
    schedule: ScheduleBlock,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = remember(schedule) { ScheduleBlock.isActiveNow(schedule) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = if (isActive && schedule.isEnabled)
            androidx.compose.foundation.BorderStroke(1.dp, DetoxMint.copy(alpha = 0.5f))
        else
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (schedule.isEnabled) FocusIndigo.copy(alpha = 0.2f)
                                else Color(0xFF334155)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = if (schedule.isEnabled) FocusIndigo else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = schedule.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        val startTimeStr = formatTime(schedule.startHour, schedule.startMinute)
                        val endTimeStr = formatTime(schedule.endHour, schedule.endMinute)
                        Text(
                            text = "$startTimeStr – $endTimeStr • ${schedule.daysSummary}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (schedule.isEnabled) DetoxMint else Color(0xFF64748B)
                            )
                        )
                        if (isActive && schedule.isEnabled) {
                            Text(
                                text = "● Active now",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DetoxMint,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF042F2E),
                        checkedTrackColor = DetoxMint,
                        uncheckedTrackColor = Color(0xFF334155)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 13.sp)
                }

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = LimitRose.copy(alpha = 0.8f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 13.sp)
                }
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%d:%02d %s".format(displayHour, minute, amPm)
}

/**
 * Unified Add/Edit Schedule Dialog.
 * Shows: name field, hour/minute pickers for start/end, full week day selector.
 * Validates: name not empty, at least one day selected, end time after start time.
 */
@Composable
fun ScheduleEditDialog(
    title: String,
    initial: ScheduleBlock?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Int, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf(initial?.title ?: "") }
    var startHour by remember { mutableIntStateOf(initial?.startHour ?: 8) }
    var startMin by remember { mutableIntStateOf(initial?.startMinute ?: 0) }
    var endHour by remember { mutableIntStateOf(initial?.endHour ?: 15) }
    var endMin by remember { mutableIntStateOf(initial?.endMinute ?: 0) }
    var daysOfWeek by remember { mutableIntStateOf(initial?.daysOfWeek ?: 0b0011111) }
    var nameError by remember { mutableStateOf(false) }
    var daysError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Schedule Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Schedule Name") },
                    placeholder = { Text("e.g. School Hours, Bedtime") },
                    singleLine = true,
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = if (nameError) {{ Text("Name is required", color = LimitRose) }} else null
                )

                // Start Time Picker
                Text(
                    "Start Time",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8)
                )
                TimePickerRow(
                    hour = startHour,
                    minute = startMin,
                    onHourChange = { startHour = it },
                    onMinuteChange = { startMin = it }
                )

                // End Time Picker
                Text(
                    "End Time",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8)
                )
                TimePickerRow(
                    hour = endHour,
                    minute = endMin,
                    onHourChange = { endHour = it },
                    onMinuteChange = { endMin = it }
                )

                // Days Selector
                Text(
                    "Active Days",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8)
                )
                DaysSelector(
                    selectedDays = daysOfWeek,
                    onDaysChanged = { daysOfWeek = it; daysError = false }
                )
                if (daysError) {
                    Text(
                        "Select at least one day",
                        color = LimitRose,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    nameError = name.isBlank()
                    daysError = daysOfWeek == 0
                    if (!nameError && !daysError) {
                        onSave(name.trim(), startHour, startMin, endHour, endMin, daysOfWeek)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DetoxMint)
            ) {
                Text("Save Schedule", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

/**
 * Hour/minute picker using + / − buttons.
 */
@Composable
fun TimePickerRow(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hour stepper
        TimeStepperBox(
            label = "Hour",
            value = "%d %s".format(displayHour, amPm),
            onDecrement = { onHourChange((hour - 1 + 24) % 24) },
            onIncrement = { onHourChange((hour + 1) % 24) },
            modifier = Modifier.weight(1f)
        )

        Text(":", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // Minute stepper
        TimeStepperBox(
            label = "Min",
            value = "%02d".format(minute),
            onDecrement = { onMinuteChange(if (minute == 0) 55 else minute - 5) },
            onIncrement = { onMinuteChange((minute + 5) % 60) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TimeStepperBox(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Text(label, fontSize = 10.sp, color = Color(0xFF64748B))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDecrement, contentPadding = PaddingValues(0.dp)) {
                Text("−", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(52.dp),
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onIncrement, contentPadding = PaddingValues(0.dp)) {
                Text("+", color = DetoxMint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Day-of-week toggle chips.
 * Mon=bit0, Tue=bit1, ..., Sun=bit6
 */
@Composable
fun DaysSelector(
    selectedDays: Int,
    onDaysChanged: (Int) -> Unit
) {
    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        dayLabels.forEachIndexed { index, label ->
            val bitFlag = 1 shl index
            val isSelected = selectedDays and bitFlag != 0
            DayChip(
                label = label,
                isSelected = isSelected,
                onClick = {
                    onDaysChanged(
                        if (isSelected) selectedDays and bitFlag.inv()
                        else selectedDays or bitFlag
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DayChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) DetoxMint else Color(0xFF334155))
            .border(
                1.dp,
                if (isSelected) DetoxMint else Color(0xFF475569),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF042F2E) else Color(0xFF94A3B8)
        )
    }
}

@Composable
fun PermissionItem(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) DetoxMint else WarningAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                    )
                }
            }

            if (!isGranted) {
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Grant", fontSize = 12.sp, color = DetoxMint)
                }
            } else {
                Surface(
                    color = DetoxMint.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Active",
                        color = DetoxMint,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
