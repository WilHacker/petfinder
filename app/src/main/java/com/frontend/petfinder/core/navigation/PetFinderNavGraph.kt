package com.frontend.petfinder.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.frontend.petfinder.auth.presentation.LoginScreen
import com.frontend.petfinder.auth.presentation.RegisterScreen
import com.frontend.petfinder.pets.presentation.RegisterPetScreen

@Composable
fun PetFinderNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Login.route
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
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(NavRoutes.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // Aquí cargamos toda la interfaz del menú inferior (MainScreen)
        composable(NavRoutes.Main.route) {
            MainScreen(rootNavController = navController)
        }

        composable(NavRoutes.RegisterPet.route) {
            RegisterPetScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}