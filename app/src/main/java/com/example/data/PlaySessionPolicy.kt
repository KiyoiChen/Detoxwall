package com.example.data

/** A round runs to its deadline. After expiration, screen off/on opens the next picker. */
enum class PlaySessionState { DISABLED, READY, RUNNING, EXPIRED }

object PlaySessionPolicy {
    val optionsMinutes = listOf(5, 10, 15, 30)

    fun state(enabled: Boolean, endAt: Long, now: Long): PlaySessionState = when {
        !enabled -> PlaySessionState.DISABLED
        endAt <= 0L -> PlaySessionState.READY
        now < endAt -> PlaySessionState.RUNNING
        else -> PlaySessionState.EXPIRED
    }

    /** Do not recycle a round before its deadline or during parent maintenance. */
    fun resetOnScreenTransition(
        enabled: Boolean,
        endAt: Long,
        now: Long,
        parentMaintenance: Boolean,
    ): Boolean = !parentMaintenance && state(enabled, endAt, now) == PlaySessionState.EXPIRED

    fun deadline(now: Long, minutes: Int): Long {
        require(minutes in optionsMinutes) { "Unsupported play duration" }
        return Math.addExact(now, minutes * 60_000L)
    }
}
