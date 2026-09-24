package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.data.model.ScheduleBlock
import com.example.data.repository.DetoxRepository
import com.example.ui.blocker.OverlayBlockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * DetoxAccessibilityService:
 * Reacts to every foreground app change (TYPE_WINDOW_STATE_CHANGED).
 * For each switch, checks:
 *   1. Focus Mode active? → Block immediately (no escape via navigation)
 *   2. Active schedule right now? → Block immediately
 *   3. Child Mode active + limit exceeded? → Blackout block
 *   4. Adult mode + limit exceeded? → Soft 5-second wall
 *
 * Key enforcement loop:
 *   User opens blocked app → service fires → block screen shown
 *   User presses Recent Apps → navigates elsewhere → service fires again → re-block
 *   This creates the enforcement loop that makes navigation bypass ineffective.
 */
class DetoxAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: DetoxRepository

    private var lastCheckedPackage: String? = null
    private var lastBlockedTimestamp: Long = 0L
    private val debounceMs = 1500L // Reduced from 3s for faster enforcement

    // System packages that should never be blocked
    private val systemPackages = setOf(
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.huawei.android.launcher",
        "com.oneplus.launcher",
        "com.oppo.launcher",
        "com.android.settings",
        "android"
    )

    override fun onCreate() {
        super.onCreate()
        repository = DetoxRepository(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return

        // Never block DetoxWall itself or the block screen
        if (pkgName == packageName) return
        if (pkgName == OverlayBlockActivity::class.java.name) return

        // Never block system UI, launcher, settings
        if (isSystemPackage(pkgName)) return

        val now = System.currentTimeMillis()

        // Debounce: don't re-check the same package within debounceMs
        // EXCEPT if we haven't blocked recently — in that case allow re-check faster
        if (pkgName == lastCheckedPackage && (now - lastBlockedTimestamp) < debounceMs) {
            return
        }
        lastCheckedPackage = pkgName

        serviceScope.launch {
            checkAndEnforce(pkgName)
        }
    }

    private suspend fun checkAndEnforce(pkgName: String) {
        // ── 1. Focus Mode check (highest priority) ─────────────────────────
        val isFocusActive = repository.focusSessionManager.isSessionActive()
        if (isFocusActive) {
            val appLimit = repository.getLimitForPackage(pkgName)
            // In focus mode, block ALL restricted apps
            if (appLimit != null && appLimit.isRestricted) {
                triggerBlockScreen(pkgName, appLimit.appName, OverlayBlockActivity.MODE_FOCUS)
                return
            }
        }

        // ── 2. Active schedule check ──────────────────────────────────────
        val schedules = repository.allSchedules.firstOrNull() ?: emptyList()
        val isScheduleActive = schedules.any { ScheduleBlock.isActiveNow(it) }
        if (isScheduleActive) {
            val appLimit = repository.getLimitForPackage(pkgName)
            if (appLimit != null && appLimit.isRestricted) {
                val mode = if (repository.securityManager.isChildModeEnabled())
                    OverlayBlockActivity.MODE_BLACKOUT
                else
                    OverlayBlockActivity.MODE_ADULT
                triggerBlockScreen(pkgName, appLimit.appName, mode)
                return
            }
        }

        // ── 3. Per-app limit check ────────────────────────────────────────
        val appLimit = repository.getLimitForPackage(pkgName) ?: return
        if (!appLimit.isRestricted) return

        val isChildMode = repository.securityManager.isChildModeEnabled()

        if (isChildMode) {
            val accumulated = repository.securityManager.getChildAccumulatedMinutes()
            val blankLimit = repository.securityManager.getScreenBlankLimitMinutes()
            val isBlankEnabled = repository.securityManager.isScreenBlankEnabled()

            val shouldBlock = appLimit.isLimitExceeded ||
                    (isBlankEnabled && accumulated >= blankLimit)

            if (shouldBlock) {
                triggerBlockScreen(pkgName, appLimit.appName, OverlayBlockActivity.MODE_BLACKOUT)
                return
            }
        }

        // Adult soft block when limit exceeded
        if (!isChildMode && appLimit.isLimitExceeded) {
            triggerBlockScreen(pkgName, appLimit.appName, OverlayBlockActivity.MODE_ADULT)
        }
    }

    private fun triggerBlockScreen(packageName: String, appName: String, mode: String) {
        lastBlockedTimestamp = System.currentTimeMillis()
        val intent = Intent(this, OverlayBlockActivity::class.java).apply {
            // FLAG_ACTIVITY_NEW_TASK required from non-Activity context
            // FLAG_ACTIVITY_REORDER_TO_FRONT ensures block screen comes to front
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(OverlayBlockActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(OverlayBlockActivity.EXTRA_APP_NAME, appName)
            putExtra(OverlayBlockActivity.EXTRA_BLOCK_MODE, mode)
        }
        startActivity(intent)
    }

    private fun isSystemPackage(pkg: String): Boolean {
        return pkg in systemPackages ||
                pkg.startsWith("com.android.") ||
                pkg.startsWith("android.") ||
                pkg.contains("launcher", ignoreCase = true)
    }

    override fun onInterrupt() {
        // Service interrupted — enforcement paused until next event
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
