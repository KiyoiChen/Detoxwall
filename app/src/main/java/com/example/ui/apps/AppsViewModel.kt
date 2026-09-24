package com.example.ui.apps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.container
import com.example.data.AppLimit
import com.example.usage.InstalledApp
import com.example.usage.UsageCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LimitedAppUi(
    val limit: AppLimit,
    val usedMinutes: Int,
    val extraMinutes: Int,
) {
    val allowedMinutes: Int get() = limit.dailyLimitMinutes + extraMinutes
    val progress: Float get() = if (allowedMinutes > 0) usedMinutes.toFloat() / allowedMinutes else 0f
    val remainingMinutes: Int get() = (allowedMinutes - usedMinutes).coerceAtLeast(0)
}

/** An installed app in the picker, with today's usage so heavy apps sort first. */
data class PickerApp(val app: InstalledApp, val usedMinutes: Int, val alreadyLimited: Boolean)

class AppsViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container
    private val usage = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val installed = MutableStateFlow<List<InstalledApp>>(emptyList())

    private val _pickerLoading = MutableStateFlow(false)
    val pickerLoading: StateFlow<Boolean> = _pickerLoading.asStateFlow()

    val limitedApps: StateFlow<List<LimitedAppUi>> =
        combine(c.repository.limits, usage, c.settings.settings) { limits, perApp, _ ->
            limits.map { limit ->
                LimitedAppUi(
                    limit = limit,
                    usedMinutes = UsageCalculator.toMinutes(perApp[limit.packageName] ?: 0L),
                    extraMinutes = c.settings.grantedMinutes(limit.packageName),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pickerApps: StateFlow<List<PickerApp>> =
        combine(installed, usage, c.repository.limits) { apps, perApp, limits ->
            val limited = limits.map { it.packageName }.toSet()
            apps.map { PickerApp(it, UsageCalculator.toMinutes(perApp[it.packageName] ?: 0L), it.packageName in limited) }
                .sortedWith(compareByDescending<PickerApp> { it.usedMinutes }.thenBy { it.app.label.lowercase() })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            while (true) {
                refreshUsage()
                delay(15_000)
            }
        }
    }

    fun refreshUsage() {
        viewModelScope.launch { usage.value = withContext(Dispatchers.IO) { c.usageReader.today() } }
    }

    fun loadInstalledApps() {
        if (installed.value.isNotEmpty()) return
        viewModelScope.launch {
            _pickerLoading.value = true
            installed.value = withContext(Dispatchers.IO) { c.installedApps.launchable() }
            _pickerLoading.value = false
        }
    }

    fun addLimit(app: InstalledApp, minutes: Int) = viewModelScope.launch {
        c.repository.saveLimit(AppLimit(packageName = app.packageName, appName = app.label, dailyLimitMinutes = minutes))
    }

    fun updateLimit(limit: AppLimit, minutes: Int) = viewModelScope.launch {
        c.repository.saveLimit(limit.copy(dailyLimitMinutes = minutes))
    }

    fun setEnabled(limit: AppLimit, enabled: Boolean) = viewModelScope.launch {
        c.repository.setLimitEnabled(limit.packageName, enabled)
    }

    fun delete(limit: AppLimit) = viewModelScope.launch { c.repository.deleteLimit(limit.packageName) }
}
