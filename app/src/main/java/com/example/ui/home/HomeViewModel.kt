package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.container
import com.example.ui.permissions.currentPermissions
import com.example.usage.UsageCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container

    private val usage = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val missingPermissions = MutableStateFlow(0)
    private val clock = MutableStateFlow(System.currentTimeMillis())

    val state: StateFlow<HomeUiState> = combine(
        c.settings.settings, c.repository.limits, usage, missingPermissions, clock,
    ) { settings, limits, perApp, missing, now ->
        val total = c.usageReader.countedTotalMinutes(perApp)
        val goal = if (settings.childMode && settings.blackoutEnabled) settings.blackoutLimitMinutes
        else limits.filter { it.enabled }.sumOf { it.dailyLimitMinutes }
        val focusLeftMs = (settings.focusEndAt - now).coerceAtLeast(0)
        HomeUiState(
            totalMinutes = total,
            goalMinutes = goal,
            childMode = settings.childMode,
            focusRemainingSeconds = ((focusLeftMs + 999) / 1_000).toInt(),
            topApps = perApp.entries
                .filter { it.key != app.packageName && it.key !in c.installedApps.launcherPackages() }
                .map { TopApp(it.key, c.installedApps.label(it.key), UsageCalculator.toMinutes(it.value)) }
                .filter { it.minutes > 0 }
                .sortedByDescending { it.minutes }
                .take(5),
            missingPermissions = missing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // 1 s clock while a focus session runs, so the countdown moves smoothly.
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                if (c.settings.current.focusEndAt > System.currentTimeMillis() || state.value.focusRemainingSeconds > 0) {
                    clock.value = System.currentTimeMillis()
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(15_000)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            usage.value = withContext(Dispatchers.IO) { c.usageReader.today() }
            missingPermissions.value = currentPermissions(app).count { it.kind.essential && !it.granted }
            clock.value = System.currentTimeMillis()
        }
    }

    fun startFocus(minutes: Int) {
        c.settings.update { it.copy(focusEndAt = System.currentTimeMillis() + minutes * 60_000L) }
        clock.value = System.currentTimeMillis()
    }

    fun stopFocus() {
        c.settings.update { it.copy(focusEndAt = 0L) }
        clock.value = System.currentTimeMillis()
    }
}
