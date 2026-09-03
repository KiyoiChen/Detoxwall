package com.example.util

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.data.model.AppLimit
import com.example.data.model.UsageLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UsageStatsHelper(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    /**
     * Checks if Usage Access permission is granted.
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Checks if System Alert Window (Overlay) permission is granted.
     */
    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Checks if Accessibility Service is enabled.
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        val expectedServiceName = "${context.packageName}/com.example.service.DetoxAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        return enabledServices.contains(context.packageName)
    }

    /**
     * Queries real UsageStats from start of today (midnight) to now.
     */
    fun getTodayUsageMinutes(): Map<String, Int> {
        if (!hasUsageStatsPermission() || usageStatsManager == null) {
            return emptyMap()
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        val usageMap = mutableMapOf<String, Int>()
        for (stat in stats) {
            val totalMinutes = (stat.totalTimeInForeground / 60000).toInt()
            if (totalMinutes > 0) {
                val current = usageMap[stat.packageName] ?: 0
                usageMap[stat.packageName] = current + totalMinutes
            }
        }
        return usageMap
    }

    /**
     * Returns list of popular detox apps + installed apps on device for the App Picker.
     */
    fun getAvailableApps(): List<AppItemInfo> {
        val pm = context.packageManager
        val popularPresets = listOf(
            AppItemInfo("com.instagram.android", "Instagram", "Social", "instagram", true, 30),
            AppItemInfo("com.zhiliaoapp.musically", "TikTok", "Social", "tiktok", true, 30),
            AppItemInfo("com.google.android.youtube", "YouTube", "Video", "youtube", true, 45),
            AppItemInfo("com.twitter.android", "X (Twitter)", "Social", "twitter", true, 30),
            AppItemInfo("com.facebook.katana", "Facebook", "Social", "facebook", true, 25),
            AppItemInfo("com.reddit.frontpage", "Reddit", "Social", "reddit", true, 30),
            AppItemInfo("com.snapchat.android", "Snapchat", "Social", "snapchat", true, 20),
            AppItemInfo("com.netflix.mediaclient", "Netflix", "Entertainment", "netflix", true, 60),
            AppItemInfo("com.android.chrome", "Chrome", "Browser", "browser", false, 60)
        )

        val installedList = mutableListOf<AppItemInfo>()
        try {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) continue
                val label = info.loadLabel(pm).toString()
                if (popularPresets.none { it.packageName == pkg }) {
                    installedList.add(
                        AppItemInfo(
                            packageName = pkg,
                            appName = label,
                            category = guessCategory(label, pkg),
                            iconKey = "app_default",
                            isPreset = false,
                            defaultLimitMinutes = 30
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return popularPresets + installedList.sortedBy { it.appName }
    }

    private fun guessCategory(name: String, pkg: String): String {
        val lower = (name + pkg).lowercase()
        return when {
            lower.contains("game") || lower.contains("play") -> "Gaming"
            lower.contains("chat") || lower.contains("msg") || lower.contains("social") -> "Social"
            lower.contains("video") || lower.contains("movie") || lower.contains("tv") -> "Video"
            lower.contains("browser") || lower.contains("web") -> "Browser"
            else -> "App"
        }
    }

    /**
     * Provides initial starter limits if database is empty.
     */
    fun getDefaultSeedLimits(): List<AppLimit> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return listOf(
            AppLimit(
                packageName = "com.instagram.android",
                appName = "Instagram",
                dailyLimitMinutes = 30,
                currentUsageMinutes = 24,
                isRestricted = true,
                category = "Social",
                iconKey = "instagram",
                dateString = todayStr
            ),
            AppLimit(
                packageName = "com.zhiliaoapp.musically",
                appName = "TikTok",
                dailyLimitMinutes = 25,
                currentUsageMinutes = 25, // already reached limit for demo
                isRestricted = true,
                category = "Social",
                iconKey = "tiktok",
                dateString = todayStr
            ),
            AppLimit(
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                dailyLimitMinutes = 45,
                currentUsageMinutes = 35,
                isRestricted = true,
                category = "Video",
                iconKey = "youtube",
                dateString = todayStr
            ),
            AppLimit(
                packageName = "com.twitter.android",
                appName = "X (Twitter)",
                dailyLimitMinutes = 20,
                currentUsageMinutes = 12,
                isRestricted = true,
                category = "Social",
                iconKey = "twitter",
                dateString = todayStr
            ),
            AppLimit(
                packageName = "com.facebook.katana",
                appName = "Facebook",
                dailyLimitMinutes = 20,
                currentUsageMinutes = 8,
                isRestricted = true,
                category = "Social",
                iconKey = "facebook",
                dateString = todayStr
            )
        )
    }

    /**
     * Provides sample past 7 days usage logs for immediate rich local chart visualization.
     */
    fun getSamplePastWeekLogs(): List<UsageLog> {
        val list = mutableListOf<UsageLog>()
        val cal = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())

        val apps = listOf(
            "com.instagram.android" to "Instagram",
            "com.zhiliaoapp.musically" to "TikTok",
            "com.google.android.youtube" to "YouTube",
            "com.twitter.android" to "X (Twitter)"
        )

        // Generate past 7 days
        for (i in 6 downTo 0) {
            val dateCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dateStr = dayFormat.format(dateCal.time)
            val dayName = dayNameFormat.format(dateCal.time)
            val timeMultiplier = when (i) {
                0 -> 1.0f // today
                1 -> 1.3f // yesterday
                2 -> 1.1f
                3 -> 0.8f
                4 -> 1.4f
                5 -> 0.9f
                else -> 1.2f
            }

            for ((pkg, name) in apps) {
                val base = when (pkg) {
                    "com.zhiliaoapp.musically" -> 28
                    "com.google.android.youtube" -> 35
                    "com.instagram.android" -> 25
                    else -> 15
                }
                val duration = (base * timeMultiplier).toInt()
                list.add(
                    UsageLog(
                        packageName = pkg,
                        appName = name,
                        dateString = dateStr,
                        minutesSpent = duration,
                        dayOfWeek = dayName,
                        timestamp = dateCal.timeInMillis
                    )
                )
            }
        }
        return list
    }
}

data class AppItemInfo(
    val packageName: String,
    val appName: String,
    val category: String,
    val iconKey: String,
    val isPreset: Boolean,
    val defaultLimitMinutes: Int
)
