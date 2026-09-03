package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UsageLog
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageLogDao {
    @Query("SELECT * FROM usage_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<UsageLog>>

    @Query("SELECT * FROM usage_logs WHERE dateString = :dateString ORDER BY minutesSpent DESC")
    fun getLogsForDate(dateString: String): Flow<List<UsageLog>>

    @Query("SELECT SUM(minutesSpent) FROM usage_logs WHERE dateString = :dateString")
    fun getTotalMinutesForDate(dateString: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: UsageLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<UsageLog>)

    @Query("DELETE FROM usage_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun pruneOldLogs(cutoffTimestamp: Long)
}
