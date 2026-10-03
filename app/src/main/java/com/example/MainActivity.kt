package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.LimitsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ScreenTimeViewModel

enum class NavigationTab(val title: String, val icon: @Composable () -> Unit, val testTag: String) {
    TODAY(
        title = "Today",
        icon = { Icon(Icons.Rounded.Dashboard, contentDescription = "Today") },
        testTag = "tab_today"
    ),
    ANALYTICS(
        title = "Analytics",
        icon = { Icon(Icons.Rounded.ShowChart, contentDescription = "Analytics") },
        testTag = "tab_analytics"
    ),
    LIMITS(
        title = "Limits",
        icon = { Icon(Icons.Rounded.HourglassBottom, contentDescription = "Limits") },
        testTag = "tab_limits"
    ),
    SETTINGS(
        title = "Settings",
        icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
        testTag = "tab_settings"
    )
}

class MainActivity : ComponentActivity() {

    private val viewModel: ScreenTimeViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var currentTab by remember { mutableStateOf(NavigationTab.TODAY) }

                // Automatically reload whenever app enters foreground (e.g. returning from Settings)
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.loadData(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.PieChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.loadData(context) },
                                    modifier = Modifier.testTag("refresh_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Refresh Data"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag("bottom_nav_bar"),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationTab.values().forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = tab.icon,
                                    label = { Text(tab.title) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tab_transition"
                        ) { tab ->
                            when (tab) {
                                NavigationTab.TODAY -> TodayScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onNavigateToAnalytics = { currentTab = NavigationTab.ANALYTICS }
                                )
                                NavigationTab.ANALYTICS -> AnalyticsScreen(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                                NavigationTab.LIMITS -> LimitsScreen(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                                NavigationTab.SETTINGS -> SettingsScreen(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
