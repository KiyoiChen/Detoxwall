package com.example

import com.example.usage.UsageCalculator
import com.example.usage.UsageKind.BACKGROUND
import com.example.usage.UsageKind.FOREGROUND
import com.example.usage.UsageKind.SCREEN_OFF
import com.example.usage.UsageSample
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageCalculatorTest {
    private val min = 60_000L
    private fun s(t: Long, pkg: String, k: com.example.usage.UsageKind) = UsageSample(t * min, pkg, k)

    @Test fun `simple session`() {
        val r = UsageCalculator.compute(listOf(s(10, "a", FOREGROUND), s(25, "a", BACKGROUND)), 0, 60 * min)
        assertEquals(15 * min, r["a"])
    }

    @Test fun `switching apps closes the previous one`() {
        val r = UsageCalculator.compute(listOf(s(0, "a", FOREGROUND), s(10, "b", FOREGROUND), s(30, "b", BACKGROUND)), 0, 60 * min)
        assertEquals(10 * min, r["a"])
        assertEquals(20 * min, r["b"])
    }

    @Test fun `app still open counts until now`() {
        val r = UsageCalculator.compute(listOf(s(50, "a", FOREGROUND)), 0, 60 * min)
        assertEquals(10 * min, r["a"])
    }

    @Test fun `app open since before range counts from range start`() {
        val r = UsageCalculator.compute(listOf(s(5, "a", BACKGROUND)), 0, 60 * min)
        assertEquals(5 * min, r["a"])
    }

    @Test fun `screen off ends the session`() {
        val r = UsageCalculator.compute(listOf(s(0, "a", FOREGROUND), s(7, "", SCREEN_OFF)), 0, 60 * min)
        assertEquals(7 * min, r["a"])
    }

    @Test fun `repeated foreground of same app does not double count`() {
        val r = UsageCalculator.compute(listOf(s(0, "a", FOREGROUND), s(5, "a", FOREGROUND), s(10, "a", BACKGROUND)), 0, 60 * min)
        assertEquals(10 * min, r["a"])
    }
}
