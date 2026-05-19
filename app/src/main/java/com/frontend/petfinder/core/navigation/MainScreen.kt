package com.frontend.petfinder.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.theme.TextGray
import com.frontend.petfinder.geofencing.presentation.MapHomeScreen
import com.frontend.petfinder.geofencing.presentation.MapViewModel
import com.frontend.petfinder.geofencing.presentation.MyZonesScreen
import com.frontend.petfinder.pets.presentation.MyPetsScreen

@Composable
fun MainScreen(rootNavController: NavHostController) {
    val bottomNavController = rememberNavController()

    // CEREBRO COMPARTIDO: Ambas pestañas verán exactamente los mismos datos
    val sharedMapViewModel: MapViewModel = viewModel()

    val rol by PetFinderApp.sessionManager.getUserRole().collectAsState(initial = null)
    val isAdmin = rol == "admin"

    Scaffold(
        bottomBar = {
            val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Nuestra Barra Customizada
            CustomBottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    bottomNavController.navigate(route) {
                        popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            NavHost(
                navController = bottomNavController,
                startDestination = NavRoutes.MapHome.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(NavRoutes.MapHome.route) {
                    MapHomeScreen(
                        mapViewModel = sharedMapViewModel,
                        onNavigateToProfile = { rootNavController.navigate(NavRoutes.Profile.route) },
                        isAdmin = isAdmin
                    )
                }
                composable(NavRoutes.MyPets.route) {
                    MyPetsScreen(
                        onNavigateToRegisterPet = { rootNavController.navigate(NavRoutes.RegisterPet.route) },
                        onNavigateToPetDetail = { mascotaId ->
                            rootNavController.navigate(NavRoutes.PetDetail.createRoute(mascotaId))
                        }
                    )
                }
                composable(NavRoutes.MyZones.route) {
                    MyZonesScreen(
                        viewModel = sharedMapViewModel,
                        onNavigateToMap = {
                            bottomNavController.navigate(NavRoutes.MapHome.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToZoneDetail = { zonaId ->
                            rootNavController.navigate(NavRoutes.ZoneDetail.createRoute(zonaId))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    // ¡AQUÍ ESTÁ LA SOLUCIÓN AL BUG!
    // Envolvemos la barra en un Box que respeta los botones del celular
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding() // <-- Empuja todo hacia arriba del menú del sistema
    ) {
        // Contenedor principal de nuestra barra flotante
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp) // Altura total para dejar espacio al botón que sobresale
        ) {
            // Fondo blanco curvo de la barra
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp) // La barra blanca es más baja que el contenedor
                    .align(Alignment.BottomCenter)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    ),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item Izquierdo (Mapa / Home)
                    BottomNavIcon(
                        icon = Icons.Default.Home,
                        isSelected = currentRoute == NavRoutes.MapHome.route,
                        onClick = { onNavigate(NavRoutes.MapHome.route) }
                    )

                    // Espacio vacío en el medio para abrazar al botón flotante
                    Spacer(modifier = Modifier.width(60.dp))

                    // Item Derecho (Zonas Seguras)
                    BottomNavIcon(
                        icon = Icons.Default.ShareLocation,
                        isSelected = currentRoute == NavRoutes.MyZones.route,
                        onClick = { onNavigate(NavRoutes.MyZones.route) }
                    )
                }
            }

            // Botón Naranja Central Flotante (Mis Mascotas)
            FloatingActionButton(
                onClick = { onNavigate(NavRoutes.MyPets.route) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 8.dp) // Lo bajamos ligeramente para que "corte" el borde
                    .size(64.dp),
                shape = CircleShape,
                containerColor = PrimaryOrange,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = "Mis Mascotas",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun BottomNavIcon(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tintColor = if (isSelected) PrimaryOrange else TextGray
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(30.dp)
        )
    }
}