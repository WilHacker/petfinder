package com.frontend.petfinder.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.frontend.petfinder.geofencing.presentation.MapHomeScreen
import com.frontend.petfinder.geofencing.presentation.MyZonesScreen
import com.frontend.petfinder.pets.presentation.MyPetsScreen

@Composable
fun MainScreen(rootNavController: NavHostController) {
    val bottomNavController = rememberNavController()

    val navItems = listOf(
        BottomNavItem("map_home", "Mapa", Icons.Default.Map),
        BottomNavItem("my_pets", "Mascotas", Icons.Default.Pets),
        BottomNavItem("my_zones", "Zonas", Icons.Default.ShareLocation)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = "map_home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("map_home") {
                MapHomeScreen()
            }
            composable("my_pets") {
                MyPetsScreen(
                    onNavigateToRegisterPet = { rootNavController.navigate(NavRoutes.RegisterPet.route) },
                    onNavigateToPetZones = { /* Ya no lo usamos, se maneja globalmente */ }
                )
            }
            composable("my_zones") {
                MyZonesScreen()
            }
        }
    }
}

data class BottomNavItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)