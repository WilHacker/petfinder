package com.frontend.petfinder.chat.data

import com.frontend.petfinder.core.network.ApiServices
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

/** Excepción con mensaje listo para mostrar al usuario. */
class ChatException(message: String) : Exception(message)

object ChatRepository {

    private val textPlain = "text/plain".toMediaType()
    private val api get() = ApiServices.chats

    // ── Inicio de chat ─────────────────────────────────────────────────────────

    suspend fun startChat(avistamientoId: String): Result<StartChatResponse> = runCatching {
        val r = api.startChat(avistamientoId)
        if (r.isSuccessful) {
            r.body() ?: throw ChatException("Respuesta vacía del servidor")
        } else throw ChatException(
            when (r.code()) {
                400 -> "Este rescatista reportó de forma anónima, no se puede iniciar el chat."
                403 -> "Has alcanzado el límite de invitaciones para esta mascota."
                else -> "No se pudo iniciar el chat (${r.code()})."
            }
        )
    }

    // ── Listado y detalle ────────────────────────────────────────────────────────

    suspend fun getChats(): Result<List<ChatSummaryDto>> = runCatching {
        val r = api.getChats()
        if (r.isSuccessful) r.body() ?: emptyList() else throw httpError(r)
    }

    suspend fun getChatDetail(conversacionId: String): Result<ChatDetailDto> = runCatching {
        val r = api.getChatDetail(conversacionId)
        if (r.isSuccessful) r.body()!! else throw httpError(r)
    }

    // ── Invitación ────────────────────────────────────────────────────────────────

    suspend fun acceptChat(conversacionId: String): Result<ChatOkResponse> = runCatching {
        val r = api.acceptChat(conversacionId)
        if (r.isSuccessful) r.body() ?: ChatOkResponse(true, conversacionId)
        else throw ChatException(
            if (r.code() == 400) "Esta invitación ya fue respondida." else "No se pudo aceptar (${r.code()})."
        )
    }

    suspend fun declineChat(conversacionId: String): Result<ChatDeclineResponse> = runCatching {
        val r = api.declineChat(conversacionId)
        if (r.isSuccessful) r.body() ?: ChatDeclineResponse(true, conversacionId)
        else throw ChatException(
            if (r.code() == 400) "Esta invitación ya fue respondida." else "No se pudo rechazar (${r.code()})."
        )
    }

    // ── Mensajes ──────────────────────────────────────────────────────────────────

    suspend fun getMessages(conversacionId: String): Result<List<ChatMessageDto>> = runCatching {
        val r = api.getMessages(conversacionId)
        if (r.isSuccessful) r.body() ?: emptyList() else throw httpError(r)
    }

    suspend fun sendMessage(
        conversacionId: String,
        contenido: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        foto: MultipartBody.Part? = null
    ): Result<ChatMessageDto> = runCatching {
        val contenidoBody = contenido?.takeIf { it.isNotBlank() }?.toRequestBody(textPlain)
        val latBody = lat?.toString()?.toRequestBody(textPlain)
        val lngBody = lng?.toString()?.toRequestBody(textPlain)
        val r = api.sendMessage(conversacionId, contenidoBody, latBody, lngBody, foto)
        if (r.isSuccessful) r.body()!! else throw ChatException(
            when (r.code()) {
                400 -> "El chat no está activo o el mensaje está vacío."
                403 -> "No eres participante de este chat."
                else -> "No se pudo enviar el mensaje (${r.code()})."
            }
        )
    }

    // ── Leído ─────────────────────────────────────────────────────────────────────

    suspend fun markAsRead(conversacionId: String): Result<Unit> = runCatching {
        val r = api.markAsRead(conversacionId)
        if (!r.isSuccessful) throw httpError(r)
    }

    private fun httpError(r: Response<*>): ChatException =
        ChatException("Error de red (${r.code()})")
}
