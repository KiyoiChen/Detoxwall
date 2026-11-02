package com.example.enforcement

import com.example.data.AppLimit
import com.example.data.Schedule
import com.example.data.Settings
import com.example.data.PlaySessionPolicy
import com.example.data.PlaySessionState

enum class LockReason { MANUAL, ALLOWANCE, SCHEDULE, REST, DAILY_PLAY }
enum class BlockReason { FOCUS, SCHEDULE, LIMIT, PLAY_NOT_STARTED }
enum class SurfaceKind { SETTINGS, UNINSTALL }

sealed interface Decision {
    data object None : Decision
    /** Full-screen input gate: no launcher/game access until a round is selected. */
    data object ChoosePlayTime : Decision
    data class LockDevice(val reason: LockReason, val detail: String = "") : Decision
    data class BlockApp(val packageName: String, val reason: BlockReason, val detail: String = "") : Decision
    data class Tamper(val surface: SurfaceKind) : Decision
}

data class EnforcementInput(
    val now: Long,
    /** 0 = Monday … 6 = Sunday. */
    val dayIndex: Int,
    val minuteOfDay: Int,
    val today: String,
    val foreground: String?,
    val settings: Settings,
    val limits: Map<String, AppLimit>,
    val usageMinutes: Map<String, Int>,
    val grantedMinutes: Map<String, Int>,
    val schedules: List<Schedule>,
    val countedTotalMinutes: Int,
    val protectedSurface: SurfaceKind?,
    val emergencyPackages: Set<String>,
)

/** The whole enforcement policy as one pure function — see design.md §3 for the table. */
object Enforcer {

    fun decide(input: EnforcementInput): Decision = with(input) {
        val strict = settings.childMode
        val active = schedules.filter { it.isActiveAt(dayIndex, minuteOfDay) }

        // 1. Tamper protection
        if (strict && settings.tamperProtection && !settings.isMaintenance(now) && protectedSurface != null) {
            return Decision.Tamper(protectedSurface)
        }

        // 2. Emergency dialer always reachable
        if (foreground != null && foreground in emergencyPackages) return Decision.None

        // 3. Whole-device lock (Child Mode only; paused while a parent unlock is running)
        if (strict && !settings.isMaintenance(now)) {
            if (settings.manualLock) return Decision.LockDevice(LockReason.MANUAL)
            active.firstOrNull { it.lockDevice }?.let {
                return Decision.LockDevice(LockReason.SCHEDULE, it.name)
            }
            if (settings.blackoutEnabled && blackoutUsed(input) >= settings.blackoutLimitMinutes) {
                return Decision.LockDevice(LockReason.ALLOWANCE)
            }
            if (settings.playSessionEnabled) {
                when (PlaySessionPolicy.state(settings, now, today)) {
                    PlaySessionState.DAILY_LIMIT -> return Decision.LockDevice(LockReason.DAILY_PLAY)
                    PlaySessionState.RESTING -> return Decision.LockDevice(LockReason.REST)
                    PlaySessionState.READY -> return Decision.ChoosePlayTime
                    else -> Unit
                }
            }
        }

        // Existing per-app blocking policy.
        val pkg = foreground ?: return Decision.None
        val limit = limits[pkg]?.takeIf { it.enabled } ?: return Decision.None

        if (settings.isFocusActive(now)) return Decision.BlockApp(pkg, BlockReason.FOCUS, settings.focusLabel)

        active.firstOrNull { !it.lockDevice || !strict }?.let {
            return Decision.BlockApp(pkg, BlockReason.SCHEDULE, it.name)
        }

        val used = usageMinutes[pkg] ?: 0
        val allowed = limit.dailyLimitMinutes + (grantedMinutes[pkg] ?: 0)
        if (used >= allowed) return Decision.BlockApp(pkg, BlockReason.LIMIT)

        Decision.None
    }

    fun blackoutUsed(input: EnforcementInput): Int =
        (input.countedTotalMinutes - input.settings.baselineFor(input.today)).coerceAtLeast(0)
}
