package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int = 30,
    val currentUsageMinutes: Int = 0,
    val isRestricted: Boolean = true,
    val category: String = "Social",
    val iconKey: String = "social",
    val lastUsedTimestamp: Long = System.currentTimeMillis(),
    val dateString: String = ""
) {
    val isLimitExceeded: Boolean
        get() = isRestricted && dailyLimitMinutes > 0 && currentUsageMinutes >= dailyLimitMinutes

    val remainingMinutes: Int
        get() = if (dailyLimitMinutes > currentUsageMinutes) dailyLimitMinutes - currentUsageMinutes else 0

    val progressPercent: Float
        get() = if (dailyLimitMinutes <= 0) 0f else (currentUsageMinutes.toFloat() / dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
}
