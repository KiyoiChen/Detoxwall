package com.example.usage

enum class UsageKind { FOREGROUND, BACKGROUND, SCREEN_OFF }

data class UsageSample(val timestamp: Long, val packageName: String, val kind: UsageKind)

/**
 * Turns a time-ordered stream of foreground/background events into foreground time per package.
 * Pure so it can be unit-tested; [UsageReader] feeds it from `UsageStatsManager.queryEvents`.
 */
object UsageCalculator {

    fun compute(samples: List<UsageSample>, rangeStart: Long, rangeEnd: Long): Map<String, Long> {
        val totals = HashMap<String, Long>()
        var current: String? = null
        var start = rangeStart
        var sawAny = false

        fun close(at: Long) {
            val pkg = current ?: return
            val clamped = at.coerceIn(rangeStart, rangeEnd)
            if (clamped > start) totals.merge(pkg, clamped - start, Long::plus)
            current = null
        }

        for (s in samples.sortedBy { it.timestamp }) {
            if (s.timestamp > rangeEnd) break
            when (s.kind) {
                UsageKind.FOREGROUND -> {
                    if (current != s.packageName) {
                        close(s.timestamp)
                        current = s.packageName
                        start = s.timestamp.coerceAtLeast(rangeStart)
                    }
                }
                UsageKind.BACKGROUND -> {
                    if (current == s.packageName) {
                        close(s.timestamp)
                    } else if (current == null && !sawAny) {
                        // App was already open when the range began (e.g. across midnight).
                        current = s.packageName
                        start = rangeStart
                        close(s.timestamp)
                    }
                }
                UsageKind.SCREEN_OFF -> close(s.timestamp)
            }
            sawAny = true
        }
        close(rangeEnd)
        return totals
    }

    fun toMinutes(ms: Long): Int = (ms / 60_000L).toInt()
}
