package com.frontend.petfinder.chat.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ChatApi {

    /** El dueño inicia (o reabre) el chat privado desde un avistamiento. */
    @POST("sightings/{avistamientoId}/chat")
    suspend fun startChat(@Path("avistamientoId") avistamientoId: String): Response<StartChatResponse>

    /** Lista de conversaciones del usuario (como dueño y como rescatista). */
    @GET("chats")
    suspend fun getChats(): Response<List<ChatSummaryDto>>

    /** Detalle de una conversación: estado + perfiles completos de ambos participantes. */
    @GET("chats/{conversacionId}")
    suspend fun getChatDetail(@Path("conversacionId") conversacionId: String): Response<ChatDetailDto>

    /** El rescatista acepta la invitación. */
    @PUT("chats/{conversacionId}/accept")
    suspend fun acceptChat(@Path("conversacionId") conversacionId: String): Response<ChatOkResponse>

    /** El rescatista rechaza la invitación. */
    @PUT("chats/{conversacionId}/decline")
    suspend fun declineChat(@Path("conversacionId") conversacionId: String): Response<ChatDeclineResponse>

    /** Envía un mensaje (texto y/o foto y/o GPS). Al menos un campo obligatorio. */
    @Multipart
    @POST("chats/{conversacionId}/messages")
    suspend fun sendMessage(
        @Path("conversacionId") conversacionId: String,
        @Part("contenido") contenido: RequestBody?,
        @Part("lat") lat: RequestBody?,
        @Part("lng") lng: RequestBody?,
        @Part foto: MultipartBody.Part?
    ): Response<ChatMessageDto>

    /** Historial completo del chat (cronológico, más antiguo primero). */
    @GET("chats/{conversacionId}/messages")
    suspend fun getMessages(@Path("conversacionId") conversacionId: String): Response<List<ChatMessageDto>>

    /** Marca como leídos todos los mensajes del otro participante. */
    @PUT("chats/{conversacionId}/read")
    suspend fun markAsRead(@Path("conversacionId") conversacionId: String): Response<ChatOkResponse>
}
