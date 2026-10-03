package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UsageStatsHelper
import com.example.model.AppUsageInfo
import com.example.ui.components.AppUsageItem
import com.example.ui.components.CircularScreenTimeGauge
import com.example.ui.components.HourlyBarChart
import com.example.ui.viewmodel.ScreenTimeUiState
import com.example.ui.viewmodel.ScreenTimeViewModel

@Composable
fun TodayScreen(
    uiState: ScreenTimeUiState,
    viewModel: ScreenTimeViewModel,
    onNavigateToAnalytics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dailyGoalMillis = uiState.dailyGoalMinutes * 60 * 1000L

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Permission Banner (if not granted)
        if (!uiState.hasPermission) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Usage Access Required",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "To accurately measure screen time and app usage on your phone, Android requires Usage Access permission.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                context.startActivity(UsageStatsHelper.getUsageAccessIntent())
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("grant_permission_button")
                        ) {
                            Text("Grant Usage Access")
                        }
                    }
                }
            }
        }

        // Demo Mode Indicator Badge
        if (uiState.isDemoMode) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Preview Demo Data Active",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        TextButton(
                            onClick = { viewModel.loadData(context) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Refresh Real Data", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Hero Screen Time Circular Gauge
        item {
            CircularScreenTimeGauge(
                totalScreenTimeMillis = uiState.totalScreenTimeMillis,
                dailyGoalMillis = dailyGoalMillis,
                pickupsCount = uiState.todayPickups
            )
        }

        // 24-Hour Activity Distribution
        item {
            HourlyBarChart(hourlyList = uiState.hourlyUsage)
        }

        // Top Apps Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Most Used Today",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onNavigateToAnalytics,
                    modifier = Modifier.testTag("view_all_apps_button")
                ) {
                    Text("View All")
                }
            }
        }

        // Top 5 Apps List
        val topApps = uiState.todayApps.take(5)
        val maxUsage = topApps.firstOrNull()?.usageTimeMillis ?: 1L

        items(topApps, key = { it.packageName }) { app ->
            AppUsageItem(
                app = app,
                maxUsageMillis = maxUsage
            )
        }

        if (topApps.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No app usage recorded yet today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
