package com.example

import com.example.data.model.ChildProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildProfileTest {

    @Test
    fun `default values are correct`() {
        val profile = ChildProfile()
        assertEquals("child_default", profile.id)
        assertEquals("Kid Profile", profile.name)
        assertEquals("kid_avatar_1", profile.avatarName)
        assertEquals(60, profile.dailyCapMinutes)
        assertEquals(20, profile.continuousLimitMinutes)
        assertEquals(0, profile.currentContinuousMinutes)
        assertEquals(0, profile.bypassAttempts)
        assertFalse(profile.isChildModeActive)
    }

    @Test
    fun `custom profile creation`() {
        val profile = ChildProfile(
            id = "teen_1",
            name = "Alex",
            avatarName = "avatar_teen",
            dailyCapMinutes = 120,
            continuousLimitMinutes = 45,
            currentContinuousMinutes = 10,
            bypassAttempts = 3,
            isChildModeActive = true
        )
        assertEquals("teen_1", profile.id)
        assertEquals("Alex", profile.name)
        assertEquals(120, profile.dailyCapMinutes)
        assertEquals(45, profile.continuousLimitMinutes)
        assertEquals(10, profile.currentContinuousMinutes)
        assertEquals(3, profile.bypassAttempts)
        assertTrue(profile.isChildModeActive)
    }

    @Test
    fun `copy modifies only specified fields`() {
        val original = ChildProfile(id = "p1", name = "Original", dailyCapMinutes = 60)
        val updated = original.copy(name = "Updated", dailyCapMinutes = 90)

        assertEquals("p1", updated.id)
        assertEquals("Updated", updated.name)
        assertEquals(90, updated.dailyCapMinutes)
        assertEquals(original.avatarName, updated.avatarName)
    }
}
