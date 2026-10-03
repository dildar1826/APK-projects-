package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UsageStatsHelper
import com.example.model.AppCategory
import com.example.model.AppUsageInfo
import com.example.model.DailyUsageSummary
import com.example.model.HourlyUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScreenTimeUiState(
    val hasPermission: Boolean = false,
    val isDemoMode: Boolean = false,
    val isLoading: Boolean = false,
    val excludeSystemApps: Boolean = true,
    val selectedDayIndex: Int = 6, // default to Today (last item)
    val dailyGoalMinutes: Int = 240, // 4 hours
    val appLimits: Map<String, Int> = mapOf(
        "com.google.android.youtube" to 60,
        "com.instagram.android" to 45
    ),
    val todayApps: List<AppUsageInfo> = emptyList(),
    val hourlyUsage: List<HourlyUsage> = emptyList(),
    val todayPickups: Int = 0,
    val weeklySummaries: List<DailyUsageSummary> = emptyList(),
    val searchQuery: String = "",
    val firstPickupTime: String = "7:15 AM",
    val lastActiveTime: String = "Just now"
) {
    val totalScreenTimeMillis: Long
        get() = todayApps.sumOf { it.usageTimeMillis }

    val filteredApps: List<AppUsageInfo>
        get() {
            if (searchQuery.isBlank()) return todayApps
            return todayApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
}

class ScreenTimeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenTimeUiState())
    val uiState: StateFlow<ScreenTimeUiState> = _uiState.asStateFlow()

    fun loadData(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val hasPermission = UsageStatsHelper.hasUsageStatsPermission(context)
            val currentState = _uiState.value

            withContext(Dispatchers.IO) {
                if (hasPermission && !currentState.isDemoMode) {
                    val (apps, hourly, pickups) = UsageStatsHelper.getTodayUsageData(
                        context,
                        currentState.excludeSystemApps
                    )
                    val weekly = UsageStatsHelper.getPastWeekSummaries(
                        context,
                        currentState.excludeSystemApps
                    )

                    // If fresh device with no activity events yet, provide preview seamlessly
                    if (apps.isEmpty() && weekly.all { it.totalScreenTimeMillis == 0L }) {
                        val demoToday = UsageStatsHelper.generateDemoTodayData()
                        val demoWeek = UsageStatsHelper.generateDemoWeekSummaries()
                        _uiState.value = _uiState.value.copy(
                            hasPermission = true,
                            isDemoMode = true,
                            isLoading = false,
                            todayApps = demoToday.first,
                            hourlyUsage = demoToday.second,
                            todayPickups = demoToday.third,
                            weeklySummaries = demoWeek
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            hasPermission = true,
                            isDemoMode = false,
                            isLoading = false,
                            todayApps = apps,
                            hourlyUsage = hourly,
                            todayPickups = pickups,
                            weeklySummaries = weekly
                        )
                    }
                } else {
                    // Demo mode data
                    val demoToday = UsageStatsHelper.generateDemoTodayData()
                    val demoWeek = UsageStatsHelper.generateDemoWeekSummaries()
                    _uiState.value = _uiState.value.copy(
                        hasPermission = hasPermission,
                        isDemoMode = true,
                        isLoading = false,
                        todayApps = demoToday.first,
                        hourlyUsage = demoToday.second,
                        todayPickups = demoToday.third,
                        weeklySummaries = demoWeek
                    )
                }
            }
        }
    }

    fun setSelectedDayIndex(index: Int) {
        _uiState.value = _uiState.value.copy(selectedDayIndex = index)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setDailyGoal(minutes: Int) {
        _uiState.value = _uiState.value.copy(dailyGoalMinutes = minutes)
    }

    fun setAppLimit(packageName: String, minutes: Int) {
        val updated = _uiState.value.appLimits.toMutableMap()
        if (minutes > 0) {
            updated[packageName] = minutes
        } else {
            updated.remove(packageName)
        }
        _uiState.value = _uiState.value.copy(appLimits = updated)
    }

    fun toggleDemoMode(context: Context) {
        val newDemo = !_uiState.value.isDemoMode
        _uiState.value = _uiState.value.copy(isDemoMode = newDemo)
        loadData(context)
    }

    fun toggleExcludeSystemApps(context: Context) {
        val newExclude = !_uiState.value.excludeSystemApps
        _uiState.value = _uiState.value.copy(excludeSystemApps = newExclude)
        loadData(context)
    }
}
