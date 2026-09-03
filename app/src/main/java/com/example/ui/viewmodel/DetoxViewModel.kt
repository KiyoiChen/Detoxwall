package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppLimit
import com.example.data.model.ChildProfile
import com.example.data.model.ScheduleBlock
import com.example.data.model.UsageLog
import com.example.data.repository.DetoxRepository
import com.example.service.DetoxMonitoringService
import com.example.ui.blocker.OverlayBlockActivity
import com.example.util.AlertSoundManager
import com.example.util.AppItemInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetoxViewModel(application: Application) : AndroidViewModel(application) {

    val repository = DetoxRepository(application)
    val alertSoundManager = AlertSoundManager(application)

    val appLimits: StateFlow<List<AppLimit>> = repository.allLimits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usageLogs: StateFlow<List<UsageLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val childProfiles: StateFlow<List<ChildProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schedules: StateFlow<List<ScheduleBlock>> = repository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Permissions state
    private val _hasUsageStatsPermission = MutableStateFlow(false)
    val hasUsageStatsPermission: StateFlow<Boolean> = _hasUsageStatsPermission.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    // Focus Session state
    private val _isFocusActive = MutableStateFlow(false)
    val isFocusActive: StateFlow<Boolean> = _isFocusActive.asStateFlow()

    private val _focusRemainingSeconds = MutableStateFlow(0L)
    val focusRemainingSeconds: StateFlow<Long> = _focusRemainingSeconds.asStateFlow()

    private val _focusTag = MutableStateFlow("Deep Work")
    val focusTag: StateFlow<String> = _focusTag.asStateFlow()

    // Child Mode state
    private val _isChildModeActive = MutableStateFlow(false)
    val isChildModeActive: StateFlow<Boolean> = _isChildModeActive.asStateFlow()

    private val _hasParentPin = MutableStateFlow(false)
    val hasParentPin: StateFlow<Boolean> = _hasParentPin.asStateFlow()

    private val _screenBlankLimitMinutes = MutableStateFlow(repository.securityManager.getScreenBlankLimitMinutes())
    val screenBlankLimitMinutes: StateFlow<Int> = _screenBlankLimitMinutes.asStateFlow()

    private val _isScreenBlankEnabled = MutableStateFlow(repository.securityManager.isScreenBlankEnabled())
    val isScreenBlankEnabled: StateFlow<Boolean> = _isScreenBlankEnabled.asStateFlow()

    private val _isSirenAlarmEnabled = MutableStateFlow(repository.securityManager.isSirenAlarmEnabled())
    val isSirenAlarmEnabled: StateFlow<Boolean> = _isSirenAlarmEnabled.asStateFlow()

    private val _sirenVolumePercent = MutableStateFlow(repository.securityManager.getSirenVolumePercent())
    val sirenVolumePercent: StateFlow<Int> = _sirenVolumePercent.asStateFlow()

    val isSirenPlaying: StateFlow<Boolean> = alertSoundManager.isSirenPlaying

    // Available apps for picker
    private val _availableApps = MutableStateFlow<List<AppItemInfo>>(emptyList())
    val availableApps: StateFlow<List<AppItemInfo>> = _availableApps.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultDataIfNeeded()
            refreshPermissions()
            _availableApps.value = repository.usageStatsHelper.getAvailableApps()
            updateChildModeState()
            updateFocusState()
            try {
                DetoxMonitoringService.startMonitoring(getApplication())
            } catch (_: Exception) {}
        }

        // Periodic ticker for active focus session
        viewModelScope.launch {
            while (true) {
                delay(1000)
                updateFocusState()
            }
        }
    }

    fun refreshPermissions() {
        val helper = repository.usageStatsHelper
        _hasUsageStatsPermission.value = helper.hasUsageStatsPermission()
        _hasOverlayPermission.value = helper.hasOverlayPermission()
        _isAccessibilityEnabled.value = helper.isAccessibilityServiceEnabled()
        updateChildModeState()
    }

    private fun updateChildModeState() {
        _isChildModeActive.value = repository.securityManager.isChildModeEnabled()
        _hasParentPin.value = repository.securityManager.hasPinSet()
    }

    private fun updateFocusState() {
        val manager = repository.focusSessionManager
        val active = manager.isSessionActive()
        _isFocusActive.value = active
        if (active) {
            _focusRemainingSeconds.value = manager.getRemainingSeconds()
            _focusTag.value = manager.getFocusTag()
        } else {
            _focusRemainingSeconds.value = 0L
        }
    }

    // App Limit actions
    fun setAppDailyLimit(packageName: String, minutes: Int) {
        viewModelScope.launch {
            repository.updateAppLimitMinutes(packageName, minutes)
        }
    }

    fun toggleAppRestriction(packageName: String, isRestricted: Boolean) {
        viewModelScope.launch {
            repository.toggleAppRestriction(packageName, isRestricted)
        }
    }

    fun addNewAppLimit(appInfo: AppItemInfo, limitMinutes: Int) {
        viewModelScope.launch {
            val limit = AppLimit(
                packageName = appInfo.packageName,
                appName = appInfo.appName,
                dailyLimitMinutes = limitMinutes,
                currentUsageMinutes = 0,
                isRestricted = true,
                category = appInfo.category,
                iconKey = appInfo.iconKey
            )
            repository.saveOrUpdateLimit(limit)
        }
    }

    fun deleteAppLimit(packageName: String) {
        viewModelScope.launch {
            repository.deleteLimit(packageName)
        }
    }

    fun simulateUsageMinute(packageName: String) {
        viewModelScope.launch {
            repository.addUsageMinute(packageName, 5)
        }
    }

    // Focus session
    fun startFocusSession(durationMinutes: Int, tag: String = "Deep Work") {
        repository.focusSessionManager.startSession(durationMinutes, tag)
        updateFocusState()
    }

    fun stopFocusSession() {
        repository.focusSessionManager.stopSession()
        updateFocusState()
    }

    // Child Mode PIN & Activation
    fun setParentPin(pin: String): Boolean {
        val success = repository.securityManager.setParentPin(pin)
        if (success) {
            _hasParentPin.value = true
        }
        return success
    }

    fun verifyParentPin(pin: String): Boolean {
        return repository.securityManager.verifyPin(pin)
    }

    fun toggleChildMode(profileId: String, enable: Boolean) {
        viewModelScope.launch {
            repository.setChildMode(profileId, enable)
            updateChildModeState()
        }
    }

    fun saveChildProfile(profile: ChildProfile) {
        viewModelScope.launch {
            repository.saveChildProfile(profile)
        }
    }

    fun setScreenBlankLimit(minutes: Int) {
        val clamped = minutes.coerceIn(5, 300)
        repository.securityManager.setScreenBlankLimitMinutes(clamped)
        _screenBlankLimitMinutes.value = clamped
    }

    fun setScreenBlankEnabled(enabled: Boolean) {
        repository.securityManager.setScreenBlankEnabled(enabled)
        _isScreenBlankEnabled.value = enabled
    }

    fun setSirenAlarmEnabled(enabled: Boolean) {
        repository.securityManager.setSirenAlarmEnabled(enabled)
        _isSirenAlarmEnabled.value = enabled
    }

    fun setSirenVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(10, 100)
        repository.securityManager.setSirenVolumePercent(clamped)
        _sirenVolumePercent.value = clamped
    }

    fun previewBlackScreen(context: Context) {
        val intent = Intent(context, OverlayBlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(OverlayBlockActivity.EXTRA_PACKAGE_NAME, "com.google.android.youtube")
            putExtra(OverlayBlockActivity.EXTRA_APP_NAME, "YouTube")
            putExtra(OverlayBlockActivity.EXTRA_BLOCK_MODE, OverlayBlockActivity.MODE_BLACKOUT)
        }
        context.startActivity(intent)
    }

    fun resetBypassAttempts(profileId: String) {
        viewModelScope.launch {
            repository.resetBypassAttempts(profileId)
        }
    }

    fun triggerSimulatedAlarm() {
        alertSoundManager.playBypassAlert(viewModelScope, volumePercent = _sirenVolumePercent.value)
    }

    fun stopSimulatedAlarm() {
        alertSoundManager.stopAlert()
    }

    // Schedules
    fun toggleSchedule(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleSchedule(id, isEnabled)
        }
    }

    fun addSchedule(title: String, startH: Int, startM: Int, endH: Int, endM: Int, days: String) {
        viewModelScope.launch {
            repository.addSchedule(
                ScheduleBlock(
                    title = title,
                    startHour = startH,
                    startMinute = startM,
                    endHour = endH,
                    endMinute = endM,
                    daysSummary = days,
                    isEnabled = true
                )
            )
        }
    }

    // Testing Overlay Screen Preview
    fun previewBlockScreen(context: Context, appName: String, packageName: String, mode: String) {
        val intent = Intent(context, OverlayBlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(OverlayBlockActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(OverlayBlockActivity.EXTRA_APP_NAME, appName)
            putExtra(OverlayBlockActivity.EXTRA_BLOCK_MODE, mode)
        }
        context.startActivity(intent)
    }

    // Settings Navigation
    fun openUsageSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openOverlaySettings(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        alertSoundManager.release()
    }
}
