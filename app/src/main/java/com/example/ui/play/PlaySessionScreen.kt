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

@Composable
fun PlaySessionScreen(settings: Settings, onStart: (Int) -> Unit, onParent: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(settings.playSessionEndAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val state = PlaySessionPolicy.state(settings.playSessionEnabled, settings.playSessionEndAt, now)
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("遊戲時間小管家", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(18.dp))
        when (state) {
            PlaySessionState.READY -> {
                Text("這次想玩幾分鐘？", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(24.dp))
                PlaySessionPolicy.optionsMinutes.chunked(2).forEach { group ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        group.forEach { minutes ->
                            Button(onClick = { onStart(minutes) }) { Text("$minutes 分鐘") }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Text("選定後不能重新選擇；時間到需家長解鎖。", textAlign = TextAlign.Center)
            }
            PlaySessionState.RUNNING -> {
                val seconds = ((settings.playSessionEndAt - now).coerceAtLeast(0L) + 999L) / 1000L
                Text("還能玩", style = MaterialTheme.typography.titleLarge)
                Text("%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.displayLarge)
                Text("按手機 Home 鍵返回桌面，開始玩吧！", textAlign = TextAlign.Center)
            }
            PlaySessionState.EXPIRED -> {
                Text("時間到了！", style = MaterialTheme.typography.headlineSmall)
                Text("請找家長幫忙解鎖。", textAlign = TextAlign.Center)
            }
            PlaySessionState.DISABLED -> Text("請由家長開啟遊戲回合功能。")
        }
        Spacer(Modifier.height(36.dp))
        OutlinedButton(onClick = onParent, modifier = Modifier.fillMaxWidth()) {
            Text("家長管理（需要密碼）")
        }
    }
}
