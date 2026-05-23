package com.frontend.petfinder.sightings.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface SightingsApi {

    @Multipart
    @POST("sightings/pets/{petId}")
    suspend fun reportSighting(
        @Path("petId") petId: String,
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody,
        @Part("mensajeRescatista") mensajeRescatista: RequestBody?,
        @Part foto: MultipartBody.Part?
    ): Response<SightingDto>

    @GET("sightings/pets/{petId}")
    suspend fun getSightings(@Path("petId") petId: String): Response<List<SightingDto>>

    @POST("sightings/{id}/thanks")
    suspend fun sendThanks(
        @Path("id") avistamientoId: String,
        @Body request: CreateThanksRequest
    ): Response<ThanksDto>

    @GET("sightings/{id}/thanks")
    suspend fun getThanks(@Path("id") avistamientoId: String): Response<List<ThanksDto>>
}
