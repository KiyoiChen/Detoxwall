package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLimit
import com.example.ui.theme.DetoxMint

@Composable
fun EditLimitDialog(
    appLimit: AppLimit,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var minutes by remember { mutableFloatStateOf(appLimit.dailyLimitMinutes.toFloat()) }
    val quickPresets = listOf(15, 30, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Daily Limit: ${appLimit.appName}",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${minutes.toInt()} minutes / day",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = DetoxMint,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = minutes,
                    onValueChange = { minutes = it },
                    valueRange = 5f..180f,
                    steps = 34,
                    colors = SliderDefaults.colors(
                        thumbColor = DetoxMint,
                        activeTrackColor = DetoxMint,
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("limit_slider")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickPresets.take(3).forEach { preset ->
                        FilterChip(
                            selected = minutes.toInt() == preset,
                            onClick = { minutes = preset.toFloat() },
                            label = { Text("${preset}m") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DetoxMint,
                                selectedLabelColor = Color(0xFF042F2E)
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickPresets.drop(3).forEach { preset ->
                        FilterChip(
                            selected = minutes.toInt() == preset,
                            onClick = { minutes = preset.toFloat() },
                            label = { Text("${preset}m") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DetoxMint,
                                selectedLabelColor = Color(0xFF042F2E)
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(minutes.toInt()) },
                colors = ButtonDefaults.buttonColors(containerColor = DetoxMint),
                modifier = Modifier.testTag("save_limit_button")
            ) {
                Text("Save Limit", color = Color(0xFF042F2E), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(20.dp)
    )
}
