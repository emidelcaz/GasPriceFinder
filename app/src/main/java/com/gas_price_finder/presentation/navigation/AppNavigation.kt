package com.gas_price_finder.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gas_price_finder.R
import com.gas_price_finder.presentation.screens.detail.DetailScreen
import com.gas_price_finder.presentation.screens.favorites.FavoritesScreen
import com.gas_price_finder.presentation.screens.map.MapScreen
import com.gas_price_finder.presentation.screens.pricehistory.PriceHistoryScreen
import com.gas_price_finder.presentation.screens.settings.SettingsFuelScreen
import com.gas_price_finder.presentation.screens.settings.SettingsScreen
import com.gas_price_finder.presentation.screens.settings.SettingsUserScreen
import com.gas_price_finder.presentation.screens.settings.SettingsViewModel
import com.gas_price_finder.presentation.screens.splash.SplashScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Map.route,
        Screen.Favorites.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(Screen.Map.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = Screen.Splash.route) {
                SplashScreen(
                    onNavigateToMap = {
                        navController.navigate(Screen.Map.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(route = Screen.Map.route) {
                MapScreen(
                    onNavigateToDetail = { stationId ->
                        navController.navigate(Screen.Detail.createRoute(stationId))
                    }
                )
            }

            composable(
                route = Screen.PriceHistory.route,
                arguments = listOf(
                    androidx.navigation.navArgument("stationId") {
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val stationId = backStackEntry.arguments?.getString("stationId") ?: ""
                PriceHistoryScreen(
                    stationId = stationId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(route = Screen.Favorites.route) {
                FavoritesScreen(
                    onNavigateToDetail = { stationId ->
                        navController.navigate(Screen.Detail.createRoute(stationId))
                    }
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("stationId") {
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val stationId = backStackEntry.arguments?.getString("stationId") ?: ""
                DetailScreen(
                    stationId = stationId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPriceHistory = { id ->
                        navController.navigate(Screen.PriceHistory.createRoute(id))
                    }
                )
            }

            // ====== SETTINGS (con ViewModel compartido) ======
            composable(route = Screen.Settings.route) { entry ->
                val viewModel = getSettingsViewModel(entry, navController)
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToUserConfig = {
                        navController.navigate(Screen.SettingsUser.route)
                    }
                )
            }

            composable(route = Screen.SettingsUser.route) { entry ->
                val viewModel = getSettingsViewModel(entry, navController)
                SettingsUserScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToFuel = {
                        navController.navigate(Screen.SettingsFuel.route)
                    }
                )
            }

            composable(route = Screen.SettingsFuel.route) { entry ->
                val viewModel = getSettingsViewModel(entry, navController)
                SettingsFuelScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Obtiene el SettingsViewModel compartido usando el backStackEntry de Settings como owner.
 * Esto asegura que las 3 pantallas (Settings, SettingsUser, SettingsFuel) compartan
 * el mismo ViewModel y su estado.
 */
@Composable
private fun getSettingsViewModel(
    currentEntry: NavBackStackEntry,
    navController: NavHostController
): SettingsViewModel {
    val parentEntry = remember(currentEntry) {
        navController.getBackStackEntry(Screen.Settings.route)
    }
    return hiltViewModel(parentEntry)
}

@Composable
private fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem(stringResource(R.string.gasolineras), Screen.Map.route, Icons.Default.LocalGasStation),
        BottomNavItem(stringResource(R.string.favoritas), Screen.Favorites.route, Icons.Default.Favorite),
        BottomNavItem(stringResource(R.string.ajustes), Screen.Settings.route, Icons.Default.Menu)
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) }
            )
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)