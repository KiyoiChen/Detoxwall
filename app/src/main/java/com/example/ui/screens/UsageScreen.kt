package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.DetoxMint
import com.example.ui.theme.LimitRose
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DetoxViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun UsageScreen(
    viewModel: DetoxViewModel,
    contentPadding: PaddingValues,
    onBackToHome: () -> Unit = {}
) {
    val selectedDate by viewModel.selectedUsageDate.collectAsStateWithLifecycle()
    val usageForDate by viewModel.usageForDate.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingUsage.collectAsStateWithLifecycle()
    val hasUsageStats by viewModel.hasUsageStatsPermission.collectAsStateWithLifecycle()

    val dateLabel = formatDateLabel(selectedDate)
    val isToday = isSameDay(selectedDate, Calendar.getInstance())
    val isFuture = selectedDate.timeInMillis > Calendar.getInstance().timeInMillis + 86400_000L

    // Build sorted usage list from real data
    val usageList = usageForDate.entries
        .map { (pkg, ms) -> pkg to ms }
        .sortedByDescending { it.second }
        .take(10)

    val totalMs = usageForDate.values.sumOf { it }
    val totalMinutes = (totalMs / 60_000L).toInt()
    val totalHrs = totalMinutes / 60
    val totalMins = totalMinutes % 60
    val formattedTotal = when {
        totalMinutes == 0 -> "0m"
        totalHrs == 0 -> "${totalMins}m"
        totalMins == 0 -> "${totalHrs}h"
        else -> "${totalHrs}h ${totalMins}m"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
    ) {
        // ── Date Navigation ────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("date_navigation_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121722)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1D2638))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ← Previous day
                    IconButton(
                        onClick = { viewModel.navigateUsageDateBack() },
                        modifier = Modifier.testTag("prev_day_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = DetoxMint
                        )
                    }

                    // Date label
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = DetoxMint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // → Next day (disabled if today)
                    IconButton(
                        onClick = { viewModel.navigateUsageDateForward() },
                        enabled = !isToday && !isFuture,
                        modifier = Modifier.testTag("next_day_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Day",
                            tint = if (!isToday && !isFuture) DetoxMint else Color(0xFF334155)
                        )
                    }
                }
            }
        }

        // ── No Permission Notice ───────────────────────────────────────────
        if (!hasUsageStats) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Usage Stats Permission Required",
                            color = WarningAmber,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Grant Usage Stats access in the Schedules tab to see real app usage data.",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            return@LazyColumn
        }

        // ── Loading indicator ──────────────────────────────────────────────
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DetoxMint, modifier = Modifier.size(36.dp))
                }
            }
            return@LazyColumn
        }

        // ── Screen Time Summary Card ───────────────────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("screen_time_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121722)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1D2638))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Screen Time",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF8E9DAE),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formattedTotal,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (totalMinutes == 0) Color(0xFF64748B) else Color.White,
                        fontSize = 28.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (totalMinutes == 0) "No usage recorded" else "Total tracked usage",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E9DAE),
                        fontSize = 12.sp
                    )

                    if (usageList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Smooth wave chart (proportional to actual usage)
                        UsageWaveChart(usageList = usageList, maxMs = usageList.first().second)
                    }
                }
            }
        }

        // ── Empty State ────────────────────────────────────────────────────
        if (usageList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Usage Data",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Android UsageStats has no data\nfor ${formatDateLabel(selectedDate)}.",
                            color = Color(0xFF475569),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            return@LazyColumn
        }

        // ── Most Used Apps Card ────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("most_used_apps_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121722)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1D2638))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Most Used Apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val maxMs = usageList.firstOrNull()?.second ?: 1L

                    usageList.forEachIndexed { index, (pkg, ms) ->
                        val appName = getAppDisplayName(pkg)
                        val minutes = (ms / 60_000L).toInt()
                        val hours = minutes / 60
                        val mins = minutes % 60
                        val durationText = when {
                            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                            hours > 0 -> "${hours}h"
                            else -> "${mins}m"
                        }
                        val progressFraction = (ms.toFloat() / maxMs.toFloat()).coerceIn(0.02f, 1f)
                        val barColor = appBarColor(index)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(barColor.copy(alpha = 0.18f))
                                            .border(1.dp, barColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = appName.take(1).uppercase(),
                                            color = barColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }

                                Text(
                                    text = durationText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF1E293B))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(barColor)
                                )
                            }
                        }

                        if (index < usageList.size - 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        // ── Breakdown by minutes ───────────────────────────────────────────
        item {
            Text(
                "Usage Breakdown",
                color = Color(0xFF94A3B8),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(usageList.size) { index ->
            val (pkg, ms) = usageList[index]
            val minutes = (ms / 60_000L).toInt()
            val limitMinutes = 60 // default reference; real limits shown in App Limits tab
            val progressFraction = (minutes.toFloat() / limitMinutes.toFloat()).coerceIn(0f, 1f)
            val color = when {
                progressFraction >= 1f -> LimitRose
                progressFraction >= 0.8f -> WarningAmber
                else -> DetoxMint
            }
            val appName = getAppDisplayName(pkg)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = appName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${minutes}m",
                        color = color,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageWaveChart(usageList: List<Pair<String, Long>>, maxMs: Long) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Build proportional points from actual usage data (up to 7)
            val capped = usageList.take(7)
            val points = if (capped.isEmpty()) {
                listOf(
                    Offset(0f, h * 0.85f),
                    Offset(w, h * 0.85f)
                )
            } else {
                val step = w / (capped.size + 1).toFloat()
                listOf(Offset(0f, h * 0.9f)) +
                capped.mapIndexed { i, (_, ms) ->
                    val normalized = 1f - (ms.toFloat() / maxMs.toFloat()).coerceIn(0.05f, 0.95f)
                    Offset((i + 1) * step, h * normalized)
                } +
                listOf(Offset(w, h * 0.9f))
            }

            val strokePath = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val midX = (p0.x + p1.x) / 2
                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                }
            }

            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DetoxMint.copy(alpha = 0.30f),
                        DetoxMint.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            drawPath(
                path = strokePath,
                color = DetoxMint,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

private fun formatDateLabel(cal: Calendar): String {
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        isSameDay(cal, today) -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            "Today, ${sdf.format(cal.time)}"
        }
        isSameDay(cal, yesterday) -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            "Yesterday, ${sdf.format(cal.time)}"
        }
        else -> {
            SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(cal.time)
        }
    }
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean {
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
           a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

private fun getAppDisplayName(packageName: String): String {
    val known = mapOf(
        "com.instagram.android" to "Instagram",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.google.android.youtube" to "YouTube",
        "com.twitter.android" to "X (Twitter)",
        "com.facebook.katana" to "Facebook",
        "com.snapchat.android" to "Snapchat",
        "com.reddit.frontpage" to "Reddit",
        "com.netflix.mediaclient" to "Netflix",
        "com.android.chrome" to "Chrome",
        "com.google.android.gm" to "Gmail",
        "com.whatsapp" to "WhatsApp",
        "com.spotify.music" to "Spotify",
        "com.discord" to "Discord",
        "com.google.android.apps.maps" to "Google Maps"
    )
    return known[packageName]
        ?: packageName.substringAfterLast('.').replaceFirstChar { it.uppercaseChar() }
}

private val APP_COLORS = listOf(
    Color(0xFF00E5BE), // mint
    Color(0xFF6366F1), // indigo
    Color(0xFFFF3366), // rose
    Color(0xFFF59E0B), // amber
    Color(0xFF25F4EE), // cyan
    Color(0xFFE1306C), // pink
    Color(0xFF3B82F6), // blue
    Color(0xFF8B5CF6), // purple
    Color(0xFF10B981), // emerald
    Color(0xFFF97316), // orange
)

private fun appBarColor(index: Int): Color = APP_COLORS[index % APP_COLORS.size]
