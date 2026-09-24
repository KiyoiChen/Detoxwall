package com.example

import com.example.data.model.AppLimit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLimitTest {

    private fun createLimit(
        dailyLimit: Int = 30,
        currentUsage: Int = 0,
        isRestricted: Boolean = true
    ) = AppLimit(
        packageName = "com.test.app",
        appName = "Test App",
        dailyLimitMinutes = dailyLimit,
        currentUsageMinutes = currentUsage,
        isRestricted = isRestricted
    )

    @Test
    fun `isLimitExceeded is false when usage is below limit`() {
        val limit = createLimit(dailyLimit = 60, currentUsage = 30)
        assertFalse(limit.isLimitExceeded)
    }

    @Test
    fun `isLimitExceeded is true when usage equals limit`() {
        val limit = createLimit(dailyLimit = 60, currentUsage = 60)
        assertTrue(limit.isLimitExceeded)
    }

    @Test
    fun `isLimitExceeded is true when usage exceeds limit`() {
        val limit = createLimit(dailyLimit = 30, currentUsage = 45)
        assertTrue(limit.isLimitExceeded)
    }

    @Test
    fun `isLimitExceeded is false when not restricted`() {
        val limit = createLimit(dailyLimit = 30, currentUsage = 50, isRestricted = false)
        assertFalse(limit.isLimitExceeded)
    }

    @Test
    fun `isLimitExceeded is false when dailyLimit is 0`() {
        val limit = createLimit(dailyLimit = 0, currentUsage = 10)
        assertFalse(limit.isLimitExceeded)
    }

    @Test
    fun `remainingMinutes returns difference when usage below limit`() {
        val limit = createLimit(dailyLimit = 60, currentUsage = 25)
        assertEquals(35, limit.remainingMinutes)
    }

    @Test
    fun `remainingMinutes returns 0 when usage equals limit`() {
        val limit = createLimit(dailyLimit = 60, currentUsage = 60)
        assertEquals(0, limit.remainingMinutes)
    }

    @Test
    fun `remainingMinutes returns 0 when usage exceeds limit`() {
        val limit = createLimit(dailyLimit = 30, currentUsage = 50)
        assertEquals(0, limit.remainingMinutes)
    }

    @Test
    fun `progressPercent is 0 when no usage`() {
        val limit = createLimit(dailyLimit = 60, currentUsage = 0)
        assertEquals(0f, limit.progressPercent)
    }

    @Test
    fun `progressPercent is half at half usage`() {
        val limit = createLimit(dailyLimit = 60, currentUsage = 30)
        assertEquals(0.5f, limit.progressPercent)
    }

    @Test
    fun `progressPercent is full at full usage`() {
        val limit = createLimit(dailyLimit = 60, currentUsage = 60)
        assertEquals(1.0f, limit.progressPercent)
    }

    @Test
    fun `progressPercent is capped at full when over limit`() {
        val limit = createLimit(dailyLimit = 30, currentUsage = 50)
        assertEquals(1.0f, limit.progressPercent)
    }

    @Test
    fun `progressPercent is 0 when dailyLimit is 0`() {
        val limit = createLimit(dailyLimit = 0, currentUsage = 10)
        assertEquals(0f, limit.progressPercent)
    }

    @Test
    fun `default values are correct`() {
        val limit = AppLimit(packageName = "com.test", appName = "Test")
        assertEquals("com.test", limit.packageName)
        assertEquals("Test", limit.appName)
        assertEquals(30, limit.dailyLimitMinutes)
        assertEquals(0, limit.currentUsageMinutes)
        assertTrue(limit.isRestricted)
        assertEquals("Social", limit.category)
        assertEquals("social", limit.iconKey)
    }
}
