package com.example.enforcement

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.MainActivity
import com.example.R
import com.example.container
import com.example.data.SettingsStore
import com.example.data.TamperType
import com.example.ui.theme.DetoxTheme
import com.example.usage.UsageCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/** Watches the foreground app, asks [Enforcer] what to do, and shows the lock overlay. */
class DetoxAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlay: LockOverlay? = null
    private var screenReceiverRegistered = false
    /** Android screen-on/off is delivered while the accessibility service is alive. */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Timers are deadline-based and do not need screen-wake resets.
            requestEvaluate()
        }
    }

    private var foreground: String? = null
    private var protectedSurface: SurfaceKind? = null
    private var running = false
    private var pending = false
    private var lastTamperAt = 0L

    private var usageCache: Map<String, Long> = emptyMap()
    private var usageReadAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val c = container
        if (!screenReceiverRegistered) {
            registerReceiver(screenReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            })
            screenReceiverRegistered = true
        }
        overlay = LockOverlay(this) { decision ->
            DetoxTheme(mode = c.settings.current.themeMode) {
                LockScreenContent(
                    decision = decision,
                    settings = c.settings.current,
                    appLabel = c.installedApps::label,
                    pinManager = c.pinManager,
                    onCloseApp = { performGlobalAction(GLOBAL_ACTION_HOME) },
                    onEmergency = {
                        runCatching { startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    },
                    onParentUse = {
                        c.settings.startParentUse()
                        GuardianService.start(this)
                        requestEvaluate()
                    },
                    onParentSkipRest = {
                        c.settings.skipRestByParent()
                        GuardianService.start(this)
                        requestEvaluate()
                    },
                    onStartRound = { minutes ->
                        if (c.settings.startPlaySession(minutes)) requestEvaluate()
                    },
                )
            }
        }
        GuardianService.start(this)
        // Re-check every 5 s so a limit that runs out while the app is open still locks it.
        scope.launch {
            while (isActive) {
                requestEvaluate()
                delay(5_000)
            }
        }
        scope.launch { c.settings.settings.collect { requestEvaluate() } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        val s = container.settings.current

        if (s.childMode && s.tamperProtection && ProtectedSurfaceDetector.isCandidate(pkg)) {
            protectedSurface = ProtectedSurfaceDetector.detect(pkg, visibleTexts(), getString(R.string.app_name))
            if (protectedSurface != null) requestEvaluate()
        }

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        // Our own overlay and dialogs report our package; only MainActivity counts as "DetoxWall open".
        if (pkg == packageName && event.className?.toString() != MainActivity::class.java.name) return
        if (pkg == SYSTEM_UI || pkg in container.installedApps.inputMethodPackages()) return

        if (!ProtectedSurfaceDetector.isCandidate(pkg)) protectedSurface = null
        if (pkg != foreground) usageReadAt = 0L
        foreground = pkg
        requestEvaluate()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (screenReceiverRegistered) {
            unregisterReceiver(screenReceiver)
            screenReceiverRegistered = false
        }
        scope.cancel()
        overlay?.destroy()
        overlay = null
        super.onDestroy()
    }

    /** Runs evaluations one at a time; requests that arrive meanwhile collapse into one re-run. */
    private fun requestEvaluate() {
        if (running) { pending = true; return }
        running = true
        scope.launch {
            try {
                do {
                    pending = false
                    runCatching { apply(evaluate()) }
                } while (pending)
            } finally {
                running = false
            }
        }
    }

    private suspend fun evaluate(): Decision {
        val c = container
        val now = System.currentTimeMillis()
        if (now - usageReadAt > USAGE_CACHE_MS) {
            usageCache = withContext(Dispatchers.IO) { c.usageReader.today(now) }
            usageReadAt = now
        }
        val today = SettingsStore.today()
        val limits = c.repository.limitsSnapshot().associateBy { it.packageName }
        val time = LocalDateTime.now()
        return Enforcer.decide(
            EnforcementInput(
                now = now,
                dayIndex = time.dayOfWeek.value - 1,
                minuteOfDay = time.hour * 60 + time.minute,
                today = today,
                foreground = foreground,
                settings = c.settings.current,
                limits = limits,
                usageMinutes = usageCache.mapValues { UsageCalculator.toMinutes(it.value) },
                grantedMinutes = limits.keys.associateWith { c.settings.grantedMinutes(it, today) },
                schedules = c.repository.schedulesSnapshot(),
                countedTotalMinutes = c.usageReader.countedTotalMinutes(usageCache),
                protectedSurface = protectedSurface,
                emergencyPackages = setOfNotNull(c.installedApps.defaultDialer()),
            )
        )
    }

    private fun apply(decision: Decision) {
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
            overlay?.hide()
            return
        }
        when (decision) {
            Decision.None -> overlay?.hide()
            is Decision.BlockApp, is Decision.LockDevice, Decision.ChoosePlayTime -> overlay?.show(decision)
            is Decision.Tamper -> {
                overlay?.hide()
                protectedSurface = null
                val now = System.currentTimeMillis()
                if (now - lastTamperAt > 1_500) {
                    lastTamperAt = now
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    val type = if (decision.surface == SurfaceKind.UNINSTALL) TamperType.UNINSTALL_ATTEMPT
                    else TamperType.PROTECTED_SETTING
                    scope.launch { container.repository.logTamper(type) }
                }
            }
        }
    }

    private fun visibleTexts(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val out = ArrayList<String>()
        val stack = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (stack.isNotEmpty() && visited < 1_000 && out.size < 400) {
            val node = stack.removeLast()
            visited++
            node.text?.let { out += it.toString() }
            node.contentDescription?.let { out += it.toString() }
            for (i in 0 until node.childCount) node.getChild(i)?.let(stack::add)
        }
        return out
    }

    companion object {
        private const val SYSTEM_UI = "com.android.systemui"
        private const val USAGE_CACHE_MS = 3_000L
    }
}
