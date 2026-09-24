package com.example.ui.apps

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppLimit
import com.example.usage.InstalledApp
import com.example.ui.components.AnimatedCounter
import com.example.ui.components.AppIcon
import com.example.ui.components.SectionCard
import com.example.ui.components.enterAnimation
import com.example.ui.components.formatMinutes
import com.example.ui.components.pressScale
import com.example.ui.theme.LocalExtraColors
import kotlin.math.roundToInt

private val PRESETS = listOf(15, 30, 45, 60, 90, 120)

/** What the limit editor sheet is showing: a new app being added, or an existing limit. */
private sealed interface Editing {
    data class New(val app: InstalledApp) : Editing
    data class Existing(val limit: AppLimit) : Editing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(viewModel: AppsViewModel, contentPadding: PaddingValues) {
    val apps by viewModel.limitedApps.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Editing?>(null) }

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
                Column(Modifier.enterAnimation().padding(bottom = 4.dp)) {
                    Text("App limits", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (apps.isEmpty()) "Pick the apps that take too much of your time."
                        else "${apps.count { it.limit.enabled }} of ${apps.size} limits active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (apps.isEmpty()) {
                item { EmptyState(Modifier.enterAnimation(120)) }
            }
            itemsIndexed(apps, key = { _, a -> a.limit.packageName }) { index, app ->
                LimitRow(
                    app = app,
                    onClick = { editing = Editing.Existing(app.limit) },
                    onToggle = { viewModel.setEnabled(app.limit, it) },
                    modifier = Modifier.animateItem().enterAnimation(80 + index * 50),
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = { viewModel.loadInstalledApps(); showPicker = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add app") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = contentPadding.calculateBottomPadding() + 20.dp),
        )
    }

    if (showPicker) {
        AppPickerSheet(
            viewModel = viewModel,
            onDismiss = { showPicker = false },
            onPick = { showPicker = false; editing = Editing.New(it) },
        )
    }

    editing?.let { current ->
        LimitEditorSheet(
            editing = current,
            onDismiss = { editing = null },
            onSave = { minutes ->
                when (current) {
                    is Editing.New -> viewModel.addLimit(current.app, minutes)
                    is Editing.Existing -> viewModel.updateLimit(current.limit, minutes)
                }
                editing = null
            },
            onDelete = (current as? Editing.Existing)?.let { e -> { viewModel.delete(e.limit); editing = null } },
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    SectionCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.HourglassEmpty, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text("No limits yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Tap \"Add app\" to set a daily limit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LimitRow(app: LimitedAppUi, onClick: () -> Unit, onToggle: (Boolean) -> Unit, modifier: Modifier) {
    val extra = LocalExtraColors.current
    val enabled = app.limit.enabled
    val barColor by animateColorAsState(
        when {
            !enabled -> MaterialTheme.colorScheme.outline
            app.progress >= 1f -> extra.danger
            app.progress >= 0.8f -> extra.warning
            else -> MaterialTheme.colorScheme.primary
        },
        label = "barColor",
    )
    val progress by animateFloatAsState(app.progress.coerceIn(0f, 1f), tween(900), label = "bar")
    val contentAlpha by animateFloatAsState(if (enabled) 1f else 0.55f, label = "alpha")

    SectionCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.limit.packageName, app.limit.appName)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    app.limit.appName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                )
                Text(
                    when {
                        !enabled -> "Paused · ${formatMinutes(app.limit.dailyLimitMinutes)} a day"
                        app.remainingMinutes == 0 -> "Limit reached · ${formatMinutes(app.usedMinutes)} used"
                        else -> "${formatMinutes(app.remainingMinutes)} left of ${formatMinutes(app.allowedMinutes)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(viewModel: AppsViewModel, onDismiss: () -> Unit, onPick: (InstalledApp) -> Unit) {
    val apps by viewModel.pickerApps.collectAsStateWithLifecycle()
    val loading by viewModel.pickerLoading.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        apps.filter { query.isBlank() || it.app.label.contains(query.trim(), ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Add an app", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search apps") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            ) {
                items(filtered, key = { it.app.packageName }) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .pressScale(enabled = !item.alreadyLimited) { onPick(item.app) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(item.app.packageName, item.app.label, size = 40.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                when {
                                    item.alreadyLimited -> "Already limited"
                                    item.usedMinutes > 0 -> "${formatMinutes(item.usedMinutes)} today"
                                    else -> "Not used today"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LimitEditorSheet(
    editing: Editing,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val (packageName, label, initial) = when (editing) {
        is Editing.New -> Triple(editing.app.packageName, editing.app.label, 30)
        is Editing.Existing -> Triple(editing.limit.packageName, editing.limit.appName, editing.limit.dailyLimitMinutes)
    }
    var minutes by remember(editing) { mutableFloatStateOf(initial.toFloat()) }
    val rounded = (minutes / 5f).roundToInt() * 5

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(packageName, label)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(label, style = MaterialTheme.typography.titleLarge)
                    Text("Daily limit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(24.dp))
            AnimatedCounter(
                formatMinutes(rounded),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Slider(
                value = minutes,
                onValueChange = { minutes = it },
                valueRange = AppLimit.MIN_MINUTES.toFloat()..AppLimit.MAX_MINUTES.toFloat(),
                modifier = Modifier.padding(top = 8.dp),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PRESETS) { preset ->
                    FilterChip(
                        selected = rounded == preset,
                        onClick = { minutes = preset.toFloat() },
                        label = { Text(formatMinutes(preset)) },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { onSave(rounded) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (editing is Editing.New) "Add limit" else "Save")
            }
            if (onDelete != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Remove limit", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
