package com.bih.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bih.app.api.ApiClient
import com.bih.app.api.BihApi
import com.bih.app.data.Settings
import com.bih.app.data.SettingsRepository
import com.bih.app.ui.ChatScreen
import com.bih.app.ui.ChatViewModel
import com.bih.app.ui.NotesScreen
import com.bih.app.ui.SettingsScreen
import com.bih.app.ui.TodayScreen
import com.bih.app.ui.theme.BihTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepo = SettingsRepository(applicationContext)
        setContent {
            BihTheme {
                val settings by settingsRepo.settings
                    .collectAsState(initial = null)
                settings?.let { current ->
                    // Rebuilds the API client whenever settings change.
                    val api = remember(current) {
                        ApiClient.create(current.apiUrl, current.apiKey)
                    }
                    BihNavHost(api = api, settingsRepo = settingsRepo, settings = current)
                }
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("chat", "Chat", Icons.Default.Send),
    Tab("today", "Today", Icons.Default.CheckCircle),
    Tab("notes", "Notes", Icons.Default.Edit),
    Tab("settings", "Settings", Icons.Default.Settings),
)

@Composable
private fun BihNavHost(
    api: BihApi,
    settingsRepo: SettingsRepository,
    settings: Settings,
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // ChatViewModel survives tab switches, rotation, and theme changes.
    val chatViewModel: ChatViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "chat",
            modifier = Modifier.padding(padding),
        ) {
            composable("chat") {
                ChatScreen(
                    api = api,
                    speakReplies = settings.speakReplies,
                    state = chatViewModel,
                    scope = scope,
                )
            }
            composable("today") { TodayScreen(api = api) }
            composable("notes") { NotesScreen(api = api) }
            composable("settings") {
                SettingsScreen(settingsRepo = settingsRepo, current = settings)
            }
        }
    }
}
