package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "child_profiles")
data class ChildProfile(
    @PrimaryKey
    val id: String = "child_default",
    val name: String = "Kid Profile",
    val avatarName: String = "kid_avatar_1",
    val dailyCapMinutes: Int = 60,
    val continuousLimitMinutes: Int = 20,
    val currentContinuousMinutes: Int = 0,
    val bypassAttempts: Int = 0,
    val isChildModeActive: Boolean = false
)
