package com.frontend.petfinder.profile.data

import com.frontend.petfinder.profile.data.dto.ContactoDto
import com.frontend.petfinder.profile.data.dto.CreateContactRequest
import com.frontend.petfinder.profile.data.dto.LocationRequest
import com.frontend.petfinder.profile.data.dto.PersonaDto
import com.frontend.petfinder.profile.data.dto.UpdateProfileRequest
import com.frontend.petfinder.profile.data.dto.UserProfileDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {
    @GET("users/me")
    suspend fun getMyProfile(): Response<UserProfileDto>

    @PUT("users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<PersonaDto>

    @POST("users/me/contacts")
    suspend fun addContact(@Body request: CreateContactRequest): Response<ContactoDto>

    @DELETE("users/me/contacts/{id}")
    suspend fun deleteContact(@Path("id") id: Int): Response<Map<String, String>>

    // Este endpoint es vital para la Fase 3 (Motor de Rastreo)
    @PUT("users/me/location")
    suspend fun updateLocation(@Body request: LocationRequest): Response<Map<String, String>>
}