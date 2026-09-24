package com.example.ui.insights

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AnimatedCounter
import com.example.ui.components.AppIcon
import com.example.ui.components.SectionCard
import com.example.ui.components.enterAnimation
import com.example.ui.components.formatMinutes
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy")
private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM")

@Composable
fun InsightsScreen(viewModel: InsightsViewModel, contentPadding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.enterAnimation()) {
                Text("Insights", style = MaterialTheme.typography.headlineMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::previousMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    AnimatedContent(
                        targetState = state.month,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "month",
                        modifier = Modifier.weight(1f),
                    ) { m ->
                        Text(
                            m.format(MONTH_FORMAT),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    IconButton(onClick = viewModel::nextMonth, enabled = state.canGoForward) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.enterAnimation(60)) {
                StatCard("This month", formatMinutes(state.monthTotal), Modifier.weight(1f))
                StatCard("Daily average", formatMinutes(state.dailyAverage), Modifier.weight(1f))
            }
        }

        item {
            SectionCard(Modifier.enterAnimation(120)) {
                Text("Screen time per day", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tap a bar to see that day's apps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                MonthBarChart(
                    days = state.days,
                    daysInMonth = state.month.lengthOfMonth(),
                    selected = state.selectedDate,
                    onSelect = viewModel::selectDay,
                )
            }
        }

        item {
            Column(Modifier.padding(top = 4.dp)) {
                Text(state.selectedDate.format(DAY_FORMAT), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (state.selectedApps.isEmpty()) "No usage recorded" else "${formatMinutes(state.selectedTotal)} across ${state.selectedApps.size} apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val max = state.selectedApps.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
        itemsIndexed(state.selectedApps, key = { _, a -> "${state.selectedDate}-${a.packageName}" }) { index, app ->
            AppUsageRow(app, max, Modifier.animateItem().enterAnimation(index * 40))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    SectionCard(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        AnimatedCounter(value, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun MonthBarChart(
    days: List<DayTotal>,
    daysInMonth: Int,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val max = (days.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(30).toFloat()

    // Bars grow from zero each time the month's data changes.
    val growth = remember(days) { Animatable(0f) }
    LaunchedEffect(days) { growth.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(days, daysInMonth) {
                detectTapGestures { offset ->
                    val slot = size.width / daysInMonth.toFloat()
                    val index = (offset.x / slot).toInt().coerceIn(0, daysInMonth - 1)
                    days.getOrNull(index)?.let { onSelect(it.date) }
                }
            },
    ) {
        val slot = size.width / daysInMonth
        val barWidth = slot * 0.62f
        for (i in 0 until daysInMonth) {
            val x = i * slot + (slot - barWidth) / 2
            drawRoundRect(track, Offset(x, 0f), Size(barWidth, size.height), CornerRadius(barWidth / 2))
            val day = days.getOrNull(i) ?: continue
            if (day.minutes <= 0) continue
            // Stagger: later days start a little after earlier ones.
            val local = ((growth.value * 1.4f) - i / (daysInMonth * 2.5f)).coerceIn(0f, 1f)
            val h = size.height * (day.minutes / max).coerceAtMost(1f) * local
            drawRoundRect(
                color = if (day.date == selected) primary else muted,
                topLeft = Offset(x, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2),
            )
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        listOf(1, 8, 15, 22, daysInMonth).distinct().forEachIndexed { i, d ->
            if (i > 0) Spacer(Modifier.weight(1f))
            Text(
                d.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppUsageRow(app: AppUsageUi, maxMinutes: Int, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(app.packageName, app.label, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(
                    app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(formatMinutes(app.minutes), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { app.minutes.toFloat() / maxMinutes },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}
