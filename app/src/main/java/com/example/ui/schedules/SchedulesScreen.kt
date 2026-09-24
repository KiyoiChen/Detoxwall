package com.example.ui.schedules

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Schedule
import com.example.ui.components.SectionCard
import com.example.ui.components.enterAnimation

@Composable
fun SchedulesScreen(viewModel: SchedulesViewModel, contentPadding: PaddingValues) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    // null = closed; id 0 = new schedule.
    var editing by remember { mutableStateOf<Schedule?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.enterAnimation()) {
                    Text("Schedules", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Limited apps are paused during these times.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (schedules.isEmpty()) {
                item { PresetCard(onAdd = viewModel::addPreset, modifier = Modifier.enterAnimation(100)) }
            }
            itemsIndexed(schedules, key = { _, s -> s.schedule.id }) { index, item ->
                ScheduleRow(
                    item = item,
                    onClick = { editing = item.schedule },
                    onToggle = { viewModel.setEnabled(item.schedule, it) },
                    modifier = Modifier.animateItem().enterAnimation(80 + index * 50),
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = Schedule(name = "", startMinute = 21 * 60, endMinute = 7 * 60) },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("New schedule") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 20.dp),
        )
    }

    editing?.let { current ->
        ScheduleEditorSheet(
            initial = current,
            onDismiss = { editing = null },
            onSave = { viewModel.save(it); editing = null },
            onDelete = if (current.id != 0L) ({ viewModel.delete(current); editing = null }) else null,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetCard(onAdd: (Schedule) -> Unit, modifier: Modifier) {
    SectionCard(modifier) {
        Icon(Icons.Filled.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(10.dp))
        Text("Start with a routine", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap one to add it. You can edit it afterwards.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SchedulesViewModel.PRESETS.forEach { preset ->
                AssistChip(onClick = { onAdd(preset) }, label = { Text(preset.name) })
            }
        }
    }
}

@Composable
private fun ScheduleRow(item: ScheduleUi, onClick: () -> Unit, onToggle: (Boolean) -> Unit, modifier: Modifier) {
    val s = item.schedule
    SectionCard(modifier.animateContentSize(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).alpha(if (s.enabled) 1f else 0.55f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.name, style = MaterialTheme.typography.titleMedium)
                    if (item.activeNow) {
                        Spacer(Modifier.width(8.dp))
                        ActiveDot()
                    }
                }
                Text(
                    "${Schedule.formatMinute(s.startMinute)} – ${Schedule.formatMinute(s.endMinute)}" +
                        (if (s.isOvernight) " (next day)" else ""),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    Schedule.daysSummary(s.daysMask) + if (item.activeNow) " · Active now" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.activeNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = s.enabled, onCheckedChange = onToggle)
        }
    }
}

/** Softly pulsing dot shown next to a schedule that is running right now. */
@Composable
private fun ActiveDot() {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    Box(
        Modifier.size(8.dp).clip(CircleShape).alpha(pulse)
            .background(MaterialTheme.colorScheme.primary),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ScheduleEditorSheet(
    initial: Schedule,
    onDismiss: () -> Unit,
    onSave: (Schedule) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial.name) }
    var start by remember { mutableIntStateOf(initial.startMinute) }
    var end by remember { mutableIntStateOf(initial.endMinute) }
    var days by remember { mutableIntStateOf(initial.daysMask) }
    var picking by remember { mutableStateOf<String?>(null) } // "start" / "end"
    val nameError = name.isBlank()
    val daysError = days == 0

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(if (initial.id == 0L) "New schedule" else "Edit schedule", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("Name") },
                placeholder = { Text("e.g. Bedtime") },
                singleLine = true,
                isError = nameError && name.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeField("Starts", start, Modifier.weight(1f)) { picking = "start" }
                TimeField("Ends", end, Modifier.weight(1f)) { picking = "end" }
            }
            if (end <= start) {
                Text(
                    "Runs overnight and ends the next morning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Days", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Schedule.DAY_LABELS.forEachIndexed { i, label ->
                    val bit = 1 shl i
                    FilterChip(
                        selected = days and bit != 0,
                        onClick = { days = days xor bit },
                        label = { Text(label) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { days = Schedule.WEEKDAYS }) { Text("Weekdays") }
                TextButton(onClick = { days = Schedule.WEEKENDS }) { Text("Weekends") }
                TextButton(onClick = { days = Schedule.EVERY_DAY }) { Text("Every day") }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    onSave(initial.copy(name = name.trim(), startMinute = start, endMinute = end, daysMask = days))
                },
                enabled = !nameError && !daysError,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Save") }
            if (onDelete != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Delete schedule", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    picking?.let { which ->
        TimePickerDialog(
            initialMinute = if (which == "start") start else end,
            onDismiss = { picking = null },
            onConfirm = { m -> if (which == "start") start = m else end = m; picking = null },
        )
    }
}

@Composable
private fun TimeField(label: String, minute: Int, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(64.dp), shape = MaterialTheme.shapes.medium) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Schedule.formatMinute(minute), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initialMinute: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinute / 60, initialMinute = initialMinute % 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) },
    )
}
