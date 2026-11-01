package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val onboardingDone: Boolean = false,
    /** Ask for the parent PIN every time the app is opened (only when a PIN is set). */
    val requirePinOnOpen: Boolean = true,
    val childMode: Boolean = false,
    val tamperProtection: Boolean = true,
    val blackoutEnabled: Boolean = true,
    val blackoutLimitMinutes: Int = 60,
    /** Screen-time total at the moment the parent last unlocked a blackout (same day only). */
    val blackoutBaselineMinutes: Int = 0,
    val blackoutBaselineDate: String = "",
    val manualLock: Boolean = false,
    val sirenEnabled: Boolean = true,
    val sirenVolumePercent: Int = 90,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val focusEndAt: Long = 0L,
    val focusLabel: String = "Focus",
    val maintenanceUntil: Long = 0L,
    val playSessionEnabled: Boolean = false,
    val playSessionEndAt: Long = 0L,
    /** In-memory counter bumped when per-day grants change so collectors re-evaluate. */
    val revision: Int = 0,
) {
    fun isFocusActive(now: Long) = focusEndAt > now
    fun isMaintenance(now: Long) = maintenanceUntil > now
    fun baselineFor(today: String) = if (blackoutBaselineDate == today) blackoutBaselineMinutes else 0
}

/** SharedPreferences-backed settings exposed as a [StateFlow]; shared by the UI and the services. */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("detox_settings", Context.MODE_PRIVATE)
    private val lock = Any()

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings.asStateFlow()
    val current: Settings get() = _settings.value

    init {
        pruneOldDailyKeys()
    }

    fun update(transform: (Settings) -> Settings) = synchronized(lock) {
        val next = transform(_settings.value)
        write(next)
        _settings.value = next
    }

    /** Only one round may be started; reset requires a verified parent PIN in the UI. */
    fun startPlaySession(minutes: Int, now: Long = System.currentTimeMillis()): Boolean {
        if (minutes !in PlaySessionPolicy.optionsMinutes) return false
        var started = false
        update { previous ->
            if (!previous.childMode || !previous.playSessionEnabled || previous.playSessionEndAt != 0L) previous
            else {
                started = true
                previous.copy(playSessionEndAt = PlaySessionPolicy.deadline(now, minutes))
            }
        }
        return started
    }

    fun resetPlaySession() = update { it.copy(playSessionEndAt = 0L, maintenanceUntil = 0L) }

    // ── Per-day extra time & snoozes ─────────────────────────────────────────

    fun grantedMinutes(pkg: String, day: String = today()): Int = prefs.getInt("grant:$day:$pkg", 0)

    fun addGrant(pkg: String, minutes: Int, day: String = today()) = synchronized(lock) {
        prefs.edit { putInt("grant:$day:$pkg", grantedMinutes(pkg, day) + minutes) }
        _settings.value = _settings.value.copy(revision = _settings.value.revision + 1)
    }

    fun snoozesUsed(pkg: String, day: String = today()): Int = prefs.getInt("snooze:$day:$pkg", 0)

    fun useSnooze(pkg: String, minutes: Int, day: String = today()) = synchronized(lock) {
        prefs.edit { putInt("snooze:$day:$pkg", snoozesUsed(pkg, day) + 1) }
        addGrant(pkg, minutes, day)
    }

    // ── Service heartbeat (not part of the observable state) ────────────────

    var lastHeartbeat: Long
        get() = prefs.getLong("heartbeat", 0L)
        set(value) = prefs.edit { putLong("heartbeat", value) }

    private fun pruneOldDailyKeys() {
        val today = today()
        val stale = prefs.all.keys.filter {
            (it.startsWith("grant:") || it.startsWith("snooze:")) && it.split(':').getOrNull(1) != today
        }
        if (stale.isNotEmpty()) prefs.edit { stale.forEach(::remove) }
    }

    private fun read(): Settings = Settings(
        onboardingDone = prefs.getBoolean("onboarding_done", false),
        requirePinOnOpen = prefs.getBoolean("require_pin_on_open", true),
        childMode = prefs.getBoolean("child_mode", false),
        tamperProtection = prefs.getBoolean("tamper_protection", true),
        blackoutEnabled = prefs.getBoolean("blackout_enabled", true),
        blackoutLimitMinutes = prefs.getInt("blackout_limit", 60).coerceIn(MIN_BLACKOUT, MAX_BLACKOUT),
        blackoutBaselineMinutes = prefs.getInt("blackout_baseline", 0),
        blackoutBaselineDate = prefs.getString("blackout_baseline_date", "") ?: "",
        manualLock = prefs.getBoolean("manual_lock", false),
        sirenEnabled = prefs.getBoolean("siren_enabled", true),
        sirenVolumePercent = prefs.getInt("siren_volume", 90).coerceIn(10, 100),
        themeMode = runCatching { ThemeMode.valueOf(prefs.getString("theme", null)!!) }
            .getOrDefault(ThemeMode.DARK),
        focusEndAt = prefs.getLong("focus_end", 0L),
        focusLabel = prefs.getString("focus_label", "Focus") ?: "Focus",
        maintenanceUntil = prefs.getLong("maintenance_until", 0L),
        playSessionEnabled = prefs.getBoolean("play_session_enabled", false),
        playSessionEndAt = prefs.getLong("play_session_end_at", 0L),
    )

    private fun write(s: Settings) = prefs.edit(commit = true) {
        putBoolean("onboarding_done", s.onboardingDone)
        putBoolean("require_pin_on_open", s.requirePinOnOpen)
        putBoolean("child_mode", s.childMode)
        putBoolean("tamper_protection", s.tamperProtection)
        putBoolean("blackout_enabled", s.blackoutEnabled)
        putInt("blackout_limit", s.blackoutLimitMinutes.coerceIn(MIN_BLACKOUT, MAX_BLACKOUT))
        putInt("blackout_baseline", s.blackoutBaselineMinutes)
        putString("blackout_baseline_date", s.blackoutBaselineDate)
        putBoolean("manual_lock", s.manualLock)
        putBoolean("siren_enabled", s.sirenEnabled)
        putInt("siren_volume", s.sirenVolumePercent.coerceIn(10, 100))
        putString("theme", s.themeMode.name)
        putLong("focus_end", s.focusEndAt)
        putString("focus_label", s.focusLabel)
        putLong("maintenance_until", s.maintenanceUntil)
        putBoolean("play_session_enabled", s.playSessionEnabled)
        putLong("play_session_end_at", s.playSessionEndAt)
    }

    companion object {
        const val MIN_BLACKOUT = 5
        const val MAX_BLACKOUT = 300
        fun today(): String = LocalDate.now().toString()
    }
}
