package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_logs")
data class UsageLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val dateString: String,
    val minutesSpent: Int,
    val dayOfWeek: String,
    val timestamp: Long = System.currentTimeMillis()
)
