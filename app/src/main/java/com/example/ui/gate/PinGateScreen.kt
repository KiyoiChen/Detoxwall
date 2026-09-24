package com.example.ui.gate

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.security.PinManager
import com.example.security.PinResult
import com.example.ui.components.PinPad
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Asks for the parent PIN before showing the app. Shows a live countdown while locked out. */
@Composable
fun PinGateScreen(pinManager: PinManager, onUnlocked: () -> Unit) {
    val scope = rememberCoroutineScope()
    var entry by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var errorKey by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var lockedMs by remember { mutableLongStateOf(pinManager.lockoutRemainingMs()) }

    // Tick the lockout countdown.
    LaunchedEffect(lockedMs > 0) {
        while (lockedMs > 0) {
            delay(1_000)
            lockedMs = pinManager.lockoutRemainingMs()
        }
        if (message?.startsWith("Too many") == true) message = null
    }

    val breathe by rememberInfiniteTransition(label = "lock").animateFloat(
        initialValue = 0.92f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "lockScale",
    )

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Filled.Lock, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp).graphicsLayer { scaleX = breathe; scaleY = breathe },
            )
            Spacer(Modifier.height(16.dp))
            Text("Enter parent PIN", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = if (lockedMs > 0) "Too many tries. Try again in ${(lockedMs + 999) / 1000} s." else message ?: " ",
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "gateMsg",
            ) { text ->
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(32.dp))
            PinPad(
                value = entry,
                errorKey = errorKey,
                enabled = !busy && lockedMs == 0L,
                onValueChange = { value ->
                    entry = value
                    if (value.length == 4 && !busy) {
                        busy = true
                        scope.launch {
                            when (val r = pinManager.verify(value)) {
                                PinResult.Success, PinResult.NotSet -> onUnlocked()
                                is PinResult.Wrong -> {
                                    message = "Wrong PIN. ${r.attemptsLeft} ${if (r.attemptsLeft == 1) "try" else "tries"} left."
                                    entry = ""; errorKey++
                                }
                                is PinResult.LockedOut -> {
                                    lockedMs = r.remainingMs; entry = ""; errorKey++
                                }
                            }
                            busy = false
                        }
                    }
                },
            )
        }
    }
}
