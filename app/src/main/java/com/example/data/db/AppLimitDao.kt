package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppLimit
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limits ORDER BY appName ASC")
    fun getAllAppLimits(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits WHERE isRestricted = 1")
    fun getRestrictedLimits(): Flow<List<AppLimit>>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName LIMIT 1")
    suspend fun getLimitForPackage(packageName: String): AppLimit?

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName LIMIT 1")
    fun observeLimitForPackage(packageName: String): Flow<AppLimit?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(appLimit: AppLimit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(appLimits: List<AppLimit>)

    @Update
    suspend fun update(appLimit: AppLimit)

    @Query("UPDATE app_limits SET currentUsageMinutes = :usage WHERE packageName = :packageName")
    suspend fun updateUsage(packageName: String, usage: Int)

    @Query("UPDATE app_limits SET currentUsageMinutes = 0")
    suspend fun resetDailyUsage()

    @Query("DELETE FROM app_limits WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
