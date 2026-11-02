package com.example.data

/** Actual state is derived from persisted deadlines; changing apps or rebooting does not reset timers. */
enum class PlaySessionState { DISABLED, READY, RUNNING, RESTING, DAILY_LIMIT }

object PlaySessionPolicy {
    val optionsMinutes = listOf(5, 10, 15, 30)

    fun usedToday(settings: Settings, today: String): Int =
        if (settings.playDailyDate == today) settings.playDailyUsedMinutes.coerceAtLeast(0) else 0

    fun remainingToday(settings: Settings, today: String): Int =
        (settings.playDailyLimitMinutes - usedToday(settings, today)).coerceAtLeast(0)

    fun choices(settings: Settings, today: String): List<Int> =
        optionsMinutes.filter { it <= remainingToday(settings, today) }

    fun state(settings: Settings, now: Long, today: String): PlaySessionState = when {
        !settings.childMode || !settings.playSessionEnabled -> PlaySessionState.DISABLED
        remainingToday(settings, today) == 0 -> PlaySessionState.DAILY_LIMIT
        settings.playSessionEndAt > now -> PlaySessionState.RUNNING
        settings.playRestEndAt > now -> PlaySessionState.RESTING
        else -> PlaySessionState.READY
    }

    fun deadline(now: Long, minutes: Int): Long {
        require(minutes in optionsMinutes) { "Unsupported play duration" }
        return Math.addExact(now, minutes * 60_000L)
    }
}
