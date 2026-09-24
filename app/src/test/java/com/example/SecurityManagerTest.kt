package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.SecurityManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityManagerTest {

    private lateinit var securityManager: SecurityManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        securityManager = SecurityManager(context)
    }

    @After
    fun teardown() {
        context.getSharedPreferences("detoxwall_secure_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `hasPinSet returns false initially`() {
        assertFalse(securityManager.hasPinSet())
    }

    @Test
    fun `setParentPin with valid pin returns true`() {
        assertTrue(securityManager.setParentPin("1234"))
    }

    @Test
    fun `setParentPin with short pin returns false`() {
        assertFalse(securityManager.setParentPin("123"))
    }

    @Test
    fun `setParentPin with exact 4 chars returns true`() {
        assertTrue(securityManager.setParentPin("0000"))
    }

    @Test
    fun `hasPinSet returns true after setting pin`() {
        securityManager.setParentPin("5678")
        assertTrue(securityManager.hasPinSet())
    }

    @Test
    fun `verifyPin succeeds with correct pin`() {
        securityManager.setParentPin("1234")
        assertTrue(securityManager.verifyPin("1234"))
    }

    @Test
    fun `verifyPin fails with incorrect pin`() {
        securityManager.setParentPin("1234")
        assertFalse(securityManager.verifyPin("9999"))
    }

    @Test
    fun `verifyPin fails when no pin is set`() {
        assertFalse(securityManager.verifyPin("1234"))
    }

    @Test
    fun `two pins produce different hashes due to random salt`() {
        securityManager.setParentPin("1234")
        val hash1 = context.getSharedPreferences("detoxwall_secure_prefs", Context.MODE_PRIVATE)
            .getString("parent_pin_hash", null)

        securityManager.setParentPin("5678")
        val hash2 = context.getSharedPreferences("detoxwall_secure_prefs", Context.MODE_PRIVATE)
            .getString("parent_pin_hash", null)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `same pin produces different hashes due to different salts`() {
        securityManager.setParentPin("1234")
        val hash1 = context.getSharedPreferences("detoxwall_secure_prefs", Context.MODE_PRIVATE)
            .getString("parent_pin_hash", null)

        securityManager.setParentPin("1234")
        val hash2 = context.getSharedPreferences("detoxwall_secure_prefs", Context.MODE_PRIVATE)
            .getString("parent_pin_hash", null)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `child mode defaults to disabled`() {
        assertFalse(securityManager.isChildModeEnabled())
    }

    @Test
    fun `setChildModeEnabled toggles state`() {
        securityManager.setChildModeEnabled(true)
        assertTrue(securityManager.isChildModeEnabled())

        securityManager.setChildModeEnabled(false)
        assertFalse(securityManager.isChildModeEnabled())
    }

    @Test
    fun `active profile ID defaults to child_default`() {
        assertEquals("child_default", securityManager.getActiveProfileId())
    }

    @Test
    fun `setChildModeEnabled stores profile ID`() {
        securityManager.setChildModeEnabled(true, "teen_profile")
        assertEquals("teen_profile", securityManager.getActiveProfileId())
    }

    @Test
    fun `screen blank limit defaults to 30`() {
        assertEquals(30, securityManager.getScreenBlankLimitMinutes())
    }

    @Test
    fun `setScreenBlankLimitMinutes clamps to min 5`() {
        securityManager.setScreenBlankLimitMinutes(1)
        assertEquals(5, securityManager.getScreenBlankLimitMinutes())
    }

    @Test
    fun `setScreenBlankLimitMinutes clamps to max 300`() {
        securityManager.setScreenBlankLimitMinutes(500)
        assertEquals(300, securityManager.getScreenBlankLimitMinutes())
    }

    @Test
    fun `setScreenBlankLimitMinutes stores valid value`() {
        securityManager.setScreenBlankLimitMinutes(120)
        assertEquals(120, securityManager.getScreenBlankLimitMinutes())
    }

    @Test
    fun `screen blank enabled defaults to true`() {
        assertTrue(securityManager.isScreenBlankEnabled())
    }

    @Test
    fun `setScreenBlankEnabled toggles state`() {
        securityManager.setScreenBlankEnabled(false)
        assertFalse(securityManager.isScreenBlankEnabled())

        securityManager.setScreenBlankEnabled(true)
        assertTrue(securityManager.isScreenBlankEnabled())
    }

    @Test
    fun `child accumulated minutes defaults to 0`() {
        assertEquals(0, securityManager.getChildAccumulatedMinutes())
    }

    @Test
    fun `setChildAccumulatedMinutes stores value`() {
        securityManager.setChildAccumulatedMinutes(45)
        assertEquals(45, securityManager.getChildAccumulatedMinutes())
    }

    @Test
    fun `setChildAccumulatedMinutes clamps negative to 0`() {
        securityManager.setChildAccumulatedMinutes(-10)
        assertEquals(0, securityManager.getChildAccumulatedMinutes())
    }

    @Test
    fun `resetChildAccumulatedMinutes resets to 0`() {
        securityManager.setChildAccumulatedMinutes(60)
        securityManager.resetChildAccumulatedMinutes()
        assertEquals(0, securityManager.getChildAccumulatedMinutes())
    }

    @Test
    fun `siren alarm enabled defaults to true`() {
        assertTrue(securityManager.isSirenAlarmEnabled())
    }

    @Test
    fun `setSirenAlarmEnabled toggles state`() {
        securityManager.setSirenAlarmEnabled(false)
        assertFalse(securityManager.isSirenAlarmEnabled())
    }

    @Test
    fun `siren volume defaults to 80`() {
        assertEquals(80, securityManager.getSirenVolumePercent())
    }

    @Test
    fun `setSirenVolumePercent clamps to min 10`() {
        securityManager.setSirenVolumePercent(5)
        assertEquals(10, securityManager.getSirenVolumePercent())
    }

    @Test
    fun `setSirenVolumePercent clamps to max 100`() {
        securityManager.setSirenVolumePercent(150)
        assertEquals(100, securityManager.getSirenVolumePercent())
    }

    @Test
    fun `setSirenVolumePercent stores valid value`() {
        securityManager.setSirenVolumePercent(55)
        assertEquals(55, securityManager.getSirenVolumePercent())
    }

    @Test
    fun `changePin succeeds with valid current and new pin`() {
        securityManager.setParentPin("1234")
        assertTrue(securityManager.changePin("1234", "5678"))
        assertTrue(securityManager.verifyPin("5678"))
        assertFalse(securityManager.verifyPin("1234"))
    }

    @Test
    fun `changePin fails with incorrect current pin`() {
        securityManager.setParentPin("1234")
        assertFalse(securityManager.changePin("0000", "5678"))
        assertTrue(securityManager.verifyPin("1234"))
        assertFalse(securityManager.verifyPin("5678"))
    }

    @Test
    fun `changePin fails with short new pin`() {
        securityManager.setParentPin("1234")
        assertFalse(securityManager.changePin("1234", "12"))
        assertTrue(securityManager.verifyPin("1234"))
    }

    @Test
    fun `removePin clears pin hash and salt`() {
        securityManager.setParentPin("1234")
        assertTrue(securityManager.hasPinSet())
        assertTrue(securityManager.removePin())
        assertFalse(securityManager.hasPinSet())
        assertFalse(securityManager.verifyPin("1234"))
    }
}
