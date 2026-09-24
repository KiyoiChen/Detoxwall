package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limits ORDER BY appName COLLATE NOCASE")
    fun observeAll(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits")
    suspend fun getAll(): List<AppLimit>

    @Upsert
    suspend fun upsert(limit: AppLimit)

    @Query("UPDATE app_limits SET enabled = :enabled WHERE packageName = :pkg")
    suspend fun setEnabled(pkg: String, enabled: Boolean)

    @Query("DELETE FROM app_limits WHERE packageName = :pkg")
    suspend fun delete(pkg: String)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY startMinute")
    fun observeAll(): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules")
    suspend fun getAll(): List<Schedule>

    @Upsert
    suspend fun upsert(schedule: Schedule): Long

    @Query("UPDATE schedules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Delete
    suspend fun delete(schedule: Schedule)
}

@Dao
interface TamperEventDao {
    @Query("SELECT * FROM tamper_events ORDER BY timestamp DESC LIMIT 50")
    fun observeRecent(): Flow<List<TamperEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: TamperEvent)

    @Query("DELETE FROM tamper_events")
    suspend fun clear()

    @Query("DELETE FROM tamper_events WHERE timestamp < :cutoff")
    suspend fun prune(cutoff: Long)
}

class Converters {
    @TypeConverter fun fromType(type: TamperType): String = type.name
    @TypeConverter fun toType(value: String): TamperType =
        runCatching { TamperType.valueOf(value) }.getOrDefault(TamperType.BYPASS_ATTEMPT)
}

@Database(
    entities = [AppLimit::class, Schedule::class, TamperEvent::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class DetoxDatabase : RoomDatabase() {
    abstract fun appLimits(): AppLimitDao
    abstract fun schedules(): ScheduleDao
    abstract fun tamperEvents(): TamperEventDao

    companion object {
        fun create(context: Context): DetoxDatabase =
            Room.databaseBuilder(context, DetoxDatabase::class.java, "detoxwall_v3.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
