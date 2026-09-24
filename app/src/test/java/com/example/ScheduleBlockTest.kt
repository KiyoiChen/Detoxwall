package com.example

import com.example.data.model.ScheduleBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleBlockTest {

    @Test
    fun `default values are correct`() {
        val schedule = ScheduleBlock(title = "Test Schedule", startHour = 8, startMinute = 0, endHour = 17, endMinute = 0)
        assertEquals(0L, schedule.id)
        assertEquals("Test Schedule", schedule.title)
        assertEquals(8, schedule.startHour)
        assertEquals(0, schedule.startMinute)
        assertEquals(17, schedule.endHour)
        assertEquals(0, schedule.endMinute)
        assertEquals("Mon–Fri", schedule.daysSummary)
        assertTrue(schedule.isEnabled)
    }

    @Test
    fun `schedule with custom values`() {
        val schedule = ScheduleBlock(
            id = 1,
            title = "Bedtime",
            startHour = 21,
            startMinute = 30,
            endHour = 7,
            endMinute = 0,
            daysSummary = "Every day",
            isEnabled = false
        )
        assertEquals(1L, schedule.id)
        assertEquals("Bedtime", schedule.title)
        assertEquals(21, schedule.startHour)
        assertEquals(30, schedule.startMinute)
        assertEquals(7, schedule.endHour)
        assertEquals(0, schedule.endMinute)
        assertEquals("Every day", schedule.daysSummary)
        assertFalse(schedule.isEnabled)
    }

    @Test
    fun `overnight schedule has end hour less than start hour`() {
        val schedule = ScheduleBlock(
            title = "Overnight Block",
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 30
        )
        assertTrue(schedule.endHour < schedule.startHour)
    }
}
