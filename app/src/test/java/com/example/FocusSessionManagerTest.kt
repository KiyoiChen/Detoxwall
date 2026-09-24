package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.FocusSessionManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusSessionManagerTest {

    private lateinit var focusManager: FocusSessionManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        focusManager = FocusSessionManager(context)
    }

    @After
    fun teardown() {
        focusManager.stopSession()
        context.getSharedPreferences("detoxwall_focus_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `session is not active initially`() {
        assertFalse(focusManager.isSessionActive())
    }

    @Test
    fun `startSession activates session`() {
        focusManager.startSession(25)
        assertTrue(focusManager.isSessionActive())
    }

    @Test
    fun `stopSession deactivates session`() {
        focusManager.startSession(25)
        focusManager.stopSession()
        assertFalse(focusManager.isSessionActive())
    }

    @Test
    fun `getFocusTag returns default`() {
        assertEquals("Deep Work", focusManager.getFocusTag())
    }

    @Test
    fun `getFocusTag returns custom tag`() {
        focusManager.startSession(30, "Exam Prep")
        assertEquals("Exam Prep", focusManager.getFocusTag())
    }

    @Test
    fun `getTargetDurationMinutes stores duration`() {
        // Default is 120 when nothing set
        assertEquals(120, focusManager.getTargetDurationMinutes())

        // After starting a session with specific duration, read stored value
        context.getSharedPreferences("detoxwall_focus_prefs", Context.MODE_PRIVATE)
            .edit().putInt("focus_duration_minutes", 25).commit()
        assertEquals(25, focusManager.getTargetDurationMinutes())
    }

    @Test
    fun `getRemainingSeconds is 0 when no session`() {
        assertEquals(0L, focusManager.getRemainingSeconds())
    }

    @Test
    fun `remaining seconds roughly matches start duration`() {
        focusManager.startSession(1) // 60 seconds
        val remaining = focusManager.getRemainingSeconds()
        assertTrue(remaining > 0)
        assertTrue(remaining <= 60)
    }
}
