package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.data.repository.DetoxRepository
import com.example.ui.blocker.OverlayBlockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DetoxAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: DetoxRepository
    private var lastCheckedPackage: String? = null
    private var lastBlockedTimestamp: Long = 0L

    override fun onCreate() {
        super.onCreate()
        repository = DetoxRepository(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return
        if (pkgName == packageName) return // Ignore DetoxWall itself
        if (pkgName == "com.android.launcher" || pkgName.contains("launcher")) return

        val now = System.currentTimeMillis()
        if (pkgName == lastCheckedPackage && (now - lastBlockedTimestamp) < 3000) {
            return
        }
        lastCheckedPackage = pkgName

        serviceScope.launch {
            checkAppLimitsAndEnforce(pkgName)
        }
    }

    private suspend fun checkAppLimitsAndEnforce(pkgName: String) {
        val appLimit = repository.getLimitForPackage(pkgName) ?: return
        if (!appLimit.isRestricted) return

        val isChildMode = repository.securityManager.isChildModeEnabled()
        val isFocusMode = repository.focusSessionManager.isSessionActive()

        // If focus mode is active, block immediately
        if (isFocusMode) {
            triggerBlockScreen(
                packageName = pkgName,
                appName = appLimit.appName,
                mode = OverlayBlockActivity.MODE_FOCUS
            )
            return
        }

        // If child mode is active, enforce black screen when limit reached
        if (isChildMode) {
            val blankLimit = repository.securityManager.getScreenBlankLimitMinutes()
            val accumulated = repository.securityManager.getChildAccumulatedMinutes()
            val isBlankEnabled = repository.securityManager.isScreenBlankEnabled()

            if (appLimit.isLimitExceeded || (isBlankEnabled && accumulated >= blankLimit) || appLimit.dailyLimitMinutes <= 0) {
                triggerBlockScreen(
                    packageName = pkgName,
                    appName = appLimit.appName,
                    mode = OverlayBlockActivity.MODE_BLACKOUT
                )
                return
            }
        }

        // Adult mode check
        if (appLimit.isLimitExceeded) {
            triggerBlockScreen(
                packageName = pkgName,
                appName = appLimit.appName,
                mode = OverlayBlockActivity.MODE_ADULT
            )
        }
    }

    private fun triggerBlockScreen(packageName: String, appName: String, mode: String) {
        lastBlockedTimestamp = System.currentTimeMillis()
        val intent = Intent(this, OverlayBlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(OverlayBlockActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(OverlayBlockActivity.EXTRA_APP_NAME, appName)
            putExtra(OverlayBlockActivity.EXTRA_BLOCK_MODE, mode)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
