package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChildProfile
import com.example.ui.theme.DetoxMint
import com.example.ui.theme.LimitRose
import com.example.ui.theme.ObsidianBg
import com.example.ui.viewmodel.DetoxViewModel

/**
 * Child Protection & Screen Blackout Home Screen:
 * Directly matched to the DetoxWall UI mockup with:
 * 1. Child Protection toggle card
 * 2. Screen Blackout card with Daily Limit, Quick Presets, Time Slider, & Test Black Screen
 * 3. Audible Siren Alarm card with Volume Slider and Siren Tester
 * 4. "Why DetoxWall?" informational value card
 */
@Composable
fun ChildModeScreen(
    viewModel: DetoxViewModel,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val isChildModeActive by viewModel.isChildModeActive.collectAsStateWithLifecycle()
    val hasParentPin by viewModel.hasParentPin.collectAsStateWithLifecycle()
    val profiles by viewModel.childProfiles.collectAsStateWithLifecycle()
    val screenBlankLimit by viewModel.screenBlankLimitMinutes.collectAsStateWithLifecycle()
    val isScreenBlankEnabled by viewModel.isScreenBlankEnabled.collectAsStateWithLifecycle()
    val isSirenAlarmEnabled by viewModel.isSirenAlarmEnabled.collectAsStateWithLifecycle()
    val sirenVolumePercent by viewModel.sirenVolumePercent.collectAsStateWithLifecycle()
    val isSirenPlaying by viewModel.isSirenPlaying.collectAsStateWithLifecycle()

    val currentProfile = profiles.firstOrNull() ?: ChildProfile()

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinVerifyDialog by remember { mutableStateOf(false) }
    var pendingToggleAction by remember { mutableStateOf(false) }

    fun formatMinutesToHuman(mins: Int): String {
        return when {
            mins < 60 -> "$mins minutes"
            mins % 60 == 0 -> "${mins / 60} ${if (mins / 60 == 1) "hour" else "hours"}"
            else -> "${mins / 60}h ${mins % 60}m"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
    ) {
        // ==========================================
        // 1. CHILD PROTECTION CARD (Matched to Mockup)
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("child_protection_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121722)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1D2638))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Blue Squircle Icon Container
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1A2338))
                                .border(1.dp, Color(0xFF283552), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Child Protection",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Child Protection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Keep your child safe with smart screen protection",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8E9DAE),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = isChildModeActive,
                        onCheckedChange = { enable ->
                            if (!hasParentPin) {
                                showPinSetupDialog = true
                            } else if (!enable) {
                                pendingToggleAction = false
                                showPinVerifyDialog = true
                            } else {
                                viewModel.toggleChildMode(currentProfile.id, true)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DetoxMint,
                            uncheckedThumbColor = Color(0xFF64748B),
                            uncheckedTrackColor = Color(0xFF1E293B),
                            uncheckedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("child_protection_switch")
                    )
                }
            }
        }

        // ==========================================
        // 2. SCREEN BLACKOUT CARD (Matched to Mockup)
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("screen_blackout_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121722)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1D2638))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Header Row: Moon icon, Title, Subtitle, Toggle switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Crescent Moon Squircle
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1A2338))
                                    .border(1.dp, Color(0xFF283552), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = "Screen Blackout",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "Screen Blackout",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Screen turns black when limit is reached",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8E9DAE),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = isScreenBlankEnabled,
                            onCheckedChange = { viewModel.setScreenBlankEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DetoxMint,
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF1E293B),
                                uncheckedBorderColor = Color(0xFF334155)
                            ),
                            modifier = Modifier.testTag("screen_blackout_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Daily Limit & 5 min - 5 hrs range badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Limit",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF8E9DAE),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatMinutesToHuman(screenBlankLimit),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = DetoxMint,
                                fontSize = 22.sp
                            )
                        }

                        // Range badge pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0B2527))
                                .border(1.dp, Color(0xFF0E4447), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "5 min – 5 hrs",
                                    color = DetoxMint,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "range",
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Quick Presets header
                    Text(
                        text = "Quick Presets",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF8E9DAE),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Preset Chips (5 min, 15 min, 30 min, 45 min, 1 hr, 2 hrs, 3 hrs, 5 hrs)
                    val presets = listOf(
                        5 to "5 min",
                        15 to "15 min",
                        30 to "30 min",
                        45 to "45 min",
                        60 to "1 hr",
                        120 to "2 hrs",
                        180 to "3 hrs",
                        300 to "5 hrs"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presets) { (mins, label) ->
                            val isSelected = screenBlankLimit == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) DetoxMint else Color(0xFF161D2B)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) DetoxMint else Color(0xFF232E44),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.setScreenBlankLimit(mins) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("preset_$mins"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF042F2E) else Color(0xFF8E9DAE)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Slider Range Labels: 5 min | Drag to adjust time | 5 hrs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "5 min", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(text = "Drag to adjust time", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(text = "5 hrs", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    // Continuous Slider with 5-minute increments
                    var sliderPos by remember(screenBlankLimit) {
                        mutableFloatStateOf(screenBlankLimit.toFloat())
                    }

                    Slider(
                        value = sliderPos,
                        onValueChange = { sliderPos = it },
                        onValueChangeFinished = {
                            val rounded = (Math.round(sliderPos / 5f) * 5).coerceIn(5, 300)
                            viewModel.setScreenBlankLimit(rounded)
                        },
                        valueRange = 5f..300f,
                        steps = 58, // 5 min increments from 5 to 300
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("blackout_time_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = DetoxMint,
                            activeTrackColor = DetoxMint,
                            inactiveTrackColor = Color(0xFF1E293B)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // "Test Black Screen Now" Button (Primary Solid Cyan)
                    Button(
                        onClick = { viewModel.previewBlackScreen(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("test_black_screen_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DetoxMint,
                            contentColor = Color(0xFF042F2E)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Test Screen",
                            tint = Color(0xFF042F2E),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test Black Screen Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // ==========================================
        // 3. AUDIBLE SIREN ALARM CARD (Matched to Mockup)
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audible_siren_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E101A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF341B26))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Siren Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Red Bell Squircle
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF301421))
                                    .border(1.dp, Color(0xFF481D31), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Audible Siren",
                                    tint = LimitRose,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "Audible Siren Alarm",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Loud emergency alarm if child attempts bypass",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9E8B94),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = isSirenAlarmEnabled,
                            onCheckedChange = { viewModel.setSirenAlarmEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DetoxMint,
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF2D1623),
                                uncheckedBorderColor = Color(0xFF481D31)
                            ),
                            modifier = Modifier.testTag("siren_alarm_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Volume Row: "Volume" | "80%"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Volume",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF9E8B94),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "$sirenVolumePercent%",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    var volumeSliderPos by remember(sirenVolumePercent) {
                        mutableFloatStateOf(sirenVolumePercent.toFloat())
                    }

                    // Coral Red Volume Slider (Exact matching mockup slider)
                    Slider(
                        value = volumeSliderPos,
                        onValueChange = { volumeSliderPos = it },
                        onValueChangeFinished = {
                            viewModel.setSirenVolumePercent(volumeSliderPos.toInt())
                        },
                        valueRange = 10f..100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("siren_volume_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = LimitRose,
                            activeTrackColor = LimitRose,
                            inactiveTrackColor = Color(0xFF381C26)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive Siren Test / Stop Button
                    if (isSirenPlaying) {
                        Button(
                            onClick = { viewModel.stopSimulatedAlarm() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("stop_siren_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LimitRose,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("STOP SIREN ALARM", fontWeight = FontWeight.ExtraBold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.triggerSimulatedAlarm() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("test_siren_alarm_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = LimitRose
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LimitRose.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Siren Alarm Sound", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. "WHY DETOXWALL?" SECTION (Matched to Mockup)
        // ==========================================
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = "Why DetoxWall?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DetoxMint,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                val benefits = listOf(
                    "Promotes healthy screen habits",
                    "Reduces digital distractions",
                    "Empowers parents with control",
                    "Peace of mind, every day"
                )

                benefits.forEach { benefit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, DetoxMint, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = DetoxMint,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = benefit,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom pill card: "🔒 Because their future matters."
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF121722))
                        .border(1.dp, Color(0xFF1D2638), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Because their future matters.",
                            color = Color(0xFFE2E8F0),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // PIN Setup Dialog
    // ==========================================
    if (showPinSetupDialog) {
        var pinInput by remember { mutableStateOf("") }
        var pinConfirm by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = {
                Text(
                    text = "Set Parent 4-Digit PIN",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Create a 4-digit PIN to prevent unauthorized changes and disable child lockouts safely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it
                        },
                        label = { Text("4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("setup_pin_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinConfirm,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) pinConfirm = it
                        },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("setup_pin_confirm_input")
                    )
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = LimitRose,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length != 4) {
                            errorMessage = "PIN must be exactly 4 digits"
                        } else if (pinInput != pinConfirm) {
                            errorMessage = "PINs do not match"
                        } else {
                            viewModel.setParentPin(pinInput)
                            viewModel.toggleChildMode(currentProfile.id, true)
                            showPinSetupDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DetoxMint, contentColor = Color(0xFF042F2E)),
                    modifier = Modifier.testTag("save_parent_pin_btn")
                ) {
                    Text("Save & Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF131926)
        )
    }

    // ==========================================
    // PIN Verification Dialog
    // ==========================================
    if (showPinVerifyDialog) {
        var pinVerifyInput by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPinVerifyDialog = false },
            title = {
                Text(
                    text = "Enter Parent PIN",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your 4-digit PIN to modify child screen protection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = pinVerifyInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                pinVerifyInput = it
                                isError = false
                            }
                        },
                        label = { Text("Parent PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = isError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("verify_pin_input")
                    )
                    if (isError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Incorrect PIN!",
                            color = LimitRose,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val valid = viewModel.verifyParentPin(pinVerifyInput)
                        if (valid) {
                            viewModel.toggleChildMode(currentProfile.id, pendingToggleAction)
                            showPinVerifyDialog = false
                        } else {
                            isError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DetoxMint, contentColor = Color(0xFF042F2E)),
                    modifier = Modifier.testTag("confirm_verify_pin_btn")
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinVerifyDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF131926)
        )
    }
}
