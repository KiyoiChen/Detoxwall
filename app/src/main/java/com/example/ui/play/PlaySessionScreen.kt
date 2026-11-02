package com.example.ui.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.PlaySessionPolicy
import com.example.data.PlaySessionState
import com.example.data.Settings
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun PlaySessionScreen(settings: Settings, onStart: (Int) -> Unit, onParent: () -> Unit, onEndParent: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val today = LocalDate.now().toString()
    val state = PlaySessionPolicy.state(settings, now, today)
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("遊戲時間小管家", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(18.dp))
        if (settings.isMaintenance(now)) {
            Text("家長暫時使用中", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text("用完請點結束；系統會回到休息倒數或下一輪選時間畫面。", textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onEndParent, modifier = Modifier.fillMaxWidth()) { Text("結束家長模式") }
        } else when (state) {
            PlaySessionState.READY -> {
                Text("這次想玩幾分鐘？", style = MaterialTheme.typography.titleLarge)
                Text("今天還有 ${PlaySessionPolicy.remainingToday(settings, today)} 分鐘", textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                PlaySessionPolicy.choices(settings, today).chunked(2).forEach { group ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        group.forEach { minutes ->
                            Button(onClick = { onStart(minutes) }) { Text("$minutes 分鐘") }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            PlaySessionState.RUNNING -> {
                val seconds = ((settings.playSessionEndAt - now).coerceAtLeast(0L) + 999L) / 1000L
                Text("本輪剩餘", style = MaterialTheme.typography.titleLarge)
                Text("%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.displayLarge)
            }
            PlaySessionState.RESTING -> {
                val seconds = ((settings.playRestEndAt - now).coerceAtLeast(0L) + 999L) / 1000L
                Text("休息時間", style = MaterialTheme.typography.titleLarge)
                Text("%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.displayLarge)
                Text("休息完自動開放下一輪，不需要家長重設。", textAlign = TextAlign.Center)
            }
            PlaySessionState.DAILY_LIMIT ->
                Text("今天的遊戲時間已用完，明天再玩！", textAlign = TextAlign.Center)
            PlaySessionState.DISABLED -> Text("請先由家長開啟單次遊戲功能。")
        }
        if (!settings.isMaintenance(now)) {
            Spacer(Modifier.height(36.dp))
            OutlinedButton(onClick = onParent, modifier = Modifier.fillMaxWidth()) {
                Text("家長管理（需要 PIN）")
            }
        }
    }
}
