package com.example.enforcement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.security.PinManager
import com.example.data.PlaySessionPolicy
import com.example.data.Settings
import com.example.security.PinResult
import com.example.ui.components.PinPad
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDate

/** What the lock overlay shows for a [Decision.BlockApp] or [Decision.LockDevice]. */
@Composable
fun LockScreenContent(
    decision: Decision,
    settings: Settings,
    appLabel: (String) -> String,
    pinManager: PinManager,
    onCloseApp: () -> Unit,
    onEmergency: () -> Unit,
    onParentUse: () -> Unit,
    onParentSkipRest: () -> Unit,
    onStartRound: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var askingPin by remember(decision) { mutableStateOf(false) }
    var authorized by remember(decision) { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(decision) { while (true) { now = System.currentTimeMillis(); delay(1_000L) } }
    val today = LocalDate.now().toString()
    val restSeconds = ((settings.playRestEndAt - now).coerceAtLeast(0L) + 999L) / 1000L
    var entry by remember(decision) { mutableStateOf("") }
    var message by remember(decision) { mutableStateOf<String?>(null) }
    var errorKey by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    val (title, body) = when (decision) {
        Decision.ChoosePlayTime -> "🎮 遊戲時間小管家" to "這一輪想玩幾分鐘？先選時間才能使用手機。"
        is Decision.BlockApp -> {
            val label = appLabel(decision.packageName)
            when (decision.reason) {
                BlockReason.LIMIT -> "Time's up" to "You've used today's time for $label."
                BlockReason.FOCUS -> "Focus session" to "$label is paused until your focus session ends."
                BlockReason.SCHEDULE -> "Blocked by schedule" to "$label is blocked during \"${decision.detail}\"."
                BlockReason.PLAY_NOT_STARTED -> "Choose play time first" to
                    "Open DetoxWall and choose 5, 10, 15, or 30 minutes before playing."
            }
        }
        is Decision.LockDevice -> "Phone locked" to when (decision.reason) {
            LockReason.ALLOWANCE -> "Today's screen time is used up."
            LockReason.SCHEDULE -> "Locked during \"${decision.detail}\"."
            LockReason.MANUAL -> "Locked by a parent."
            LockReason.REST -> "休息倒數 %02d:%02d，結束後會自動開放下一輪。".format(restSeconds / 60, restSeconds % 60)
            LockReason.DAILY_PLAY -> "今天的遊戲時間已用完，明天自動恢復。"
        }
        else -> "" to ""
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                if (askingPin) message ?: "Enter parent PIN" else body,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = if (askingPin && message != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            if (authorized) {
                Text("家長管理", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onParentUse, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("暫時解鎖給家長使用（15 分鐘）")
                }
                if (decision is Decision.LockDevice && decision.reason == LockReason.REST) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onParentSkipRest, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("提前結束休息，讓孩子選下一輪")
                    }
                }
                TextButton(onClick = { authorized = false }) { Text("取消") }
            } else if (askingPin) {
                PinPad(
                    value = entry,
                    errorKey = errorKey,
                    enabled = !busy,
                    onValueChange = { value ->
                        entry = value
                        message = null
                        if (value.length == 4 && !busy) {
                            busy = true
                            scope.launch {
                                when (val r = pinManager.verify(value)) {
                                    PinResult.Success -> { authorized = true; askingPin = false; entry = "" }
                                    PinResult.NotSet -> { message = "家長 PIN 尚未設定"; entry = ""; errorKey++ }
                                    is PinResult.Wrong -> { message = "Wrong PIN. ${r.attemptsLeft} tries left."; entry = ""; errorKey++ }
                                    is PinResult.LockedOut -> { message = "Too many tries. Wait ${(r.remainingMs + 999) / 1000} s."; entry = ""; errorKey++ }
                                }
                                busy = false
                            }
                        }
                    },
                )
                TextButton(onClick = { askingPin = false }) { Text("Cancel") }
            } else when (decision) {
                Decision.ChoosePlayTime -> {
                    Text("今日剩餘 ${PlaySessionPolicy.remainingToday(settings, today)} 分鐘")
                    Spacer(Modifier.height(12.dp))
                    PlaySessionPolicy.choices(settings, today).chunked(2).forEach { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            group.forEach { minutes ->
                                Button(onClick = { onStartRound(minutes) }, modifier = Modifier.weight(1f).height(58.dp)) {
                                    Text("$minutes 分鐘")
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(
                        "時間到會自動休息 ${settings.playRestMinutes} 分鐘，不用家長重設。",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { askingPin = true }) { Text("家長管理（PIN）") }
                    OutlinedButton(onClick = onEmergency, modifier = Modifier.fillMaxWidth()) {
                        Text("緊急電話")
                    }
                }
                is Decision.BlockApp -> {
                    Button(onClick = onCloseApp, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("Close ${appLabel(decision.packageName)}")
                    }
                    if (decision.reason == BlockReason.LIMIT) {
                        TextButton(onClick = { askingPin = true }) { Text("Parent: add 15 minutes") }
                    }
                }
                is Decision.LockDevice -> {
                    Button(onClick = { askingPin = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("家長解鎖（PIN）")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onEmergency, modifier = Modifier.fillMaxWidth()) { Text("Emergency call") }
                }
                else -> Unit
            }
        }
    }
}
