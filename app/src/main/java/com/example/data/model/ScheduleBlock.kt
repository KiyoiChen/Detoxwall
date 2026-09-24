package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a scheduled restriction block.
 * daysOfWeek is a bitmask: bit 0 = Monday, bit 1 = Tuesday, ..., bit 6 = Sunday
 * Default 0b0111110 = Mon-Fri (bits 1-5)
 */
@Entity(tableName = "schedule_blocks")
data class ScheduleBlock(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    /** Bitmask: bit0=Mon, bit1=Tue, bit2=Wed, bit3=Thu, bit4=Fri, bit5=Sat, bit6=Sun */
    val daysOfWeek: Int = 0b0011111, // Mon-Fri by default
    val daysSummary: String = "Mon–Fri",
    val isEnabled: Boolean = true
) {
    companion object {
        const val MON = 1 shl 0
        const val TUE = 1 shl 1
        const val WED = 1 shl 2
        const val THU = 1 shl 3
        const val FRI = 1 shl 4
        const val SAT = 1 shl 5
        const val SUN = 1 shl 6

        val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        fun buildDaysSummary(daysOfWeek: Int): String {
            if (daysOfWeek == 0) return "No days"
            val selected = DAY_LABELS.filterIndexed { index, _ -> daysOfWeek and (1 shl index) != 0 }
            return when {
                selected.size == 7 -> "Every day"
                daysOfWeek == 0b0011111 -> "Mon–Fri"
                daysOfWeek == 0b1100000 -> "Weekends"
                selected.size <= 3 -> selected.joinToString(", ")
                else -> "${selected.first()}–${selected.last()}"
            }
        }

        fun isActiveNow(schedule: ScheduleBlock): Boolean {
            if (!schedule.isEnabled) return false
            val cal = java.util.Calendar.getInstance()
            // Calendar.DAY_OF_WEEK: 1=Sun, 2=Mon, ... 7=Sat
            val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
            // Convert to bitmask index: Mon=0, Tue=1, ..., Sun=6
            val bitIndex = when (dayOfWeek) {
                java.util.Calendar.MONDAY -> 0
                java.util.Calendar.TUESDAY -> 1
                java.util.Calendar.WEDNESDAY -> 2
                java.util.Calendar.THURSDAY -> 3
                java.util.Calendar.FRIDAY -> 4
                java.util.Calendar.SATURDAY -> 5
                java.util.Calendar.SUNDAY -> 6
                else -> return false
            }
            if (schedule.daysOfWeek and (1 shl bitIndex) == 0) return false
            val currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            val startMinutes = schedule.startHour * 60 + schedule.startMinute
            val endMinutes = schedule.endHour * 60 + schedule.endMinute
            return if (endMinutes > startMinutes) {
                currentMinutes in startMinutes until endMinutes
            } else {
                // Overnight schedule
                currentMinutes >= startMinutes || currentMinutes < endMinutes
            }
        }
    }
}
