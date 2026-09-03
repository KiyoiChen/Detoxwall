package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChildProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildProfileDao {
    @Query("SELECT * FROM child_profiles")
    fun getAllProfiles(): Flow<List<ChildProfile>>

    @Query("SELECT * FROM child_profiles WHERE isChildModeActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<ChildProfile?>

    @Query("SELECT * FROM child_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): ChildProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: ChildProfile)

    @Update
    suspend fun update(profile: ChildProfile)

    @Query("UPDATE child_profiles SET bypassAttempts = bypassAttempts + 1 WHERE id = :id")
    suspend fun incrementBypassAttempt(id: String)

    @Query("UPDATE child_profiles SET bypassAttempts = 0 WHERE id = :id")
    suspend fun resetBypassAttempts(id: String)

    @Query("UPDATE child_profiles SET isChildModeActive = :isActive WHERE id = :id")
    suspend fun setChildModeActive(id: String, isActive: Boolean)

    @Query("UPDATE child_profiles SET isChildModeActive = 0")
    suspend fun deactivateAllProfiles()

    @Query("UPDATE child_profiles SET currentContinuousMinutes = :minutes WHERE id = :id")
    suspend fun updateContinuousMinutes(id: String, minutes: Int)
}
