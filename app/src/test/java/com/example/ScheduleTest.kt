package com.example

import com.example.data.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTest {
    private val mon = 0; private val tue = 1; private val sat = 5; private val sun = 6

    @Test fun `daytime window on weekdays`() {
        val school = Schedule(name = "School", startMinute = 8 * 60 + 30, endMinute = 15 * 60, daysMask = Schedule.WEEKDAYS)
        assertTrue(school.isActiveAt(mon, 9 * 60))
        assertFalse(school.isActiveAt(mon, 15 * 60)) // end is exclusive
        assertFalse(school.isActiveAt(sat, 9 * 60))
    }

    @Test fun `overnight window belongs to the day it started`() {
        val bed = Schedule(name = "Bed", startMinute = 21 * 60, endMinute = 7 * 60, daysMask = 1 shl mon)
        assertTrue(bed.isActiveAt(mon, 22 * 60))
        assertTrue(bed.isActiveAt(tue, 2 * 60))   // Monday night, after midnight
        assertFalse(bed.isActiveAt(mon, 2 * 60))  // Sunday night wasn't selected
    }

    @Test fun `sunday night rolls into monday`() {
        val bed = Schedule(name = "Bed", startMinute = 22 * 60, endMinute = 6 * 60, daysMask = 1 shl sun)
        assertTrue(bed.isActiveAt(mon, 1 * 60))
    }

    @Test fun `disabled schedule is never active`() {
        val s = Schedule(name = "x", startMinute = 0, endMinute = 23 * 60, daysMask = Schedule.EVERY_DAY, enabled = false)
        assertFalse(s.isActiveAt(mon, 60))
    }

    @Test fun `day summaries`() {
        assertEquals("Weekdays", Schedule.daysSummary(Schedule.WEEKDAYS))
        assertEquals("Weekends", Schedule.daysSummary(Schedule.WEEKENDS))
        assertEquals("Every day", Schedule.daysSummary(Schedule.EVERY_DAY))
        assertEquals("Mon, Wed", Schedule.daysSummary(0b101))
    }

    @Test fun `time formatting`() {
        assertEquals("12:00 AM", Schedule.formatMinute(0))
        assertEquals("9:30 PM", Schedule.formatMinute(21 * 60 + 30))
    }
}
