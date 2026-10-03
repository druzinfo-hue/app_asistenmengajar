package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "Beranda",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Jadwal : Screen(
        route = "jadwal",
        title = "Jadwal",
        selectedIcon = Icons.Filled.CalendarToday,
        unselectedIcon = Icons.Outlined.CalendarToday
    )

    data object Kalender : Screen(
        route = "kalender",
        title = "Kalender",
        selectedIcon = Icons.Filled.EventNote,
        unselectedIcon = Icons.Outlined.EventNote
    )

    data object Chat : Screen(
        route = "chat",
        title = "Tanya AI",
        selectedIcon = Icons.Filled.Chat,
        unselectedIcon = Icons.Outlined.Chat
    )

    data object Progress : Screen(
        route = "progress",
        title = "Progress",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )

    data object Materi : Screen(
        route = "materi",
        title = "Bank Materi",
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    )

    data object Generator : Screen(
        route = "generator",
        title = "Generator",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    )

    data object Settings : Screen(
        route = "settings",
        title = "Pengaturan",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    companion object {
        val bottomNavItems = listOf(
            Dashboard,
            Jadwal,
            Chat,
            Materi
        )

        val drawerNavItems = listOf(
            Dashboard,
            Jadwal,
            Kalender,
            Chat,
            Progress,
            Materi,
            Generator
        )
    }
}
