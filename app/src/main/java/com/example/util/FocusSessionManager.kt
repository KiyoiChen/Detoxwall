package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FocusSessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("detoxwall_focus_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_END_TIME = "focus_end_time"
        private const val KEY_DURATION_MINUTES = "focus_duration_minutes"
        private const val KEY_FOCUS_TAG = "focus_tag"
    }

    private val _isFocusActive = MutableStateFlow(isSessionActive())
    val isFocusActive: StateFlow<Boolean> = _isFocusActive.asStateFlow()

    fun startSession(durationMinutes: Int, tag: String = "Deep Work") {
        val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        prefs.edit()
            .putLong(KEY_END_TIME, endTime)
            .putInt(KEY_DURATION_MINUTES, durationMinutes)
            .putString(KEY_FOCUS_TAG, tag)
            .apply()
        _isFocusActive.value = true
    }

    fun stopSession() {
        prefs.edit()
            .putLong(KEY_END_TIME, 0L)
            .apply()
        _isFocusActive.value = false
    }

    fun isSessionActive(): Boolean {
        val endTime = prefs.getLong(KEY_END_TIME, 0L)
        val active = endTime > System.currentTimeMillis()
        if (!active && endTime != 0L) {
            prefs.edit().putLong(KEY_END_TIME, 0L).apply()
        }
        return active
    }

    fun getRemainingSeconds(): Long {
        val endTime = prefs.getLong(KEY_END_TIME, 0L)
        val remaining = (endTime - System.currentTimeMillis()) / 1000L
        return if (remaining > 0) remaining else 0L
    }

    fun getTargetDurationMinutes(): Int {
        return prefs.getInt(KEY_DURATION_MINUTES, 120)
    }

    fun getFocusTag(): String {
        return prefs.getString(KEY_FOCUS_TAG, "Deep Work") ?: "Deep Work"
    }
}
