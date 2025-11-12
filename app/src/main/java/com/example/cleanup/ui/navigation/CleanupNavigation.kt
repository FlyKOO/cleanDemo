package com.example.cleanup.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cleanup.ui.screen.AboutScreen
import com.example.cleanup.ui.screen.AppManagementScreen
import com.example.cleanup.ui.screen.AutoCleanupSettingsScreen
import com.example.cleanup.ui.screen.CleanupHistoryScreen
import com.example.cleanup.ui.screen.DashboardScreen
import com.example.cleanup.ui.screen.LanguageSelectionScreen
import com.example.cleanup.ui.screen.LargeFileScanScreen
import com.example.cleanup.ui.screen.PermissionScreen
import com.example.cleanup.ui.screen.ScheduledCleanupScreen
import com.example.cleanup.ui.screen.SettingsScreen
import com.example.cleanup.work.EVENT_18
import com.example.cleanup.work.EVENT_19
import com.example.cleanup.work.EVENT_21
import com.example.cleanup.work.EVENT_24
import com.example.cleanup.work.EVENT_4
import com.example.cleanup.work.EVENT_5
import com.example.cleanup.work.EVENT_6
import com.example.cleanup.work.EVENT_8

@Composable
fun CleanupNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                    EVENT_8()
                },
                onNavigateToAppManagement = {
                    navController.navigate("app_management")
                    EVENT_4()
                },
                onNavigateToLargeFileScan = {
                    navController.navigate("large_file_scan")
                    EVENT_5()
                },
                onNavigateToCleanupHistory = {
                    navController.navigate("cleanup_history")
                    EVENT_6()
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToScheduledCleanup = {
                    EVENT_19()
                    navController.navigate("scheduled_cleanup")
                },
                onNavigateToPermissions = {
                    EVENT_21()
                    navController.navigate("permissions")
                },
                onNavigateToLanguageSelection = {
                    navController.navigate("language_selection")
                },
                onNavigateToAbout = {
                    EVENT_24()
                    navController.navigate("about")
                },
                onNavigateToAutoCleanupSettings = {
                    EVENT_18()
                    navController.navigate("auto_cleanup_settings")
                }
            )
        }
        
        composable("app_management") {
            AppManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("large_file_scan") {
            LargeFileScanScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("cleanup_history") {
            CleanupHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("scheduled_cleanup") {
            ScheduledCleanupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("permissions") {
            PermissionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("language_selection") {
            LanguageSelectionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("about") {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("auto_cleanup_settings") {
            AutoCleanupSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
