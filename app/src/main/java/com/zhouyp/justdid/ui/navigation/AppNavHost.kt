package com.zhouyp.justdid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zhouyp.justdid.ui.home.HomeScreen
import com.zhouyp.justdid.ui.settings.SettingsScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Home.route
    ) {
        composable(Route.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Route.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}
