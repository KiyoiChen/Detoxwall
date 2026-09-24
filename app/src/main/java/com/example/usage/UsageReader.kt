package com.example.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

data class DayUsage(val date: LocalDate, val perAppMs: Map<String, Long>) {
    val totalMs: Long get() = perAppMs.values.sum()
}

/** Reads real foreground time from Android's usage events. Requires Usage Access. */
class UsageReader(private val context: Context, private val installedApps: InstalledApps) {
    private val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun today(now: Long = System.currentTimeMillis()): Map<String, Long> =
        forRange(startOfDay(LocalDate.now()), now)

    fun forDay(date: LocalDate, now: Long = System.currentTimeMillis()): Map<String, Long> {
        val start = startOfDay(date)
        if (start > now) return emptyMap()
        return forRange(start, minOf(startOfDay(date.plusDays(1)), now))
    }

    /**
     * Best available usage for a day: precise event data when the phone still has it, otherwise
     * Android's aggregated daily totals (kept much longer, slightly less exact).
     */
    fun forDayBest(date: LocalDate, now: Long = System.currentTimeMillis()): Map<String, Long> {
        val precise = forDay(date, now)
        if (precise.isNotEmpty() || date == LocalDate.now()) return precise
        if (!Permissions.hasUsageAccess(context)) return emptyMap()
        val start = startOfDay(date)
        val end = minOf(startOfDay(date.plusDays(1)), now)
        if (end <= start) return emptyMap()
        return runCatching { usm.queryAndAggregateUsageStats(start, end) }.getOrNull()
            ?.mapValues { it.value.totalTimeInForeground }
            ?.filterValues { it > 0 }
            ?: emptyMap()
    }

    fun lastDays(count: Int): List<DayUsage> {
        val today = LocalDate.now()
        return (count - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            DayUsage(date, forDay(date))
        }
    }

    /** Screen time that counts toward the Child Mode allowance: every app except ours and launchers. */
    fun countedTotalMinutes(perApp: Map<String, Long>): Int {
        val excluded = installedApps.launcherPackages() + context.packageName
        return UsageCalculator.toMinutes(perApp.filterKeys { it !in excluded }.values.sum())
    }

    private fun forRange(start: Long, end: Long): Map<String, Long> {
        if (!Permissions.hasUsageAccess(context) || end <= start) return emptyMap()
        val samples = ArrayList<UsageSample>(512)
        val events = runCatching { usm.queryEvents(start, end) }.getOrNull() ?: return emptyMap()
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val kind = when (e.eventType) {
                EVENT_RESUMED -> UsageKind.FOREGROUND
                EVENT_PAUSED, EVENT_STOPPED -> UsageKind.BACKGROUND
                EVENT_SCREEN_OFF, EVENT_KEYGUARD_SHOWN, EVENT_SHUTDOWN -> UsageKind.SCREEN_OFF
                else -> null
            } ?: continue
            samples += UsageSample(e.timeStamp, e.packageName ?: continue, kind)
        }
        return UsageCalculator.compute(samples, start, end)
    }

    companion object {
        // Raw constants so we don't need API-level checks (values are stable since API 21/28).
        private const val EVENT_RESUMED = 1
        private const val EVENT_PAUSED = 2
        private const val EVENT_SCREEN_OFF = 16
        private const val EVENT_KEYGUARD_SHOWN = 17
        private const val EVENT_STOPPED = 23
        private const val EVENT_SHUTDOWN = 26

        fun startOfDay(date: LocalDate): Long =
            date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
