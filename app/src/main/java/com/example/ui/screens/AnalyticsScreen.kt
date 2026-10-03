package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppCategory
import com.example.model.AppUsageInfo
import com.example.ui.components.AppUsageItem
import com.example.ui.components.CategoryBreakdownBar
import com.example.ui.components.WeeklyLineChart
import com.example.ui.viewmodel.ScreenTimeUiState
import com.example.ui.viewmodel.ScreenTimeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    uiState: ScreenTimeUiState,
    viewModel: ScreenTimeViewModel,
    modifier: Modifier = Modifier
) {
    val dailyGoalMillis = uiState.dailyGoalMinutes * 60 * 1000L
    var selectedCategoryFilter by remember { mutableStateOf<AppCategory?>(null) }

    // Calculate weekly metrics
    val weeklyTotalMillis = remember(uiState.weeklySummaries) {
        uiState.weeklySummaries.sumOf { it.totalScreenTimeMillis }
    }
    val weeklyDailyAverageMillis = remember(uiState.weeklySummaries, weeklyTotalMillis) {
        if (uiState.weeklySummaries.isNotEmpty()) {
            weeklyTotalMillis / uiState.weeklySummaries.size
        } else 0L
    }

    // Filter apps based on search & category chip
    val displayedApps = remember(uiState.filteredApps, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) {
            uiState.filteredApps
        } else {
            uiState.filteredApps.filter { it.category == selectedCategoryFilter }
        }
    }

    val maxAppUsage = displayedApps.firstOrNull()?.usageTimeMillis ?: 1L

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Section Title
        item {
            Column {
                Text(
                    text = "Weekly Trends",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Screen time history over the past 7 days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Summary KPI Cards (Total & Daily Average)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Weekly Time
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "7-Day Total",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = AppUsageInfo.formatDuration(weeklyTotalMillis),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Daily Average
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Daily Average",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = AppUsageInfo.formatDuration(weeklyDailyAverageMillis),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Weekly Line Graph (Cubic Bezier curve with data points and gradient fill)
        item {
            WeeklyLineChart(
                summaries = uiState.weeklySummaries,
                dailyGoalMillis = dailyGoalMillis,
                selectedIndex = uiState.selectedDayIndex,
                onSelectDay = { index -> viewModel.setSelectedDayIndex(index) }
            )
        }

        // Category Breakdown
        item {
            CategoryBreakdownBar(apps = uiState.todayApps)
        }

        // App Leaderboard Header & Search Bar
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Usage Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${displayedApps.size} apps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search TextField
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search apps...") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("All") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    AppCategory.values().filter { it != AppCategory.SYSTEM }.forEach { category ->
                        FilterChip(
                            selected = selectedCategoryFilter == category,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == category) null else category
                            },
                            label = { Text(category.title) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // Ranked Apps List
        items(displayedApps, key = { it.packageName }) { app ->
            AppUsageItem(
                app = app,
                maxUsageMillis = maxAppUsage
            )
        }

        if (displayedApps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No apps matched your filter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
