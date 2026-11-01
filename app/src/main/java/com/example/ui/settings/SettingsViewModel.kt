package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.container
import com.example.data.Settings
import com.example.data.SettingsStore
import com.example.data.ThemeMode
import com.example.security.PinResult
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container
    val settings: StateFlow<Settings> = c.settings.settings
    val isPinSet: StateFlow<Boolean> = c.pinManager.isPinSet

    fun setTheme(mode: ThemeMode) = c.settings.update { it.copy(themeMode = mode) }
    fun setRequirePinOnOpen(on: Boolean) = c.settings.update { it.copy(requirePinOnOpen = on) }
    fun setChildMode(on: Boolean) = c.settings.update { it.copy(childMode = on) }
    fun setBlackoutEnabled(on: Boolean) = c.settings.update { it.copy(blackoutEnabled = on) }
    fun setPlaySessionEnabled(on: Boolean) = c.settings.update {
        it.copy(playSessionEnabled = on)
    }
    /** Only call after a successful parent-PIN confirmation in SettingsScreen. */
    fun resetPlaySession() = c.settings.resetPlaySession()
    fun changeBlackoutLimit(delta: Int) = c.settings.update {
        it.copy(blackoutLimitMinutes = (it.blackoutLimitMinutes + delta).coerceIn(SettingsStore.MIN_BLACKOUT, SettingsStore.MAX_BLACKOUT))
    }

    fun setPin(pin: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(c.pinManager.setPin(pin)) }
    }

    fun verifyPin(pin: String, onResult: (PinResult) -> Unit) {
        viewModelScope.launch { onResult(c.pinManager.verify(pin)) }
    }
}
