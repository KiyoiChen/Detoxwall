package com.example.data

import kotlinx.coroutines.flow.Flow

class DetoxRepository(private val db: DetoxDatabase) {
    val limits: Flow<List<AppLimit>> = db.appLimits().observeAll()
    val schedules: Flow<List<Schedule>> = db.schedules().observeAll()
    val tamperEvents: Flow<List<TamperEvent>> = db.tamperEvents().observeRecent()

    suspend fun limitsSnapshot(): List<AppLimit> = db.appLimits().getAll()
    suspend fun schedulesSnapshot(): List<Schedule> = db.schedules().getAll()

    suspend fun saveLimit(limit: AppLimit) = db.appLimits().upsert(
        limit.copy(dailyLimitMinutes = limit.dailyLimitMinutes.coerceIn(AppLimit.MIN_MINUTES, AppLimit.MAX_MINUTES))
    )
    suspend fun setLimitEnabled(pkg: String, enabled: Boolean) = db.appLimits().setEnabled(pkg, enabled)
    suspend fun deleteLimit(pkg: String) = db.appLimits().delete(pkg)

    suspend fun saveSchedule(schedule: Schedule) = db.schedules().upsert(schedule)
    suspend fun setScheduleEnabled(id: Long, enabled: Boolean) = db.schedules().setEnabled(id, enabled)
    suspend fun deleteSchedule(schedule: Schedule) = db.schedules().delete(schedule)

    suspend fun logTamper(type: TamperType, detail: String = "") {
        db.tamperEvents().insert(TamperEvent(type = type, detail = detail))
        db.tamperEvents().prune(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
    }
    suspend fun clearTamperLog() = db.tamperEvents().clear()
}
