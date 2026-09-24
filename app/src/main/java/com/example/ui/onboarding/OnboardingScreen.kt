package com.example.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.security.PinManager
import com.example.ui.components.PinPad
import com.example.ui.permissions.PermissionsScreen
import com.example.usage.PermissionStatus
import kotlinx.coroutines.launch

private enum class Step { WELCOME, PIN, CONFIRM, PERMISSIONS }

/** First run: welcome → create a parent PIN (optional) → permissions → done. */
@Composable
fun OnboardingScreen(
    pinManager: PinManager,
    permissions: List<PermissionStatus>,
    onPermissionsChanged: () -> Unit,
    onFinished: () -> Unit,
) {
    var step by remember { mutableStateOf(Step.WELCOME) }
    var first by remember { mutableStateOf("") }
    var entry by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var errorKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            StepDots(current = step.ordinal, total = Step.entries.size, modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally))
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally { it / 3 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 * dir } + fadeOut())
                },
                label = "onboarding",
                modifier = Modifier.weight(1f),
            ) { s ->
                when (s) {
                    Step.WELCOME -> Welcome(onNext = { step = Step.PIN })
                    Step.PIN, Step.CONFIRM -> Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            if (s == Step.PIN) "Create a parent PIN" else "Enter it again",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            message ?: "Protects your settings. Stored only on this phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (message != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(32.dp))
                        PinPad(value = entry, errorKey = errorKey, onValueChange = { v ->
                            entry = v; message = null
                            if (v.length < 4) return@PinPad
                            if (s == Step.PIN) {
                                first = v; entry = ""; step = Step.CONFIRM
                            } else if (v != first) {
                                message = "PINs didn't match. Start again."
                                entry = ""; first = ""; errorKey++; step = Step.PIN
                            } else {
                                scope.launch { pinManager.setPin(v); entry = ""; step = Step.PERMISSIONS }
                            }
                        })
                        TextButton(onClick = { entry = ""; step = Step.PERMISSIONS }) { Text("Skip for now") }
                    }
                    Step.PERMISSIONS -> Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            PermissionsScreen(permissions, PaddingValues(0.dp), onPermissionsChanged)
                        }
                        Button(
                            onClick = onFinished,
                            modifier = Modifier.fillMaxWidth().padding(20.dp).height(52.dp),
                        ) { Text(if (permissions.all { it.granted }) "Start using DetoxWall" else "Continue") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Welcome(onNext: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        if (shown) 1f else 0.4f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logo",
    )
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(112.dp).graphicsLayer { scaleX = scale; scaleY = scale; alpha = scale }
                .clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text("DetoxWall", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "See where your time goes, set limits and routines, and take focused breaks. Everything stays on this phone.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Get started") }
    }
}

@Composable
private fun StepDots(current: Int, total: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val width by animateFloatAsState(if (i == current) 22f else 8f, label = "dot$i")
            Box(
                Modifier.height(8.dp).width(width.dp).clip(CircleShape)
                    .background(if (i <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
            )
        }
    }
}
