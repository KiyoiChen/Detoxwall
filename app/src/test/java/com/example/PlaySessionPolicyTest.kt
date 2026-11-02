package com.example

import com.example.data.PlaySessionPolicy
import com.example.data.PlaySessionState
import com.example.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaySessionPolicyTest {
    private val day = "2026-11-02"
    private fun settings(
        end: Long = 0L, rest: Long = 0L, used: Int = 0, total: Int = 60,
        date: String = day,
    ) = Settings(
        childMode = true, playSessionEnabled = true, playSessionEndAt = end,
        playRestEndAt = rest, playDailyUsedMinutes = used,
        playDailyDate = date, playDailyLimitMinutes = total,
    )

    @Test fun supportedDurationsOnly() {
        assertEquals(300_000L, PlaySessionPolicy.deadline(0L, 5))
        assertEquals(1_800_000L, PlaySessionPolicy.deadline(0L, 30))
        assertThrows(IllegalArgumentException::class.java) { PlaySessionPolicy.deadline(0L, 999) }
    }

    @Test fun oneRoundRestThenAutomaticNewSelection() {
        val s = settings(end = 1000L, rest = 4000L, used = 15)
        assertEquals(PlaySessionState.RUNNING, PlaySessionPolicy.state(s, 999L, day))
        assertEquals(PlaySessionState.RESTING, PlaySessionPolicy.state(s, 1000L, day))
        assertEquals(PlaySessionState.RESTING, PlaySessionPolicy.state(s, 3999L, day))
        assertEquals(PlaySessionState.READY, PlaySessionPolicy.state(s, 4000L, day))
    }

    @Test fun finalBookedRoundCompletesBeforeDailyLock() {
        val s = settings(end = 1000L, rest = 4000L, used = 60)
        assertEquals(PlaySessionState.RUNNING, PlaySessionPolicy.state(s, 999L, day))
        assertEquals(PlaySessionState.DAILY_LIMIT, PlaySessionPolicy.state(s, 1000L, day))
    }

    @Test fun dailyAllowanceResetsWhenCalendarDateChanges() {
        val s = settings(used = 60)
        assertEquals(PlaySessionState.DAILY_LIMIT, PlaySessionPolicy.state(s, 5000L, day))
        assertEquals(PlaySessionState.READY, PlaySessionPolicy.state(s, 5000L, "2026-11-03"))
    }

    @Test fun choicesNeverExceedDailyRemaining() {
        assertEquals(listOf(5, 10, 15), PlaySessionPolicy.choices(settings(used = 45), day))
        assertEquals(listOf(5), PlaySessionPolicy.choices(settings(used = 55), day))
        assertEquals(emptyList<Int>(), PlaySessionPolicy.choices(settings(used = 60), day))
    }
}
