package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UsageStatsHelper
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.ScreenTimeUiState
import com.example.ui.viewmodel.ScreenTimeViewModel

@Composable
fun SettingsScreen(
    uiState: ScreenTimeUiState,
    viewModel: ScreenTimeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                    text = "Settings & Diagnostics",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "System permissions, calculation modes, and device guide",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Android Permission Status Card
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
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (uiState.hasPermission) EmeraldSuccess.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.hasPermission) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = if (uiState.hasPermission) EmeraldSuccess else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Usage Access",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.hasPermission) "Granted & Active" else "Permission Required",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.hasPermission) EmeraldSuccess else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Button(
                            onClick = {
                                context.startActivity(UsageStatsHelper.getUsageAccessIntent())
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("open_settings_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.hasPermission) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                contentColor = if (uiState.hasPermission) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(if (uiState.hasPermission) "Settings" else "Grant")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Allows the app to read system foreground events and accurately count time spent in each app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Data Mode & Filtering Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Calculation Preferences",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Exclude System Apps Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exclude System Apps",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hides Launcher, SystemUI, and Settings from your screen time stats",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.excludeSystemApps,
                            onCheckedChange = { viewModel.toggleExcludeSystemApps(context) },
                            modifier = Modifier.testTag("exclude_system_apps_switch")
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Toggle Demo Preview Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Demo Sample Data",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Useful for testing UI charts when emulator has zero real app history",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isDemoMode,
                            onCheckedChange = { viewModel.toggleDemoMode(context) },
                            modifier = Modifier.testTag("demo_mode_switch")
                        )
                    }
                }
            }
        }

        // 3. How to install on your mobile phone card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.InstallMobile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "How to Use on Your Physical Phone",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val steps = listOf(
                        "1. In the AI Studio top bar, tap the Settings/Export menu and select 'Generate APK'.",
                        "2. Download the compiled .apk file and transfer it to your Android phone.",
                        "3. On your phone, tap the file to install (enable 'Install from Unknown Sources' if prompted).",
                        "4. Launch the app and tap 'Grant Usage Access' to allow real-time screen tracking."
                    )

                    steps.forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // 4. Accuracy & Technical Architecture info
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Precision Engine Details",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• API: Android UsageStatsManager & UsageEvents\n" +
                               "• Precision: Activity Resumed/Paused chronological event pairing\n" +
                               "• Sleep Capping: Screen-off events cap running apps so background standby is not miscounted as active screen time\n" +
                               "• Storage: 100% on-device private processing with zero cloud uploads",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
