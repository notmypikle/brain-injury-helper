package com.bih.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bih.app.data.ApiClient
import com.bih.app.data.SettingsStore
import com.bih.app.ui.screens.*
import com.bih.app.ui.theme.BihTheme

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("chat", "Chat", Icons.Filled.Email),
    Tab("today", "Today", Icons.Filled.CheckCircle),
    Tab("notes", "Notes", Icons.Filled.Edit),
    Tab("routines", "Routines", Icons.Filled.List),
    Tab("settings", "Settings", Icons.Filled.Settings),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsStore(this)
        val api = ApiClient(settings)
        setContent {
            BihTheme {
                AppNav(api, settings)
            }
        }
    }
}

@Composable
fun AppNav(api: ApiClient, settings: SettingsStore) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "chat", modifier = Modifier.padding(padding)) {
            composable("chat") { ChatScreen(api) }
            composable("today") { TodayScreen(api) }
            composable("notes") { NotesScreen(api) }
            composable("routines") { RoutinesScreen(api) }
            composable("settings") { SettingsScreen(settings) }
        }
    }
}
