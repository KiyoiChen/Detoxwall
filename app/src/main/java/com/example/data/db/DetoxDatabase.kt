package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AppLimit
import com.example.data.model.ChildProfile
import com.example.data.model.ScheduleBlock
import com.example.data.model.UsageLog

@Database(
    entities = [
        AppLimit::class,
        UsageLog::class,
        ChildProfile::class,
        ScheduleBlock::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DetoxDatabase : RoomDatabase() {
    abstract fun appLimitDao(): AppLimitDao
    abstract fun usageLogDao(): UsageLogDao
    abstract fun childProfileDao(): ChildProfileDao
    abstract fun scheduleBlockDao(): ScheduleBlockDao

    companion object {
        @Volatile
        private var INSTANCE: DetoxDatabase? = null

        fun getDatabase(context: Context): DetoxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DetoxDatabase::class.java,
                    "detoxwall_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
