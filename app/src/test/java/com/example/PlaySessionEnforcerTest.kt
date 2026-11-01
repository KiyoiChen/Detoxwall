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
        endAt: Long,
        foreground: String = "com.example.game",
        maintenance: Long = 0L,
    ) = EnforcementInput(
        now = now,
        dayIndex = 0,
        minuteOfDay = 600,
        today = "2026-11-01",
        foreground = foreground,
        settings = Settings(
            childMode = true,
            blackoutEnabled = false,
            playSessionEnabled = true,
            playSessionEndAt = endAt,
            maintenanceUntil = maintenance,
        ),
        limits = emptyMap(),
        usageMinutes = emptyMap(),
        grantedMinutes = emptyMap(),
        schedules = emptyList(),
        countedTotalMinutes = 0,
        protectedSurface = null,
        emergencyPackages = setOf("com.android.dialer"),
    )

    @Test fun pickerCoversEveryAppIncludingLauncherAndDetoxWall() {
        assertEquals(Decision.ChoosePlayTime, Enforcer.decide(input(now = 100L, endAt = 0L)))
        assertEquals(Decision.ChoosePlayTime, Enforcer.decide(input(100L, 0L, "com.launcher")))
        assertEquals(Decision.ChoosePlayTime, Enforcer.decide(input(100L, 0L, "com.example.detoxwall")))
        assertEquals(Decision.ChoosePlayTime, Enforcer.decide(input(100L, 0L, foreground = "")))
    }

    @Test fun parentMaintenanceTemporarilyHidesPicker() {
        assertEquals(Decision.None, Enforcer.decide(input(100L, 0L, maintenance = 5000L)))
    }

    @Test fun expiryLocksWholeDeviceAtDeadline() {
        assertEquals(Decision.None, Enforcer.decide(input(now = 999L, endAt = 1000L)))
        assertEquals(
            Decision.LockDevice(LockReason.PLAY_SESSION),
            Enforcer.decide(input(now = 1000L, endAt = 1000L)),
        )
    }

    @Test fun temporaryParentUnlockDoesNotResetRound() {
        assertEquals(Decision.None, Enforcer.decide(input(now = 1001L, endAt = 1000L, maintenance = 5000L)))
        assertEquals(
            Decision.LockDevice(LockReason.PLAY_SESSION),
            Enforcer.decide(input(now = 5000L, endAt = 1000L, maintenance = 5000L)),
        )
    }

    @Test fun emergencyDialerRemainsAccessible() {
        assertEquals(
            Decision.None,
            Enforcer.decide(input(now = 5000L, endAt = 1000L, foreground = "com.android.dialer")),
        )
    }
}
