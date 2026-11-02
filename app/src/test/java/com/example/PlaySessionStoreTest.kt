package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PlaySessionPolicy
import com.example.data.PlaySessionState
import com.example.data.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlaySessionStoreTest {
    @Test fun automaticNextRoundPreservesBudgetAcrossProcessRestart() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("detox_settings", Context.MODE_PRIVATE).edit().clear().commit()
        val store = SettingsStore(ctx)
        store.update { it.copy(childMode = true, playSessionEnabled = true, playDailyLimitMinutes = 60) }
        val day = "2026-11-02"
        assertTrue(store.startPlaySession(5, now = 100_000L, day = day))
        assertFalse(store.startPlaySession(10, now = 200_000L, day = day))
        val reopened = SettingsStore(ctx)
        assertEquals(PlaySessionState.RESTING, PlaySessionPolicy.state(reopened.current, 400_000L, day))
        assertFalse(reopened.startPlaySession(10, now = 500_000L, day = day))
        val readyAt = 100_000L + 5 * 60_000L + 30 * 60_000L
        assertEquals(PlaySessionState.READY, PlaySessionPolicy.state(reopened.current, readyAt, day))
        assertTrue(reopened.startPlaySession(10, now = readyAt, day = day))
        assertEquals(15, reopened.current.playDailyUsedMinutes)
    }

    @Test fun parentTemporaryUseDoesNotEraseRestOrDailyBudget() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("detox_settings", Context.MODE_PRIVATE).edit().clear().commit()
        val store = SettingsStore(ctx)
        store.update { it.copy(childMode = true, playSessionEnabled = true) }
        assertTrue(store.startPlaySession(15, now = 100_000L, day = "2026-11-02"))
        val restAt = store.current.playRestEndAt
        val used = store.current.playDailyUsedMinutes
        store.startParentUse(now = 1_000_000L)
        assertTrue(store.current.maintenanceUntil > 1_000_000L)
        store.endParentUse()
        assertEquals(0L, store.current.maintenanceUntil)
        assertEquals(restAt, store.current.playRestEndAt)
        assertEquals(used, store.current.playDailyUsedMinutes)
        store.skipRestByParent()
        assertEquals(0L, store.current.playRestEndAt)
        assertEquals(used, store.current.playDailyUsedMinutes)
    }
}
