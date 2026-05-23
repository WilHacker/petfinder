package com.frontend.petfinder.profile.data

import com.frontend.petfinder.profile.data.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface UserApi {

    @GET("users/me")
    suspend fun getMyProfile(): Response<UserProfileDto>

    @PUT("users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<PersonaDto>

    @Multipart
    @PUT("users/me/photo")
    suspend fun updateProfilePhoto(
        @Part foto: MultipartBody.Part
    ): Response<PersonaDto>

    @PUT("users/me/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): Response<Map<String, String>>

    @PUT("users/me/location")
    suspend fun updateLocation(@Body request: LocationRequest): Response<Map<String, String>>

    @GET("users/me/contacts/emergency")
    suspend fun getEmergencyContacts(): Response<List<ContactoDto>>

    @POST("users/me/contacts")
    suspend fun addContact(@Body request: CreateContactRequest): Response<ContactoDto>

    @PUT("users/me/contacts/{id}")
    suspend fun updateContact(
        @Path("id") id: Int,
        @Body request: UpdateContactRequest
    ): Response<ContactoDto>

    @DELETE("users/me/contacts/{id}")
    suspend fun deleteContact(@Path("id") id: Int): Response<Map<String, String>>
}