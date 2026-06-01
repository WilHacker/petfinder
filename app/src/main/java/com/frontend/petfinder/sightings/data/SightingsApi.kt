package com.frontend.petfinder.sightings.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface SightingsApi {

    // ── Avistamientos ─────────────────────────────────────────────────────────

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

    // ── Comentarios ───────────────────────────────────────────────────────────

    @Multipart
    @POST("sightings/{id}/comments")
    suspend fun addComment(
        @Path("id") avistamientoId: String,
        @Part("mensaje") mensaje: RequestBody?,
        @Part("lat") lat: RequestBody?,
        @Part("lng") lng: RequestBody?,
        @Part("replyToUserId") replyToUserId: RequestBody?,
        @Part foto: MultipartBody.Part?
    ): Response<SightingCommentDto>

    @GET("sightings/{id}/comments")
    suspend fun getComments(@Path("id") avistamientoId: String): Response<List<SightingCommentDto>>

    // ── No leídos ─────────────────────────────────────────────────────────────

    @PUT("sightings/{id}/read")
    suspend fun markAsRead(@Path("id") avistamientoId: String): Response<Unit>

    @GET("sightings/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountDto>

    // ── Chat — listas del dueño y rescatista ──────────────────────────────────

    @GET("sightings/my-pets/threads")
    suspend fun getMyPetsThreads(): Response<List<SightingThreadDto>>

    @GET("sightings/my-participations")
    suspend fun getMyParticipations(): Response<List<MyParticipationDto>>

    // ── Agradecimientos (legacy) ──────────────────────────────────────────────

    @POST("sightings/{id}/thanks")
    suspend fun sendThanks(
        @Path("id") avistamientoId: String,
        @Body request: CreateThanksRequest
    ): Response<ThanksDto>

    @GET("sightings/{id}/thanks")
    suspend fun getThanks(@Path("id") avistamientoId: String): Response<List<ThanksDto>>
}
