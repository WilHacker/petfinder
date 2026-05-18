package com.frontend.petfinder.core.navigation

sealed class NavRoutes(val route: String) {
    // Root graph
    object Login : NavRoutes("login")
    object Register : NavRoutes("register")
    object Main : NavRoutes("main")
    object RegisterPet : NavRoutes("register_pet")

    // Bottom nav graph
    object MapHome : NavRoutes("map_home")
    object MyPets : NavRoutes("my_pets")
    object MyZones : NavRoutes("my_zones")
}