package com.example.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ThemeMode
import com.example.security.PinResult
import com.example.ui.components.PinPad
import com.example.ui.components.SectionCard
import com.example.ui.components.enterAnimation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, contentPadding: PaddingValues, onOpenPermissions: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pinSet by viewModel.isPinSet.collectAsStateWithLifecycle()
    var pinFlow by remember { mutableStateOf(false) }
    var enableChildAfterPin by remember { mutableStateOf(false) }
    var confirmChildOff by remember { mutableStateOf(false) }
    var confirmPlayReset by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.enterAnimation()) }

        item {
            SectionCard(Modifier.enterAnimation(30)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ChildCare, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Child Mode", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (settings.childMode) "On. The parent PIN is needed to turn it off."
                            else "Locks the phone when the daily allowance or a lock schedule is reached.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.childMode,
                        onCheckedChange = { on ->
                            when {
                                !on -> confirmChildOff = true
                                pinSet -> viewModel.setChildMode(true)
                                // A PIN is required first, otherwise the child could just switch it off.
                                else -> { enableChildAfterPin = true; pinFlow = true }
                            }
                        },
                    )
                }
                if (settings.childMode) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("讓孩子選擇單次遊戲時間", style = MaterialTheme.typography.bodyMedium)
                            Text("時間到強制休息，休息結束自動選下一輪。",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = settings.playSessionEnabled,
                            onCheckedChange = viewModel::setPlaySessionEnabled)
                    }
                    if (settings.playSessionEnabled) {
                        Spacer(Modifier.height(10.dp))
                        Text("每輪後強制休息", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { viewModel.changeRestMinutes(-15) }) { Text("−15") }
                            Text(
                                "休息 ${settings.playRestMinutes} 分鐘",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { viewModel.changeRestMinutes(15) }) { Text("+15") }
                        }
                        Text("每日遊戲總額", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { viewModel.changeDailyPlayLimit(-15) }) { Text("−15") }
                            Text(
                                "每天 ${settings.playDailyLimitMinutes} 分鐘",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { viewModel.changeDailyPlayLimit(15) }) { Text("+15") }
                        }
                        Text(
                            "孩子的遊戲回合自動開放；家長使用不會重設今日額度。",
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = { confirmPlayReset = true }) { Text("家長：立即開放新一輪") }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Daily screen-time allowance", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = settings.blackoutEnabled, onCheckedChange = viewModel::setBlackoutEnabled)
                    }
                    if (settings.blackoutEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { viewModel.changeBlackoutLimit(-15) }) { Text("−15") }
                            Text(
                                "${settings.blackoutLimitMinutes} min per day",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { viewModel.changeBlackoutLimit(15) }) { Text("+15") }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(Modifier.enterAnimation(60)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                val options = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { i, (mode, label) ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        ) { Text(label) }
                    }
                }
            }
        }

        item {
            SectionCard(Modifier.enterAnimation(120), onClick = { pinFlow = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Parent PIN", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (pinSet) "Set. Stored only on this phone." else "Not set yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(if (pinSet) "Change" else "Set up", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (pinSet) {
            item {
                SectionCard(Modifier.enterAnimation(150)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Ask for PIN when opening", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Also asked again after 30 s in the background.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = settings.requirePinOnOpen, onCheckedChange = viewModel::setRequirePinOnOpen)
                    }
                }
            }
        }

        item {
            SectionCard(Modifier.enterAnimation(180), onClick = onOpenPermissions) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Permissions", style = MaterialTheme.typography.titleMedium)
                        Text("Usage access, accessibility, notifications, battery", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                }
            }
        }

        item {
            Text(
                "DetoxWall keeps all data on this phone. Nothing is uploaded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }

    if (pinFlow) PinSetupFlow(viewModel, requireCurrent = pinSet, onClose = {
        pinFlow = false
        if (enableChildAfterPin && viewModel.isPinSet.value) viewModel.setChildMode(true)
        enableChildAfterPin = false
    })
    if (confirmPlayReset) PinConfirmDialog(
        viewModel,
        title = "輸入家長 PIN 以重設遊戲回合",
        onConfirmed = { confirmPlayReset = false; viewModel.resetPlaySession() },
        onClose = { confirmPlayReset = false },
    )
    if (confirmChildOff) PinConfirmDialog(
        viewModel,
        title = "Enter parent PIN to turn off Child Mode",
        onConfirmed = { confirmChildOff = false; viewModel.setChildMode(false) },
        onClose = { confirmChildOff = false },
    )
}

/** Asks for the current parent PIN and calls [onConfirmed] once it verifies. */
@Composable
private fun PinConfirmDialog(viewModel: SettingsViewModel, title: String, onConfirmed: () -> Unit, onClose: () -> Unit) {
    var entry by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var errorKey by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(message ?: " ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(32.dp))
                PinPad(
                    value = entry,
                    errorKey = errorKey,
                    enabled = !busy,
                    onValueChange = { value ->
                        entry = value
                        message = null
                        if (value.length == 4 && !busy) {
                            busy = true
                            viewModel.verifyPin(value) { result ->
                                busy = false
                                when (result) {
                                    PinResult.Success, PinResult.NotSet -> onConfirmed()
                                    is PinResult.Wrong -> { message = "Wrong PIN. ${result.attemptsLeft} tries left."; entry = ""; errorKey++ }
                                    is PinResult.LockedOut -> { message = "Too many tries. Wait ${(result.remainingMs + 999) / 1000} s."; entry = ""; errorKey++ }
                                }
                            }
                        }
                    },
                )
                TextButton(onClick = onClose) { Text("Cancel") }
            }
        }
    }
}

private enum class PinStep { CURRENT, NEW, CONFIRM, DONE }

/** Full-screen flow: (current PIN →) new PIN → confirm, with animated steps. */
@Composable
private fun PinSetupFlow(viewModel: SettingsViewModel, requireCurrent: Boolean, onClose: () -> Unit) {
    var step by remember { mutableStateOf(if (requireCurrent) PinStep.CURRENT else PinStep.NEW) }
    var entry by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var errorKey by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    fun fail(text: String) { message = text; entry = ""; errorKey++ }

    fun onEntry(value: String) {
        entry = value
        message = null
        if (value.length < 4 || busy) return
        when (step) {
            PinStep.CURRENT -> {
                busy = true
                viewModel.verifyPin(value) { result ->
                    busy = false
                    when (result) {
                        PinResult.Success -> { entry = ""; step = PinStep.NEW }
                        is PinResult.Wrong -> fail("Wrong PIN. ${result.attemptsLeft} tries left.")
                        is PinResult.LockedOut -> fail("Too many tries. Wait ${(result.remainingMs + 999) / 1000} s.")
                        PinResult.NotSet -> { entry = ""; step = PinStep.NEW }
                    }
                }
            }
            PinStep.NEW -> { firstPin = value; entry = ""; step = PinStep.CONFIRM }
            PinStep.CONFIRM -> {
                if (value != firstPin) {
                    firstPin = ""
                    step = PinStep.NEW
                    fail("PINs didn't match. Try again.")
                } else {
                    busy = true
                    viewModel.setPin(value) { ok ->
                        busy = false
                        if (ok) step = PinStep.DONE else fail("Couldn't save the PIN.")
                    }
                }
            }
            PinStep.DONE -> Unit
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
                    },
                    label = "pinStep",
                ) { s ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when (s) {
                                PinStep.CURRENT -> "Enter your current PIN"
                                PinStep.NEW -> "Choose a 4-digit PIN"
                                PinStep.CONFIRM -> "Enter it again"
                                PinStep.DONE -> "PIN saved"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (s == PinStep.DONE) "Only stored on this phone, as a secure hash."
                            else message ?: " ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (message != null && s != PinStep.DONE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
                if (step == PinStep.DONE) {
                    Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Done") }
                } else {
                    PinPad(value = entry, onValueChange = ::onEntry, errorKey = errorKey, enabled = !busy)
                    TextButton(onClick = onClose) { Text("Cancel") }
                }
            }
        }
    }
}
