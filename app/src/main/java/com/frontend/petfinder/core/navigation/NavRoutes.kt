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

    // QR público — accesible sin sesión via deep link
    object QrPublicCard : NavRoutes("qr/{token}") {
        fun createRoute(token: String) = "qr/$token"
        const val ARG_TOKEN = "token"
    }

    // Perfil del usuario autenticado
    object Profile : NavRoutes("profile")

    // Detalle de mascota
    object PetDetail : NavRoutes("pet_detail/{mascotaId}") {
        fun createRoute(mascotaId: String) = "pet_detail/$mascotaId"
        const val ARG_PET_ID = "mascotaId"
    }

    // Historial médico de una mascota
    object MedicalHistory : NavRoutes("medical/{mascotaId}") {
        fun createRoute(mascotaId: String) = "medical/$mascotaId"
        const val ARG_PET_ID = "mascotaId"
    }

    // Editar datos de una mascota
    object EditPet : NavRoutes("edit_pet/{mascotaId}") {
        fun createRoute(mascotaId: String) = "edit_pet/$mascotaId"
        const val ARG_PET_ID = "mascotaId"
    }

    // Detalle de zona segura
    object ZoneDetail : NavRoutes("zone_detail/{zonaId}") {
        fun createRoute(zonaId: Int) = "zone_detail/$zonaId"
        const val ARG_ZONE_ID = "zonaId"
    }
}