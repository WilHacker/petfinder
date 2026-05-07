package com.frontend.petfinder.core.navigation

sealed class NavRoutes(val route: String) {
    object Login : NavRoutes("login")
    object Register : NavRoutes("register")
    object Main : NavRoutes("main") // La nueva ruta contenedora con el menú inferior
    object RegisterPet : NavRoutes("register_pet")
}