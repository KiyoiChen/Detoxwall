package com.example.data.repository

import android.content.Context
import com.example.data.db.DetoxDatabase
import com.example.data.model.AppLimit
import com.example.data.model.ChildProfile
import com.example.data.model.ScheduleBlock
import com.example.data.model.UsageLog
import com.example.util.FocusSessionManager
import com.example.util.SecurityManager
import com.example.util.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DetoxRepository(
    private val context: Context,
    private val database: DetoxDatabase = DetoxDatabase.getDatabase(context),
    val securityManager: SecurityManager = SecurityManager(context),
    val focusSessionManager: FocusSessionManager = FocusSessionManager(context),
    val usageStatsHelper: UsageStatsHelper = UsageStatsHelper(context)
) {
    private val appLimitDao = database.appLimitDao()
    private val usageLogDao = database.usageLogDao()
    private val childProfileDao = database.childProfileDao()
    private val scheduleBlockDao = database.scheduleBlockDao()

    val allLimits: Flow<List<AppLimit>> = appLimitDao.getAllAppLimits()
    val restrictedLimits: Flow<List<AppLimit>> = appLimitDao.getRestrictedLimits()
    val allLogs: Flow<List<UsageLog>> = usageLogDao.getAllLogs()
    val allProfiles: Flow<List<ChildProfile>> = childProfileDao.getAllProfiles()
    val activeChildProfile: Flow<ChildProfile?> = childProfileDao.getActiveProfile()
    val allSchedules: Flow<List<ScheduleBlock>> = scheduleBlockDao.getAllSchedules()

    suspend fun initializeDefaultDataIfNeeded() = withContext(Dispatchers.IO) {
        val existing = allLimits.firstOrNull()
        if (existing.isNullOrEmpty()) {
            val seeds = usageStatsHelper.getDefaultSeedLimits()
            appLimitDao.insertAll(seeds)
        }

        val logs = allLogs.firstOrNull()
        if (logs.isNullOrEmpty()) {
            val sampleLogs = usageStatsHelper.getSamplePastWeekLogs()
            usageLogDao.insertAll(sampleLogs)
        }

        val profiles = allProfiles.firstOrNull()
        if (profiles.isNullOrEmpty()) {
            childProfileDao.insertOrUpdate(
                ChildProfile(
                    id = "child_default",
                    name = "Default Kid",
                    avatarName = "kid_avatar_1",
                    dailyCapMinutes = 60,
                    continuousLimitMinutes = 25,
                    currentContinuousMinutes = 0,
                    bypassAttempts = 0,
                    isChildModeActive = securityManager.isChildModeEnabled()
                )
            )
            childProfileDao.insertOrUpdate(
                ChildProfile(
                    id = "child_teen",
                    name = "Teen Profile",
                    avatarName = "kid_avatar_2",
                    dailyCapMinutes = 90,
                    continuousLimitMinutes = 45,
                    currentContinuousMinutes = 0,
                    bypassAttempts = 0,
                    isChildModeActive = false
                )
            )
        }

        val schedules = allSchedules.firstOrNull()
        if (schedules.isNullOrEmpty()) {
            scheduleBlockDao.insert(
                ScheduleBlock(
                    title = "School Hours Lock",
                    startHour = 8,
                    startMinute = 30,
                    endHour = 15,
                    endMinute = 0,
                    daysSummary = "Mon - Fri",
                    isEnabled = false
                )
            )
            scheduleBlockDao.insert(
                ScheduleBlock(
                    title = "Bedtime Detox",
                    startHour = 21,
                    startMinute = 30,
                    endHour = 7,
                    endMinute = 0,
                    daysSummary = "Every day",
                    isEnabled = true
                )
            )
        }
    }

    suspend fun syncUsageWithSystem() = withContext(Dispatchers.IO) {
        val systemUsage = usageStatsHelper.getTodayUsageMinutes()
        if (systemUsage.isNotEmpty()) {
            for ((pkg, minutes) in systemUsage) {
                val existing = appLimitDao.getLimitForPackage(pkg)
                if (existing != null) {
                    appLimitDao.updateUsage(pkg, minutes)
                }
            }
        }
    }

    suspend fun saveOrUpdateLimit(appLimit: AppLimit) = withContext(Dispatchers.IO) {
        appLimitDao.insertOrUpdate(appLimit)
    }

    suspend fun updateAppLimitMinutes(packageName: String, newLimitMinutes: Int) = withContext(Dispatchers.IO) {
        val existing = appLimitDao.getLimitForPackage(packageName)
        if (existing != null) {
            appLimitDao.insertOrUpdate(existing.copy(dailyLimitMinutes = newLimitMinutes, isRestricted = true))
        }
    }

    suspend fun toggleAppRestriction(packageName: String, isRestricted: Boolean) = withContext(Dispatchers.IO) {
        val existing = appLimitDao.getLimitForPackage(packageName)
        if (existing != null) {
            appLimitDao.insertOrUpdate(existing.copy(isRestricted = isRestricted))
        }
    }

    suspend fun deleteLimit(packageName: String) = withContext(Dispatchers.IO) {
        appLimitDao.delete(packageName)
    }

    suspend fun getLimitForPackage(packageName: String): AppLimit? = withContext(Dispatchers.IO) {
        appLimitDao.getLimitForPackage(packageName)
    }

    suspend fun addUsageMinute(packageName: String, minutesToAdd: Int = 1) = withContext(Dispatchers.IO) {
        val existing = appLimitDao.getLimitForPackage(packageName)
        if (existing != null) {
            val newUsage = existing.currentUsageMinutes + minutesToAdd
            appLimitDao.updateUsage(packageName, newUsage)

            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(Date())
            usageLogDao.insert(
                UsageLog(
                    packageName = packageName,
                    appName = existing.appName,
                    dateString = todayStr,
                    minutesSpent = minutesToAdd,
                    dayOfWeek = dayOfWeek
                )
            )
        }
    }

    // Child Mode operations
    suspend fun setChildMode(profileId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        securityManager.setChildModeEnabled(enabled, profileId)
        childProfileDao.deactivateAllProfiles()
        if (enabled) {
            childProfileDao.setChildModeActive(profileId, true)
        }
    }

    suspend fun registerBypassAttempt(profileId: String) = withContext(Dispatchers.IO) {
        childProfileDao.incrementBypassAttempt(profileId)
    }

    suspend fun resetBypassAttempts(profileId: String) = withContext(Dispatchers.IO) {
        childProfileDao.resetBypassAttempts(profileId)
    }

    suspend fun saveChildProfile(profile: ChildProfile) = withContext(Dispatchers.IO) {
        childProfileDao.insertOrUpdate(profile)
    }

    // Schedule operations
    suspend fun addSchedule(schedule: ScheduleBlock): Long = withContext(Dispatchers.IO) {
        scheduleBlockDao.insert(schedule)
    }

    suspend fun toggleSchedule(id: Long, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        scheduleBlockDao.setEnabled(id, isEnabled)
    }

    suspend fun deleteSchedule(schedule: ScheduleBlock) = withContext(Dispatchers.IO) {
        scheduleBlockDao.delete(schedule)
    }

    fun isScheduleActiveNow(): Boolean {
        // Will check active schedule blocks against current time
        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return false // evaluated reactively or via service
    }
}
