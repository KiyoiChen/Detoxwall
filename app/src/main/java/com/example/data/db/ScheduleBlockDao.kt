package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScheduleBlock
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleBlockDao {
    @Query("SELECT * FROM schedule_blocks ORDER BY startHour ASC, startMinute ASC")
    fun getAllSchedules(): Flow<List<ScheduleBlock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: ScheduleBlock): Long

    @Update
    suspend fun update(schedule: ScheduleBlock)

    @Delete
    suspend fun delete(schedule: ScheduleBlock)

    @Query("UPDATE schedule_blocks SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setEnabled(id: Long, isEnabled: Boolean)

    @Query("SELECT * FROM schedule_blocks WHERE id = :id")
    suspend fun getById(id: Long): ScheduleBlock?
}
