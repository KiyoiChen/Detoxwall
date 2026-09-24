package com.example.ui.blocker

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.data.repository.DetoxRepository
import com.example.ui.theme.DetoxMint
import com.example.ui.theme.LimitRose
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WarningAmber
import com.example.util.AlertSoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayBlockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_BLOCK_MODE = "extra_block_mode"

        const val MODE_ADULT = "mode_adult"
        const val MODE_CHILD = "mode_child"
        const val MODE_FOCUS = "mode_focus"
        const val MODE_BLACKOUT = "mode_blackout"
    }

    private lateinit var repository: DetoxRepository
    private lateinit var alertSoundManager: AlertSoundManager
    private var blockMode: String = MODE_BLACKOUT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = DetoxRepository(applicationContext)
        alertSoundManager = AlertSoundManager(applicationContext)

        blockMode = intent.getStringExtra(EXTRA_BLOCK_MODE) ?: MODE_BLACKOUT
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "com.instagram.android"
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Restricted App"

        // ── Window flags for proper lock behavior ────────────────────────────
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        // Keep screen on and show over keyguard (lock screen)
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false
        windowInsetsController.isAppearanceLightNavigationBars = false

        // Hide system bars for child/blackout. Swipe reveals them transiently —
        // the AccessibilityService will re-enforce if user navigates away.
        if (blockMode == MODE_CHILD || blockMode == MODE_BLACKOUT || blockMode == MODE_FOCUS) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // ── Intercept ALL Back button presses ───────────────────────────────
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (blockMode) {
                    MODE_CHILD, MODE_BLACKOUT -> handleChildBypassAttempt()
                    MODE_FOCUS -> handleChildBypassAttempt() // focus mode: back also triggers alarm
                    else -> finish() // Adult soft block allows back
                }
            }
        })

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    when (blockMode) {
                        MODE_CHILD, MODE_BLACKOUT -> ChildBlackoutScreen(
                            appName = appName,
                            onAttemptBypass = { handleChildBypassAttempt() },
                            onUnlockWithPin = { pin ->
                                val isValid = repository.securityManager.verifyPin(pin)
                                if (isValid) {
                                    alertSoundManager.stopAlert()
                                    repository.securityManager.resetChildAccumulatedMinutes()
                                    finish()
                                }
                                isValid
                            },
                            onTestSiren = {
                                alertSoundManager.playBypassAlert(lifecycleScope)
                            }
                        )
                        MODE_FOCUS -> FocusLockOverlayScreen(
                            appName = appName,
                            onAttemptBypass = { handleChildBypassAttempt() },
                            onUnlockWithPin = { pin ->
                                val isValid = repository.securityManager.verifyPin(pin)
                                if (isValid) {
                                    alertSoundManager.stopAlert()
                                    finish()
                                }
                                isValid
                            }
                        )
                        else -> AdultSoftBlockScreen(
                            appName = appName,
                            onDismissWaitComplete = { finish() },
                            onExitApp = { finish() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-enforce immersive fullscreen when returning from system UI swipe
        if (blockMode == MODE_CHILD || blockMode == MODE_BLACKOUT || blockMode == MODE_FOCUS) {
            val ctrl = WindowCompat.getInsetsController(window, window.decorView)
            ctrl.hide(WindowInsetsCompat.Type.systemBars())
            ctrl.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onPause() {
        super.onPause()
        // For CHILD / BLACKOUT / FOCUS: if we lose focus (e.g. user pressed Recent Apps),
        // the AccessibilityService will detect the new foreground app and re-launch this block.
        // We do NOT finish() here — let the AccessibilityService handle re-enforcement.
        // Log bypass attempt for tracking
        if (blockMode == MODE_CHILD || blockMode == MODE_BLACKOUT || blockMode == MODE_FOCUS) {
            lifecycleScope.launch {
                val activeId = repository.securityManager.getActiveProfileId()
                repository.registerBypassAttempt(activeId)
            }
        }
    }

    private fun handleChildBypassAttempt() {
        lifecycleScope.launch {
            val activeId = repository.securityManager.getActiveProfileId()
            repository.registerBypassAttempt(activeId)
            alertSoundManager.playBypassAlert(lifecycleScope)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        alertSoundManager.release()
    }
}

/**
 * Child Blackout Screen:
 * Full-screen pitch black. Only unlock via parent PIN.
 * Back button triggers alarm and records bypass attempt.
 * Cannot be dismissed by tapping outside the dialog.
 */
@Composable
fun ChildBlackoutScreen(
    appName: String,
    onAttemptBypass: () -> Unit,
    onUnlockWithPin: (String) -> Boolean,
    onTestSiren: () -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Tap on black screen → open PIN dialog (not dismiss)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showPinDialog = true }
            .testTag("child_blackout_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle parent-tap indicator at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF18181B).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF27272A), RoundedCornerShape(20.dp))
                    .clickable { showPinDialog = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = Color(0xFFA1A1AA),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Screen Time Reached • Tap to Unlock",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFA1A1AA)
                )
            }
        }

        // PIN Unlock Dialog — cannot be dismissed by tapping outside
        if (showPinDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.96f))
                    // Intercept all taps so outside-tap cannot dismiss
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Block outside taps */ }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f))
                                .border(1.5.dp, Color(0xFF818CF8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security",
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Parent Unlock",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Screen turned black for $appName.\nEnter parent PIN to resume.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = pinText,
                            onValueChange = {
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    pinText = it
                                    pinError = false
                                }
                            },
                            label = { Text("Parent PIN", color = Color(0xFF94A3B8)) },
                            placeholder = { Text("Enter 4-digit PIN", color = Color(0xFF64748B)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val ok = onUnlockWithPin(pinText)
                                    if (!ok) {
                                        pinError = true
                                        onAttemptBypass()
                                    }
                                }
                            ),
                            isError = pinError,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parent_unlock_pin_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF818CF8),
                                unfocusedBorderColor = Color(0xFF475569),
                                errorBorderColor = LimitRose,
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B)
                            )
                        )

                        if (pinError) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ Incorrect PIN — alarm activated",
                                color = LimitRose,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Fixed button layout — no weight clipping
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val ok = onUnlockWithPin(pinText)
                                    if (!ok) {
                                        pinError = true
                                        onAttemptBypass()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("parent_unlock_confirm_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Unlock", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    showPinDialog = false
                                    pinText = ""
                                    pinError = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF94A3B8)
                                )
                            ) {
                                Text("Back to Black Screen")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = onTestSiren,
                            modifier = Modifier.testTag("blackout_test_siren_btn")
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = LimitRose,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Siren Sound", color = LimitRose, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Adult Soft Block Screen:
 * 5-second mindfulness pause before dismissal.
 */
@Composable
fun AdultSoftBlockScreen(
    appName: String,
    onDismissWaitComplete: () -> Unit,
    onExitApp: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
                )
            )
            .padding(28.dp)
            .testTag("adult_soft_block_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(WarningAmber.copy(alpha = 0.2f))
                    .border(2.dp, WarningAmber, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassBottom,
                    contentDescription = "Time's Up",
                    tint = WarningAmber,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Time's Up for Today",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You've reached your daily screen allowance for $appName.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "5-Second Mindfulness Pause",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9),
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (countdown > 0) "Take a deep breath ($countdown s)..."
                               else "Pause complete. Close the app or proceed mindfully.",
                        fontSize = 13.sp,
                        color = if (countdown > 0) WarningAmber else DetoxMint,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { (5 - countdown) / 5f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = DetoxMint,
                        trackColor = Color(0xFF334155)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onExitApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("exit_restricted_app_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF334155),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Close $appName", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (countdown == 0) {
                OutlinedButton(
                    onClick = onDismissWaitComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("proceed_mindfully_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DetoxMint)
                ) {
                    Text("Proceed with Awareness", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Focus Lock Overlay Screen:
 * Displayed when user opens a restricted app during an active Focus Session.
 * NO dismiss button — only parent PIN can unlock it.
 * This prevents Focus Mode bypass via "Back to DetoxWall" escape.
 */
@Composable
fun FocusLockOverlayScreen(
    appName: String,
    onAttemptBypass: () -> Unit,
    onUnlockWithPin: (String) -> Boolean
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                )
            )
            // Intercept taps — cannot be tapped away
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* block taps */ }
            .testTag("focus_lock_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1).copy(alpha = 0.2f))
                    .border(1.5.dp, Color(0xFF818CF8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Focus Mode",
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Focus Mode Active",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$appName is paused during your deep work session.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To stop Focus Mode, ask a parent to end the session in DetoxWall.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Parent PIN unlock only — no free dismiss
            TextButton(
                onClick = { showPinDialog = true },
                modifier = Modifier.testTag("focus_parent_unlock_btn")
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Parent Unlock",
                    color = Color(0xFF818CF8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // PIN Dialog — cannot be dismissed by tapping outside
        if (showPinDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.90f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* block outside taps */ }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Parent Unlock",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Enter parent PIN to exit Focus Mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = pinText,
                            onValueChange = {
                                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                    pinText = it
                                    pinError = false
                                }
                            },
                            label = { Text("Parent PIN", color = Color(0xFF94A3B8)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val ok = onUnlockWithPin(pinText)
                                    if (!ok) {
                                        pinError = true
                                        onAttemptBypass()
                                    }
                                }
                            ),
                            isError = pinError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("focus_pin_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF818CF8),
                                unfocusedBorderColor = Color(0xFF475569),
                                errorBorderColor = LimitRose,
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B)
                            )
                        )

                        if (pinError) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Incorrect PIN",
                                color = LimitRose,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val ok = onUnlockWithPin(pinText)
                                    if (!ok) {
                                        pinError = true
                                        onAttemptBypass()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Unlock", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    showPinDialog = false
                                    pinText = ""
                                    pinError = false
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF94A3B8)
                                )
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}
