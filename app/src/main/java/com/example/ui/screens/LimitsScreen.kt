package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppUsageInfo
import com.example.ui.viewmodel.ScreenTimeUiState
import com.example.ui.viewmodel.ScreenTimeViewModel

@Composable
fun LimitsScreen(
    uiState: ScreenTimeUiState,
    viewModel: ScreenTimeViewModel,
    modifier: Modifier = Modifier
) {
    var selectedAppForLimit by remember { mutableStateOf<AppUsageInfo?>(null) }
    var continuousReminderEnabled by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Column {
                Text(
                    text = "App Limits & Goals",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Build healthy habits with daily screen time targets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Daily Screen Time Goal Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daily Screen Time Target",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val hours = uiState.dailyGoalMinutes / 60
                        val mins = uiState.dailyGoalMinutes % 60
                        val targetText = if (mins > 0) "${hours}h ${mins}m" else "${hours}h"

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = targetText,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Goal Slider (1h to 8h in steps of 30 mins)
                    Slider(
                        value = uiState.dailyGoalMinutes.toFloat(),
                        onValueChange = { viewModel.setDailyGoal(it.toInt()) },
                        valueRange = 60f..480f,
                        steps = 13,
                        modifier = Modifier.testTag("daily_goal_slider")
                    )

                    // Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(120 to "2h", 180 to "3h", 240 to "4h", 300 to "5h", 360 to "6h").forEach { (minutes, label) ->
                            val isSelected = uiState.dailyGoalMinutes == minutes
                            SuggestionChip(
                                onClick = { viewModel.setDailyGoal(minutes) },
                                label = { Text(label) },
                                shape = RoundedCornerShape(8.dp),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        }

        // Mindful Break Reminder Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Take a Break Reminder",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Gentle nudge after 45 minutes of continuous screen use",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = continuousReminderEnabled,
                        onCheckedChange = { continuousReminderEnabled = it },
                        modifier = Modifier.testTag("break_reminder_switch")
                    )
                }
            }
        }

        // App-Specific Limits Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App Timers & Limits",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap an app to adjust",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // List top apps with limit status
        val topApps = uiState.todayApps.take(8)
        items(topApps, key = { it.packageName }) { app ->
            val limitMinutes = uiState.appLimits[app.packageName]
            val usageMinutes = (app.usageTimeMillis / (60 * 1000)).toInt()

            Surface(
                onClick = { selectedAppForLimit = app },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(app.category.color.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = app.appName.firstOrNull()?.uppercase() ?: "A",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = app.category.color
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${app.formattedDuration} used today",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (limitMinutes != null) {
                        val isOver = usageMinutes >= limitMinutes
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isOver) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (isOver) "Limit reached (${limitMinutes}m)" else "${limitMinutes}m limit",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isOver) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { selectedAppForLimit = app },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Set Limit", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Dialog to configure limit for an app
    if (selectedAppForLimit != null) {
        val app = selectedAppForLimit!!
        val currentLimit = uiState.appLimits[app.packageName] ?: 30

        AlertDialog(
            onDismissRequest = { selectedAppForLimit = null },
            title = {
                Text("Set Daily Limit for ${app.appName}")
            },
            text = {
                Column {
                    Text(
                        text = "Choose maximum allowed daily usage:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val options = listOf(15, 30, 45, 60, 90, 120)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.chunked(3).forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowOptions.forEach { opt ->
                                    val isSelected = currentLimit == opt
                                    Button(
                                        onClick = {
                                            viewModel.setAppLimit(app.packageName, opt)
                                            selectedAppForLimit = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Text(if (opt >= 60) "${opt/60}h" + (if (opt%60 > 0) " ${opt%60}m" else "") else "${opt}m")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setAppLimit(app.packageName, 0) // removes limit
                        selectedAppForLimit = null
                    }
                ) {
                    Text("Remove Limit", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForLimit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
