package com.gas_price_finder.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Map : Screen("map")
    object List : Screen("list")
    object Detail : Screen("detail/{stationId}") {
        fun createRoute(stationId: String) = "detail/$stationId"
    }

    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object SettingsUser : Screen("settings_user")
    object SettingsFuel : Screen("settings_fuel")
    object PriceHistory : Screen("priceHistory/{stationId}") {
        fun createRoute(stationId: String) = "priceHistory/$stationId"
    }
}