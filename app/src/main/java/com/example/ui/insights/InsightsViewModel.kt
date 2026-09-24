package com.example.ui.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.container
import com.example.usage.UsageCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

data class AppUsageUi(val packageName: String, val label: String, val minutes: Int)

data class DayTotal(val date: LocalDate, val minutes: Int)

data class InsightsUiState(
    val month: YearMonth = YearMonth.now(),
    val days: List<DayTotal> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedApps: List<AppUsageUi> = emptyList(),
    val loading: Boolean = true,
) {
    val monthTotal: Int get() = days.sumOf { it.minutes }
    val activeDays: Int get() = days.count { it.minutes > 0 }
    val dailyAverage: Int get() = if (activeDays == 0) 0 else monthTotal / activeDays
    val selectedTotal: Int get() = selectedApps.sumOf { it.minutes }
    val canGoForward: Boolean get() = month < YearMonth.now()
}

class InsightsViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container
    private val _state = MutableStateFlow(InsightsUiState())
    val state: StateFlow<InsightsUiState> = _state.asStateFlow()

    /** Per-day per-app cache so tapping between days is instant. */
    private val cache = HashMap<LocalDate, Map<String, Long>>()
    private var loadJob: Job? = null

    init { loadMonth(YearMonth.now()) }

    fun previousMonth() = loadMonth(_state.value.month.minusMonths(1))

    fun nextMonth() {
        if (_state.value.canGoForward) loadMonth(_state.value.month.plusMonths(1))
    }

    fun refresh() {
        cache.remove(LocalDate.now())
        loadMonth(_state.value.month, keepSelection = true)
    }

    fun selectDay(date: LocalDate) {
        _state.update { it.copy(selectedDate = date, selectedApps = appsFor(cache[date].orEmpty())) }
    }

    private fun loadMonth(month: YearMonth, keepSelection: Boolean = false) {
        loadJob?.cancel()
        val today = LocalDate.now()
        val selected = when {
            keepSelection -> _state.value.selectedDate
            month == YearMonth.from(today) -> today
            else -> month.atEndOfMonth()
        }
        _state.update { it.copy(month = month, loading = true, selectedDate = selected) }
        loadJob = viewModelScope.launch {
            val days = withContext(Dispatchers.IO) {
                (1..month.lengthOfMonth()).map { month.atDay(it) }
                    .filter { !it.isAfter(today) }
                    .map { date ->
                        val perApp = cache.getOrPut(date) { c.usageReader.forDayBest(date) }
                        DayTotal(date, c.usageReader.countedTotalMinutes(perApp))
                    }
            }
            val apps = withContext(Dispatchers.IO) { appsFor(cache[selected].orEmpty()) }
            _state.update { it.copy(days = days, selectedApps = apps, loading = false) }
        }
    }

    private fun appsFor(perApp: Map<String, Long>): List<AppUsageUi> {
        val hidden = c.installedApps.launcherPackages() + getApplication<Application>().packageName
        return perApp.filterKeys { it !in hidden }
            .map { AppUsageUi(it.key, c.installedApps.label(it.key), UsageCalculator.toMinutes(it.value)) }
            .filter { it.minutes > 0 }
            .sortedByDescending { it.minutes }
    }
}
