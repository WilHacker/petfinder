package com.frontend.petfinder.pets.data

import com.frontend.petfinder.pets.data.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface PetApi {

    // --- Tipos de mascota ---

    @GET("tipos-mascota")
    suspend fun getTiposMascota(): Response<List<TipoMascotaDto>>

    // --- CRUD de mascotas ---

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

    @GET("pets")
    suspend fun getMyPets(): Response<List<PetListItemDto>>

    @GET("pets/{id}")
    suspend fun getPetDetail(@Path("id") petId: String): Response<PetListItemDto>

    @PUT("pets/{id}")
    suspend fun updatePet(
        @Path("id") petId: String,
        @Body request: UpdatePetRequest
    ): Response<PetListItemDto>

    @DELETE("pets/{id}")
    suspend fun deletePet(@Path("id") petId: String): Response<Map<String, String>>

    // --- QR ---

    @GET("pets/{id}/qr")
    suspend fun getPetQrCode(@Path("id") petId: String): Response<ResponseBody>

    @GET("qr/{token}")
    suspend fun getPublicPetCard(@Path("token") token: String): Response<PublicPetCardDto>

    @POST("qr/{token}/scan")
    suspend fun registerQrScan(
        @Path("token") token: String,
        @Body request: QrScanRequest
    ): Response<PetScanDto>

    // --- Estado y ubicación ---

    @PUT("pets/{id}/status")
    suspend fun updatePetStatus(
        @Path("id") petId: String,
        @Body request: UpdateStatusRequest
    ): Response<Any>

    @PUT("pets/{id}/location")
    suspend fun updatePetLocation(
        @Path("id") petId: String,
        @Body request: UpdateLocationRequest
    ): Response<Map<String, String>>

    // --- Fotos ---

    @Multipart
    @POST("pets/{id}/photos")
    suspend fun addPetPhotos(
        @Path("id") petId: String,
        @Part("fotoPrincipalIndex") fotoPrincipalIndex: RequestBody?,
        @Part fotos: List<MultipartBody.Part>
    ): Response<List<FotoMascotaDto>>

    @DELETE("pets/{id}/photos/{fotoId}")
    suspend fun deletePetPhoto(
        @Path("id") petId: String,
        @Path("fotoId") fotoId: Int
    ): Response<Map<String, String>>

    // --- Co-propietarios ---

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

    // --- Historial (Sprint 2) ---

    @GET("pets/{id}/scans")
    suspend fun getPetScans(@Path("id") petId: String): Response<List<PetScanDto>>

    @GET("pets/{id}/reports")
    suspend fun getPetReports(@Path("id") petId: String): Response<List<PetReportDto>>
}
