package com.example.data

/** A single child-selected round stays spent until a parent explicitly resets it. */
enum class PlaySessionState { DISABLED, READY, RUNNING, EXPIRED }

object PlaySessionPolicy {
    val optionsMinutes = listOf(5, 10, 15, 30)

    fun state(enabled: Boolean, endAt: Long, now: Long): PlaySessionState = when {
        !enabled -> PlaySessionState.DISABLED
        endAt <= 0L -> PlaySessionState.READY
        now < endAt -> PlaySessionState.RUNNING
        else -> PlaySessionState.EXPIRED
    }

    fun deadline(now: Long, minutes: Int): Long {
        require(minutes in optionsMinutes) { "Unsupported play duration" }
        return Math.addExact(now, minutes * 60_000L)
    }
}
