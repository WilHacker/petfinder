package com.frontend.petfinder.pets.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// =============================================================================
// DTOs para el registro de mascotas y la Placa QR
// =============================================================================

data class PlacaQrDto(
    val placaId: String,
    val tokenAcceso: String,
    val estaActiva: Boolean
)

data class RegisterPetResponse(
    val mascotaId: String,
    val nombre: String,
    val placaQr: PlacaQrDto?
)

// =============================================================================
// DTOs para el catálogo (Dropdown) de tipos de mascota
// =============================================================================

data class TipoMascotaDto(
    val tipoId: Int,
    val nombre: String
)

// =============================================================================
// NUEVOS DTOs para listar "Mis Mascotas" (Para la pantalla de Mis Zonas)
// =============================================================================

data class FotoMascotaDto(
    val fotoUrl: String,
    val esPrincipal: Boolean
)

data class TipoMascotaRefDto(
    val tipoId: Int,
    val nombre: String
)

data class PetListItemDto(
    val mascotaId: String,
    val nombre: String,
    val estado: String,
    val tipoMascota: TipoMascotaRefDto?,
    val fotos: List<FotoMascotaDto>?
)

// =============================================================================
// INTERFAZ DE LA API (Retrofit)
// =============================================================================

interface PetApi {

    // 1. Obtener la lista dinámica de tipos (Perro, Gato, etc.) para el formulario
    @GET("tipos-mascota")
    suspend fun getTiposMascota(): Response<List<TipoMascotaDto>>

    // 2. NUEVO: Obtener la lista de mis mascotas registradas
    @GET("pets")
    suspend fun getMyPets(): Response<List<PetListItemDto>>

    // 3. Registrar una nueva mascota enviando texto y fotos (Multipart)
    @Multipart
    @POST("pets")
    suspend fun registerPet(
        @Part("nombre") nombre: RequestBody,
        @Part("tipoId") tipoId: RequestBody?,
        @Part("sexo") sexo: RequestBody?,
        @Part("colorPrimario") colorPrimario: RequestBody?,
        @Part("rasgosParticulares") rasgosParticulares: RequestBody?,
        @Part("fotoPrincipalIndex") fotoPrincipalIndex: RequestBody?,
        @Part fotos: List<MultipartBody.Part>?
    ): Response<RegisterPetResponse>
}