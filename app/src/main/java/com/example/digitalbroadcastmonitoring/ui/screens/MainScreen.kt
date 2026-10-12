package com.example.digitalbroadcastmonitoring.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.digitalbroadcastmonitoring.ui.navigation.Screen
import com.example.digitalbroadcastmonitoring.ui.navigation.bottomNavItems
import com.example.digitalbroadcastmonitoring.ui.theme.*

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar ONLY on primary destinations
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Programs.route,
        Screen.Schedule.route,
        Screen.Reports.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextWhite,
                    tonalElevation = 8.dp,
                    modifier = Modifier.border(
                        BorderStroke(1.dp, com.example.digitalbroadcastmonitoring.ui.theme.BorderStroke)
                    )
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TextWhite,
                                selectedTextColor = TextWhite,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = PrimaryBlue
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Primary Destinations
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToSchedule = { navController.navigate(Screen.Schedule.route) },
                    onNavigateToAddSchedule = { navController.navigate(Screen.AddSchedule.route) },
                    onNavigateToAddProgram = { navController.navigate(Screen.AddProgram.route) },
                    onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToScheduleDetail = { id -> navController.navigate(Screen.ScheduleDetail.createRoute(id)) }
                )
            }

            composable(Screen.Programs.route) {
                ProgramsScreen(
                    onNavigateToAddProgram = { navController.navigate(Screen.AddProgram.route) },
                    onNavigateToProgramDetail = { id -> navController.navigate(Screen.ProgramDetail.createRoute(id)) },
                    onNavigateToAnalysis = { id -> navController.navigate(Screen.Analysis.createRoute(id)) }
                )
            }

            composable(Screen.Schedule.route) {
                ScheduleScreen(
                    onNavigateToAddSchedule = { navController.navigate(Screen.AddSchedule.route) },
                    onNavigateToScheduleDetail = { id -> navController.navigate(Screen.ScheduleDetail.createRoute(id)) }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(
                    onNavigateToAnalysis = { navController.navigate(Screen.Analysis.createRoute("default")) }
                )
            }

            // Authentication Destinations
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Programs Nested Routes
            composable(Screen.AddProgram.route) {
                AddProgramScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ProgramDetail.route,
                arguments = listOf(navArgument("programId") { type = NavType.StringType })
            ) { backStackEntry ->
                val programId = backStackEntry.arguments?.getString("programId") ?: ""
                ProgramDetailScreen(
                    programId = programId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate(Screen.EditProgram.createRoute(id)) },
                    onNavigateToAnalysis = { id -> navController.navigate(Screen.Analysis.createRoute(id)) }
                )
            }

            composable(
                route = Screen.EditProgram.route,
                arguments = listOf(navArgument("programId") { type = NavType.StringType })
            ) { backStackEntry ->
                val programId = backStackEntry.arguments?.getString("programId") ?: ""
                EditProgramScreen(
                    programId = programId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Schedules Nested Routes
            composable(Screen.AddSchedule.route) {
                AddScheduleScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ScheduleDetail.route,
                arguments = listOf(navArgument("scheduleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val scheduleId = backStackEntry.arguments?.getString("scheduleId") ?: ""
                ScheduleDetailScreen(
                    scheduleId = scheduleId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate(Screen.EditSchedule.createRoute(id)) }
                )
            }

            composable(
                route = Screen.EditSchedule.route,
                arguments = listOf(navArgument("scheduleId") { type = NavType.StringType })
            ) { backStackEntry ->
                val scheduleId = backStackEntry.arguments?.getString("scheduleId") ?: ""
                EditScheduleScreen(
                    scheduleId = scheduleId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Analysis Nested Route
            composable(
                route = Screen.Analysis.route,
                arguments = listOf(navArgument("programId") { type = NavType.StringType })
            ) { backStackEntry ->
                val programId = backStackEntry.arguments?.getString("programId")
                BroadcastAnalysisScreen(
                    programId = programId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // User & Settings Nested Routes
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
