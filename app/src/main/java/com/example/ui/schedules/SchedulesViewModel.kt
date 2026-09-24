package com.example.ui.schedules

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.container
import com.example.data.Schedule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class ScheduleUi(val schedule: Schedule, val activeNow: Boolean)

class SchedulesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.container.repository
    private val minuteTick = MutableStateFlow(LocalDateTime.now())

    val schedules: StateFlow<List<ScheduleUi>> = combine(repo.schedules, minuteTick) { list, now ->
        val day = now.dayOfWeek.value - 1 // Monday = 0
        val minute = now.hour * 60 + now.minute
        list.map { ScheduleUi(it, it.isActiveAt(day, minute)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                minuteTick.value = LocalDateTime.now()
            }
        }
    }

    fun save(schedule: Schedule) = viewModelScope.launch { repo.saveSchedule(schedule) }
    fun setEnabled(schedule: Schedule, enabled: Boolean) = viewModelScope.launch { repo.setScheduleEnabled(schedule.id, enabled) }
    fun delete(schedule: Schedule) = viewModelScope.launch { repo.deleteSchedule(schedule) }

    /** One-tap starting points for new users. */
    fun addPreset(preset: Schedule) = save(preset.copy(id = 0))

    companion object {
        val PRESETS = listOf(
            Schedule(name = "School hours", startMinute = 8 * 60 + 30, endMinute = 15 * 60, daysMask = Schedule.WEEKDAYS),
            Schedule(name = "Bedtime", startMinute = 21 * 60 + 30, endMinute = 7 * 60, daysMask = Schedule.EVERY_DAY),
            Schedule(name = "Homework", startMinute = 17 * 60, endMinute = 19 * 60, daysMask = Schedule.WEEKDAYS),
        )
    }
}
