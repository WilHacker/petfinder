package com.frontend.petfinder.sightings.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SightingsApi {

    @GET("sightings/pets/{petId}")
    suspend fun getSightings(@Path("petId") petId: String): Response<List<SightingDto>>

    @POST("sightings/pets/{petId}")
    suspend fun reportSighting(
        @Path("petId") petId: String,
        @Body request: CreateSightingRequest
    ): Response<SightingDto>

    @GET("sightings/{id}/thanks")
    suspend fun getThanks(@Path("id") avistamientoId: Int): Response<List<ThanksDto>>

    @POST("sightings/{id}/thanks")
    suspend fun sendThanks(
        @Path("id") avistamientoId: Int,
        @Body request: CreateThanksRequest
    ): Response<ThanksDto>
}
