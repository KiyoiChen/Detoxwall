package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.DetoxMint
import com.example.ui.theme.ObsidianBg
import com.example.ui.viewmodel.DetoxViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MockAppUsage(
    val name: String,
    val durationText: String,
    val usageMinutes: Int,
    val maxMinutes: Int,
    val barColor: Color,
    val iconColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun UsageScreen(
    viewModel: DetoxViewModel,
    contentPadding: PaddingValues,
    onBackToHome: () -> Unit = {}
) {
    val limits by viewModel.appLimits.collectAsStateWithLifecycle()

    val totalTodayMinutes = remember(limits) {
        val sum = limits.sumOf { it.currentUsageMinutes }
        if (sum == 0) 165 else sum // Default to 2h 45m (165m) as shown in mockup
    }

    val hrs = totalTodayMinutes / 60
    val mins = totalTodayMinutes % 60
    val formattedTotal = "${hrs}h ${mins}m"

    val todayDateFormatted = remember {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        "Today, ${sdf.format(Date())}"
    }

    // App items directly matching the mockup
    val appsList = remember(limits) {
        listOf(
            MockAppUsage(
                name = "YouTube",
                durationText = "1h 20m",
                usageMinutes = 80,
                maxMinutes = 100,
                barColor = Color(0xFFFF3B30),
                iconColor = Color(0xFFFF3B30),
                icon = Icons.Default.PlayArrow
            ),
            MockAppUsage(
                name = "TikTok",
                durationText = "45m",
                usageMinutes = 45,
                maxMinutes = 100,
                barColor = Color(0xFF25F4EE),
                iconColor = Color(0xFF25F4EE),
                icon = Icons.Default.MusicNote
            ),
            MockAppUsage(
                name = "Instagram",
                durationText = "25m",
                usageMinutes = 25,
                maxMinutes = 100,
                barColor = Color(0xFFE1306C),
                iconColor = Color(0xFFE1306C),
                icon = Icons.Default.PhotoCamera
            ),
            MockAppUsage(
                name = "Games",
                durationText = "15m",
                usageMinutes = 15,
                maxMinutes = 100,
                barColor = Color(0xFF3B82F6),
                iconColor = Color(0xFF3B82F6),
                icon = Icons.Default.Gamepad
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 48.dp)
    ) {
        // ==========================================
        // 1. DATE SELECTOR PILL (Matched to Mockup)
        // ==========================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF121722))
                    .border(1.dp, Color(0xFF1D2638), RoundedCornerShape(16.dp))
                    .clickable { /* Date selector */ }
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .testTag("date_selector_pill"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        tint = DetoxMint,
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = todayDateFormatted,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Date",
                        tint = DetoxMint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ==========================================
        // 2. SCREEN TIME & WAVE CHART CARD
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("screen_time_wave_card"),
                shape = RoundedCornerShape(22.dp),
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
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formattedTotal,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 28.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Total Usage",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E9DAE),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Smooth Glowing Cyan Wave Curve Chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Points representing usage curve: [12AM, 6AM, 12PM, 6PM, 12AM]
                            val points = listOf(
                                Offset(0f, h * 0.85f),
                                Offset(w * 0.20f, h * 0.70f),
                                Offset(w * 0.38f, h * 0.40f),
                                Offset(w * 0.52f, h * 0.75f),
                                Offset(w * 0.70f, h * 0.45f),
                                Offset(w * 0.85f, h * 0.65f),
                                Offset(w, h * 0.25f)
                            )

                            // Path for stroke
                            val strokePath = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 0 until points.size - 1) {
                                    val p0 = points[i]
                                    val p1 = points[i + 1]
                                    val midX = (p0.x + p1.x) / 2
                                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                }
                            }

                            // Path for gradient fill
                            val fillPath = Path().apply {
                                addPath(strokePath)
                                lineTo(w, h)
                                lineTo(0f, h)
                                close()
                            }

                            // Draw subtle gradient fill underneath
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        DetoxMint.copy(alpha = 0.35f),
                                        DetoxMint.copy(alpha = 0.05f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = h
                                )
                            )

                            // Draw vibrant cyan curve line
                            drawPath(
                                path = strokePath,
                                color = DetoxMint,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            )

                            // Draw highlighted peaks
                            drawCircle(
                                color = DetoxMint,
                                radius = 4.dp.toPx(),
                                center = points[2]
                            )
                            drawCircle(
                                color = DetoxMint,
                                radius = 4.dp.toPx(),
                                center = points[6]
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Timeline Markers: 12 AM, 6 AM, 12 PM, 6 PM, 12 AM
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val labels = listOf("12 AM", "6 AM", "12 PM", "6 PM", "12 AM")
                        labels.forEach { label ->
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. MOST USED APPS CARD (Matched to Mockup)
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("most_used_apps_card"),
                shape = RoundedCornerShape(22.dp),
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
                        color = Color.White,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    appsList.forEachIndexed { index, app ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // App Icon Squircle
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(app.barColor.copy(alpha = 0.18f))
                                            .border(1.dp, app.barColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = app.icon,
                                            contentDescription = app.name,
                                            tint = app.iconColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = app.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }

                                Text(
                                    text = app.durationText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Colored progress track
                            val progressFraction = (app.usageMinutes.toFloat() / app.maxMinutes.toFloat()).coerceIn(0.05f, 1f)
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
                                        .background(app.barColor)
                                )
                            }
                        }

                        if (index < appsList.size - 1) {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }
            }
        }
    }
}
