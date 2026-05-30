package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.service.FirewallVpnService
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PermissionUtils
import com.example.worker.ScheduleWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Kickstart Periodic WorkManager scans for schedules as a backup anchor
        try {
            val workRequest = PeriodicWorkRequestBuilder<ScheduleWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "FreezeXScheduleWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize custom WorkManager: ${e.message}")
        }

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                MainNavigationContainer(viewModel = viewModel)
            }
        }
    }
}

sealed class NavigationTab(val route: String, val label: String, val icon: ImageVector) {
    object Console : NavigationTab("dashboard", "Console", Icons.Default.Dashboard)
    object Apps : NavigationTab("app_selection", "Apps", Icons.Default.AppBlocking)
    object Firewall : NavigationTab("firewall", "Firewall", Icons.Default.Security)
    object Schedules : NavigationTab("schedules", "Schedules", Icons.Default.Schedule)
    object Metrics : NavigationTab("stats", "Metrics", Icons.Default.Analytics)
    object Config : NavigationTab("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MainNavigationContainer(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "splash"
    
    val context = LocalContext.current

    // Bottom tab lists
    val bottomTabs = listOf(
        NavigationTab.Console,
        NavigationTab.Apps,
        NavigationTab.Firewall,
        NavigationTab.Schedules,
        NavigationTab.Metrics,
        NavigationTab.Config
    )

    // Check if the current screen requires a structural navigation bar wrapper
    val isWorkspaceRoute = currentRoute in bottomTabs.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isWorkspaceRoute) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_navigation_bar"),
                    tonalElevation = 8.dp
                ) {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Splash Screen channel
            composable("splash") {
                SplashScreen(onNavigateDone = { next ->
                    navController.navigate(next) {
                        popUpTo("splash") { inclusive = true }
                    }
                })
            }

            // 2. Onboarding Screen channel
            composable("onboarding") {
                OnboardingScreen(onNavigateDone = {
                    navController.navigate("permissions") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                })
            }

            // 3. Permissions Checklist Screen channel
            composable("permissions") {
                PermissionsScreen(onNavigateDone = {
                    navController.navigate("dashboard") {
                        popUpTo("permissions") { inclusive = true }
                    }
                })
            }

            // 4. Console Dashboard screen
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToApps = { navController.navigate("app_selection") },
                    onNavigateToFirewall = { navController.navigate("firewall") }
                )
            }

            // 5. App restrictions screen
            composable("app_selection") {
                AppSelectionScreen(viewModel = viewModel)
            }

            // 6. Firewall rules configurator screen
            composable("firewall") {
                FirewallScreen(viewModel = viewModel)
            }

            // 7. Timer Schedules setup screen
            composable("schedules") {
                SchedulesScreen(viewModel = viewModel)
            }

            // 8. Statistics visual tracker
            composable("stats") {
                StatsScreen(viewModel = viewModel)
            }

            // 9. Overall Settings configurator
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToPermissions = { navController.navigate("permissions") }
                )
            }
        }
    }
}
