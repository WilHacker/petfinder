package com.frontend.petfinder.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.auth.presentation.LoginScreen
import com.frontend.petfinder.auth.presentation.RegisterScreen
import com.frontend.petfinder.chat.presentation.ConversationScreen
import com.frontend.petfinder.geofencing.presentation.ZoneDetailScreen
import com.frontend.petfinder.pets.presentation.EditPetScreen
import com.frontend.petfinder.pets.presentation.MedicalHistoryScreen
import com.frontend.petfinder.pets.presentation.PetDetailScreen
import com.frontend.petfinder.pets.presentation.PetPublicCardScreen
import com.frontend.petfinder.pets.presentation.RegisterPetScreen
import com.frontend.petfinder.profile.presentation.ProfileScreen

@Composable
fun PetFinderNavGraph(navController: NavHostController) {
    val isSessionValid by PetFinderApp.sessionManager
        .isSessionValid()
        .collectAsStateWithLifecycle(initialValue = null)

    when (isSessionValid) {
        null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        else -> {
            val startDestination = if (isSessionValid == true) {
                NavRoutes.Main.route
            } else {
                NavRoutes.Login.route
            }

            // Maneja cambios de sesión en caliente (Google Sign-In callback mientras Login está activo)
            LaunchedEffect(isSessionValid) {
                if (isSessionValid == true &&
                    navController.currentDestination?.route == NavRoutes.Login.route) {
                    navController.navigate(NavRoutes.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(NavRoutes.Login.route) {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(NavRoutes.Main.route) {
                                popUpTo(NavRoutes.Login.route) { inclusive = true }
                            }
                        },
                        onNavigateToRegister = { navController.navigate(NavRoutes.Register.route) }
                    )
                }

                composable(NavRoutes.Register.route) {
                    RegisterScreen(
                        onNavigateNext = {
                            navController.navigate(NavRoutes.Main.route) {
                                popUpTo(NavRoutes.Login.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(NavRoutes.Main.route) {
                    LaunchedEffect(isSessionValid) {
                        if (isSessionValid == false) {
                            navController.navigate(NavRoutes.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                    MainScreen(rootNavController = navController)
                }

                composable(NavRoutes.RegisterPet.route) {
                    RegisterPetScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.Profile.route) {
                    ProfileScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = NavRoutes.PetDetail.route,
                    arguments = listOf(
                        navArgument(NavRoutes.PetDetail.ARG_PET_ID) {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val mascotaId = backStackEntry.arguments
                        ?.getString(NavRoutes.PetDetail.ARG_PET_ID) ?: ""
                    PetDetailScreen(
                        mascotaId = mascotaId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToMedical = {
                            navController.navigate(NavRoutes.MedicalHistory.createRoute(mascotaId))
                        },
                        onNavigateToEdit = {
                            navController.navigate(NavRoutes.EditPet.createRoute(mascotaId))
                        },
                        onViewOnMap = { petId ->
                            navController.getBackStackEntry(NavRoutes.Main.route)
                                .savedStateHandle["focusMascotaId"] = petId
                            navController.popBackStack(NavRoutes.Main.route, inclusive = false)
                        },
                        onOpenConversation = { conversacionId ->
                            navController.navigate(NavRoutes.Conversation.createRoute(conversacionId))
                        }
                    )
                }

                composable(
                    route = NavRoutes.EditPet.route,
                    arguments = listOf(
                        navArgument(NavRoutes.EditPet.ARG_PET_ID) { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val mascotaId = backStackEntry.arguments?.getString(NavRoutes.EditPet.ARG_PET_ID) ?: ""
                    EditPetScreen(
                        mascotaId = mascotaId,
                        onNavigateBack = { navController.popBackStack() },
                        onDeleted = {
                            navController.popBackStack(NavRoutes.Main.route, inclusive = false)
                        }
                    )
                }

                composable(
                    route = NavRoutes.MedicalHistory.route,
                    arguments = listOf(
                        navArgument(NavRoutes.MedicalHistory.ARG_PET_ID) {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val mascotaId = backStackEntry.arguments
                        ?.getString(NavRoutes.MedicalHistory.ARG_PET_ID) ?: ""
                    MedicalHistoryScreen(
                        mascotaId = mascotaId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = NavRoutes.ZoneDetail.route,
                    arguments = listOf(
                        navArgument(NavRoutes.ZoneDetail.ARG_ZONE_ID) {
                            type = NavType.IntType
                        }
                    )
                ) { backStackEntry ->
                    val zonaId = backStackEntry.arguments
                        ?.getInt(NavRoutes.ZoneDetail.ARG_ZONE_ID) ?: 0
                    ZoneDetailScreen(
                        zonaId = zonaId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Conversación privada 1:1 dueño ↔ rescatista
                composable(
                    route = NavRoutes.Conversation.route,
                    arguments = listOf(
                        navArgument(NavRoutes.Conversation.ARG_CONVERSACION_ID) {
                            type = NavType.StringType
                        }
                    )
                ) { backStackEntry ->
                    val conversacionId = backStackEntry.arguments
                        ?.getString(NavRoutes.Conversation.ARG_CONVERSACION_ID) ?: ""
                    ConversationScreen(
                        conversacionId = conversacionId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Ficha pública del QR — accesible sin sesión, también vía deep link
                composable(
                    route = NavRoutes.QrPublicCard.route,
                    arguments = listOf(
                        navArgument(NavRoutes.QrPublicCard.ARG_TOKEN) {
                            type = NavType.StringType
                        }
                    ),
                    deepLinks = listOf(
                        navDeepLink {
                            uriPattern = "https://backend-petfinder.onrender.com/qr/{token}"
                        },
                        navDeepLink {
                            uriPattern = "https://pet-qr-web.vercel.app/scan/{token}"
                        },
                        navDeepLink {
                            uriPattern = "petfinder://pet/{token}"
                        }
                    )
                ) { backStackEntry ->
                    val token = backStackEntry.arguments
                        ?.getString(NavRoutes.QrPublicCard.ARG_TOKEN) ?: ""
                    PetPublicCardScreen(
                        token = token,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
