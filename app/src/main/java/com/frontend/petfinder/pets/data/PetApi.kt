package com.frontend.petfinder.pets.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody // ¡NUEVA IMPORTACIÓN REQUERIDA!
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import com.frontend.petfinder.pets.data.dto.*
import retrofit2.http.*

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
// DTO para fijar la ubicación manualmente
data class UpdateLocationRequest(
    val lat: Double,
    val lng: Double
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

    // ¡CAMBIO AQUÍ! Retornamos ResponseBody para que Gson no intente parsear el HTML/Texto crudo
    @GET("pets/{id}/qr")
    suspend fun getPetQrCode(@Path("id") petId: String): Response<ResponseBody>

    @GET("pets/{id}/card")
    suspend fun getPublicPetCard(@Path("id") petId: String): Response<PublicPetCardDto>

    @PUT("pets/{id}/status")
    suspend fun updatePetStatus(
        @Path("id") petId: String,
        @Body request: UpdateStatusRequest
    ): Response<Any> // Puede devolver el Pet actualizado

    // --- Co-Propietarios ---

    @POST("pets/{id}/owners")
    suspend fun addPetOwner(
        @Path("id") petId: String,
        @Body request: AddOwnerRequest
    ): Response<PetOwnerRelationDto>

    @DELETE("pets/{id}/owners/{personaId}")
    suspend fun removePetOwner(
        @Path("id") petId: String,
        @Path("personaId") personaId: String
    ): Response<Map<String, String>>

    // --- Multimedia (Límites: Máx 4 fotos, 5MB c/u) ---

    @Multipart
    @POST("pets/{id}/photos")
    suspend fun replacePetPhotos(
        @Path("id") petId: String,
        @Part("fotoPrincipalIndex") fotoPrincipalIndex: RequestBody?,
        @Part fotos: List<MultipartBody.Part>
    ): Response<List<FotoMascotaDto>>

    @DELETE("pets/{id}/photos/{fotoId}")
    suspend fun deletePetPhoto(
        @Path("id") petId: String,
        @Path("fotoId") fotoId: Int
    ): Response<Map<String, String>>

    // NUEVO: Fijar ubicación manual de la mascota sin iniciar paseo
    @PUT("pets/{id}/location")
    suspend fun updatePetLocation(
        @Path("id") petId: String,
        @Body request: UpdateLocationRequest
    ): Response<Map<String, String>>
}