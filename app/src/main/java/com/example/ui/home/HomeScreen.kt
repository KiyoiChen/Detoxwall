package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.AnimatedCounter
import com.example.ui.components.ProgressRing
import com.example.ui.components.SectionCard
import com.example.ui.components.enterAnimation
import com.example.ui.components.formatMinutes
import com.example.ui.theme.LocalExtraColors

data class TopApp(val packageName: String, val label: String, val minutes: Int)

data class HomeUiState(
    val totalMinutes: Int = 0,
    /** Minutes the ring is measured against (child allowance, or the sum of app limits). */
    val goalMinutes: Int = 0,
    val childMode: Boolean = false,
    val focusRemainingSeconds: Int = 0,
    val topApps: List<TopApp> = emptyList(),
    val missingPermissions: Int = 0,
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    contentPadding: PaddingValues,
    onStartFocus: (minutes: Int) -> Unit,
    onStopFocus: () -> Unit,
    onFixPermissions: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.enterAnimation()) {
                Text("Today", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (state.childMode) "Child Mode is on" else "Adult mode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            AnimatedVisibility(
                visible = state.missingPermissions > 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                PermissionBanner(state.missingPermissions, onFixPermissions)
            }
        }

        item { ScreenTimeCard(state, Modifier.enterAnimation(80)) }
        item { FocusCard(state.focusRemainingSeconds, onStartFocus, onStopFocus, Modifier.enterAnimation(160)) }

        if (state.topApps.isNotEmpty()) {
            item {
                Text(
                    "Most used today",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp).enterAnimation(220),
                )
            }
            val max = state.topApps.maxOf { it.minutes }.coerceAtLeast(1)
            itemsIndexed(state.topApps, key = { _, app -> app.packageName }) { index, app ->
                TopAppRow(app, max, Modifier.animateItem().enterAnimation(260 + index * 60))
            }
        }
    }
}

@Composable
private fun PermissionBanner(missing: Int, onFix: () -> Unit) {
    val warning = LocalExtraColors.current.warning
    SectionCard(onClick = onFix) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = warning)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Setup incomplete", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$missing permission${if (missing == 1) "" else "s"} missing. Tap to fix.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ScreenTimeCard(state: HomeUiState, modifier: Modifier) {
    val progress = if (state.goalMinutes > 0) state.totalMinutes.toFloat() / state.goalMinutes else 0f
    val extra = LocalExtraColors.current
    val ringColor = when {
        progress >= 1f -> extra.danger
        progress >= 0.8f -> extra.warning
        else -> MaterialTheme.colorScheme.primary
    }
    SectionCard(modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ProgressRing(progress = progress, color = ringColor) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedCounter(formatMinutes(state.totalMinutes), style = MaterialTheme.typography.displaySmall)
                    Text(
                        if (state.goalMinutes > 0) "of ${formatMinutes(state.goalMinutes)}" else "screen time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusCard(
    remainingSeconds: Int,
    onStart: (Int) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier,
) {
    SectionCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(10.dp))
            Text("Focus session", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (remainingSeconds > 0) {
                AnimatedCounter(
                    "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (remainingSeconds > 0) {
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("End session") }
        } else {
            Text(
                "Pause your limited apps for a while.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 60, 120).forEach { minutes ->
                    FilledTonalButton(onClick = { onStart(minutes) }, modifier = Modifier.weight(1f)) {
                        Text(formatMinutes(minutes), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopAppRow(app: TopApp, maxMinutes: Int, modifier: Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                formatMinutes(app.minutes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { app.minutes.toFloat() / maxMinutes },
            modifier = Modifier.fillMaxWidth().height(6.dp).size(6.dp),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}
