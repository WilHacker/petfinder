package com.frontend.petfinder.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.auth.presentation.LoginScreen
import com.frontend.petfinder.auth.presentation.RegisterScreen
import com.frontend.petfinder.pets.presentation.RegisterPetScreen

@Composable
fun PetFinderNavGraph(navController: NavHostController) {
    val isSessionValid by PetFinderApp.sessionManager
        .isSessionValid()
        .collectAsState(initial = null)

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
                    // Forced logout: if session becomes invalid while in Main, kick back to Login
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
            }
        }
    }
}
