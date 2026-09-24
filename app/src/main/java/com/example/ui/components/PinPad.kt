package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * PIN entry: dots that pop as digits are typed and a keypad with spring feedback.
 * Bump [errorKey] to shake the dots after a wrong PIN.
 */
@Composable
fun PinPad(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 4,
    errorKey: Int = 0,
    enabled: Boolean = true,
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(errorKey) {
        if (errorKey == 0) return@LaunchedEffect
        for (x in listOf(18f, -16f, 12f, -8f, 4f, 0f)) shake.animateTo(x, spring(stiffness = Spring.StiffnessHigh))
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.graphicsLayer { translationX = shake.value }.testTag("pin_dots"),
        ) {
            repeat(length) { i -> PinDot(filled = i < value.length, error = errorKey != 0 && value.isEmpty()) }
        }
        Spacer(Modifier.height(32.dp))
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    Key(key, enabled) {
                        when (key) {
                            "⌫" -> onValueChange(value.dropLast(1))
                            "" -> Unit
                            else -> if (value.length < length) onValueChange(value + key)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun PinDot(filled: Boolean, error: Boolean) {
    val scale by animateFloatAsState(
        if (filled) 1.15f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dotScale",
    )
    val color by animateColorAsState(
        when {
            error -> MaterialTheme.colorScheme.error
            filled -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "dotColor",
    )
    Box(
        Modifier.size(16.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape).background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (filled) 0f else 0.6f), CircleShape),
    )
}

@Composable
private fun Key(label: String, enabled: Boolean, onClick: () -> Unit) {
    val keySize = 72.dp
    if (label.isEmpty()) {
        Spacer(Modifier.size(keySize))
        return
    }
    Box(
        Modifier.size(keySize).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .pressScale(enabled = enabled, onClick = onClick)
            .testTag("pin_key_$label"),
        contentAlignment = Alignment.Center,
    ) {
        if (label == "⌫") {
            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete")
        } else {
            Text(label, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
