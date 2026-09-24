package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val enabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val MIN_MINUTES = 5
        const val MAX_MINUTES = 240
    }
}

/**
 * A repeating time window. Minutes are minutes-of-day (0..1439).
 * [daysMask] bit 0 = Monday … bit 6 = Sunday. When [endMinute] <= [startMinute] the window runs
 * overnight, and the part after midnight belongs to the day the window *started* on.
 */
@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startMinute: Int,
    val endMinute: Int,
    val daysMask: Int = WEEKDAYS,
    /** Child Mode only: lock the whole device instead of just limited apps. */
    val lockDevice: Boolean = false,
    val enabled: Boolean = true,
) {
    val isOvernight: Boolean get() = endMinute <= startMinute

    /** @param dayIndex 0 = Monday … 6 = Sunday. */
    fun isActiveAt(dayIndex: Int, minuteOfDay: Int): Boolean {
        if (!enabled || daysMask == 0) return false
        val today = hasDay(dayIndex)
        return if (!isOvernight) {
            today && minuteOfDay >= startMinute && minuteOfDay < endMinute
        } else {
            val yesterday = hasDay((dayIndex + 6) % 7)
            (today && minuteOfDay >= startMinute) || (yesterday && minuteOfDay < endMinute)
        }
    }

    fun hasDay(dayIndex: Int) = daysMask and (1 shl dayIndex) != 0

    companion object {
        const val WEEKDAYS = 0b0011111
        const val WEEKENDS = 0b1100000
        const val EVERY_DAY = 0b1111111
        val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        fun daysSummary(mask: Int): String = when (mask and EVERY_DAY) {
            0 -> "Never"
            EVERY_DAY -> "Every day"
            WEEKDAYS -> "Weekdays"
            WEEKENDS -> "Weekends"
            else -> DAY_LABELS.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.joinToString(", ")
        }

        fun formatMinute(minuteOfDay: Int): String {
            val h = minuteOfDay / 60
            val m = minuteOfDay % 60
            val suffix = if (h < 12) "AM" else "PM"
            val h12 = when (val x = h % 12) { 0 -> 12; else -> x }
            return "%d:%02d %s".format(h12, m, suffix)
        }
    }
}

enum class TamperType(val title: String) {
    BYPASS_ATTEMPT("Tried to leave the lock screen"),
    PROTECTED_SETTING("Opened a protected setting"),
    UNINSTALL_ATTEMPT("Tried to uninstall DetoxWall"),
    WRONG_PIN("Wrong parent PIN"),
    TIME_CHANGED("Device clock changed"),
    PROTECTION_INTERRUPTED("Protection was interrupted"),
    DEVICE_RESTARTED("Device restarted"),
}

@Entity(tableName = "tamper_events")
data class TamperEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: TamperType,
    val detail: String = "",
)
