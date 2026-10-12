package com.example.digitalbroadcastmonitoring.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Primary Bottom Navigation Destinations
    data object Home : Screen("home")
    data object Programs : Screen("programs")
    data object Schedule : Screen("schedule")
    data object Reports : Screen("reports")

    // Authentication
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")

    // Programs
    data object AddProgram : Screen("add_program")
    data object ProgramDetail : Screen("program_detail/{programId}") {
        fun createRoute(programId: String) = "program_detail/$programId"
    }
    data object EditProgram : Screen("edit_program/{programId}") {
        fun createRoute(programId: String) = "edit_program/$programId"
    }

    // Schedules
    data object AddSchedule : Screen("add_schedule")
    data object ScheduleDetail : Screen("schedule_detail/{scheduleId}") {
        fun createRoute(scheduleId: String) = "schedule_detail/$scheduleId"
    }
    data object EditSchedule : Screen("edit_schedule/{scheduleId}") {
        fun createRoute(scheduleId: String) = "edit_schedule/$scheduleId"
    }

    // Analysis
    data object Analysis : Screen("analysis/{programId}") {
        fun createRoute(programId: String = "default") = "analysis/$programId"
    }

    // Settings & User
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
    data object About : Screen("about")
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home),
    BottomNavItem(Screen.Programs.route, "Programs", Icons.AutoMirrored.Filled.List),
    BottomNavItem(Screen.Schedule.route, "Schedule", Icons.Filled.DateRange),
    BottomNavItem(Screen.Reports.route, "Reports", Icons.Filled.Insights)
)
