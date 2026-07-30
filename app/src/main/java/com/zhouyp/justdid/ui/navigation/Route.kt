package com.zhouyp.justdid.ui.navigation

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Settings : Route("settings")
}
