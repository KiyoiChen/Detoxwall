package com.example

import com.example.data.Settings
import com.example.enforcement.Decision
import com.example.enforcement.EnforcementInput
import com.example.enforcement.Enforcer
import com.example.enforcement.LockReason
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaySessionEnforcerTest {
    private fun input(
        now: Long,
        endAt: Long = 0L,
        restAt: Long = 0L,
        used: Int = 0,
        foreground: String = "com.example.game",
        maintenance: Long = 0L,
        day: String = "2026-11-02",
    ) = EnforcementInput(
        now = now, dayIndex = 0, minuteOfDay = 600, today = day,
        foreground = foreground,
        settings = Settings(
            childMode = true, blackoutEnabled = false,
            playSessionEnabled = true,
            playSessionEndAt = endAt, playRestEndAt = restAt,
            playDailyUsedMinutes = used, playDailyDate = "2026-11-02",
            maintenanceUntil = maintenance,
        ),
        limits = emptyMap(), usageMinutes = emptyMap(), grantedMinutes = emptyMap(),
        schedules = emptyList(), countedTotalMinutes = 0, protectedSurface = null,
        emergencyPackages = setOf("com.android.dialer"),
    )

    @Test fun mustChooseBeforeUsingAnyApp() {
        assertEquals(Decision.ChoosePlayTime, Enforcer.decide(input(now = 100L)))
        assertEquals(Decision.ChoosePlayTime, Enforcer.decide(input(100L, foreground = "com.launcher")))
    }

    @Test fun playRestThenNewPickerWithoutParentReset() {
        assertEquals(Decision.None, Enforcer.decide(input(999L, endAt = 1000L, restAt = 4000L, used = 15)))
        assertEquals(
            Decision.LockDevice(LockReason.REST),
            Enforcer.decide(input(1000L, endAt = 1000L, restAt = 4000L, used = 15)),
        )
        assertEquals(Decision.ChoosePlayTime, Enforcer.decide(input(4000L, endAt = 1000L, restAt = 4000L, used = 15)))
    }

    @Test fun dailyLimitOverridesAnyFurtherRestOrNewGame() {
        assertEquals(
            Decision.LockDevice(LockReason.DAILY_PLAY),
            Enforcer.decide(input(1001L, endAt = 1000L, restAt = 9000L, used = 60)),
        )
        assertEquals(Decision.ChoosePlayTime, Enforcer.decide(input(1001L, used = 60, day = "2026-11-03")))
    }

    @Test fun parentTemporaryModeAllowsUseWithoutResettingTimers() {
        assertEquals(Decision.None, Enforcer.decide(input(1500L, endAt = 1000L, restAt = 6000L, used = 15, maintenance = 5000L)))
        assertEquals(
            Decision.LockDevice(LockReason.REST),
            Enforcer.decide(input(5000L, endAt = 1000L, restAt = 6000L, used = 15, maintenance = 5000L)),
        )
        assertEquals(Decision.ChoosePlayTime, Enforcer.decide(input(6000L, endAt = 1000L, restAt = 6000L, used = 15)))
    }

    @Test fun emergencyDialerIsExemptEvenDuringRestAndDailyLock() {
        assertEquals(
            Decision.None,
            Enforcer.decide(input(now = 5000L, endAt = 1000L, restAt = 6000L, used = 60, foreground = "com.android.dialer")),
        )
    }
}
