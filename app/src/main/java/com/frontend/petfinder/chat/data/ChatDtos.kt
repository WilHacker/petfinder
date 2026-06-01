package com.frontend.petfinder.chat.data

import com.google.gson.annotations.SerializedName

// ── Referencias compartidas ───────────────────────────────────────────────────

data class ChatMascotaDto(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("fotoUrl") val fotoUrl: String? = null
)

/** Otro participante en GET /chats (sin usuarioId — solo para el encabezado de la lista). */
data class ChatOtherParticipantDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("apellidoPaterno") val apellidoPaterno: String? = null,
    @SerializedName("fotoUrl") val fotoUrl: String? = null
) {
    val nombreCompleto: String
        get() = listOfNotNull(nombre, apellidoPaterno).joinToString(" ").ifBlank { "Usuario" }
}

/** Participante completo en GET /chats/{id} (incluye usuarioId). */
data class ChatParticipantDto(
    @SerializedName("usuarioId") val usuarioId: String,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("apellidoPaterno") val apellidoPaterno: String? = null,
    @SerializedName("fotoUrl") val fotoUrl: String? = null
) {
    val nombreCompleto: String
        get() = listOfNotNull(nombre, apellidoPaterno).joinToString(" ").ifBlank { "Usuario" }
}

/** Autor embebido en cada mensaje (GET/POST /chats/{id}/messages). */
data class ChatMessageAutorDto(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("apellidoPaterno") val apellidoPaterno: String? = null,
    @SerializedName("fotoPerfilUrl") val fotoPerfilUrl: String? = null
) {
    val nombreCompleto: String
        get() = listOfNotNull(nombre, apellidoPaterno).joinToString(" ").ifBlank { "Usuario" }
}

// ── Estado de la conversación ─────────────────────────────────────────────────

object ChatEstado {
    const val PENDIENTE = "pendiente"
    const val ACEPTADA = "aceptada"
    const val RECHAZADA = "rechazada"
}

// ── GET /chats ────────────────────────────────────────────────────────────────

data class ChatSummaryDto(
    @SerializedName("conversacionId") val conversacionId: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("soyDueno") val soyDueno: Boolean = false,
    @SerializedName("mascota") val mascota: ChatMascotaDto,
    @SerializedName("otroParticipante") val otroParticipante: ChatOtherParticipantDto?,
    @SerializedName("ultimoMensaje") val ultimoMensaje: String? = null,
    @SerializedName("ultimaActividad") val ultimaActividad: String? = null,
    @SerializedName("noLeidos") val noLeidos: Int = 0
)

// ── GET /chats/{id} ───────────────────────────────────────────────────────────

data class ChatDetailDto(
    @SerializedName("conversacionId") val conversacionId: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("intentos") val intentos: Int = 0,
    @SerializedName("maxIntentos") val maxIntentos: Int = 2,
    @SerializedName("mascota") val mascota: ChatMascotaDto,
    @SerializedName("dueno") val dueno: ChatParticipantDto?,
    @SerializedName("rescatista") val rescatista: ChatParticipantDto?
) {
    val intentosRestantes: Int get() = (maxIntentos - intentos).coerceAtLeast(0)
}

// ── Mensajes (GET/POST /chats/{id}/messages) ──────────────────────────────────

data class ChatMessageDto(
    @SerializedName("mensajeId") val mensajeId: String,
    @SerializedName("conversacionId") val conversacionId: String,
    @SerializedName("autorUsuarioId") val autorUsuarioId: String?,
    @SerializedName("contenido") val contenido: String? = null,
    @SerializedName("fotoUrl") val fotoUrl: String? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null,
    @SerializedName("creadoEl") val creadoEl: String,
    @SerializedName("leidoEl") val leidoEl: String? = null,
    @SerializedName("autor") val autor: ChatMessageAutorDto? = null
)

// ── Respuestas de acciones ────────────────────────────────────────────────────

/** POST /sightings/{avistamientoId}/chat */
data class StartChatResponse(
    @SerializedName("conversacionId") val conversacionId: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("mensaje") val mensaje: String? = null
)

/** PUT /chats/{id}/accept y /chats/{id}/read */
data class ChatOkResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("conversacionId") val conversacionId: String? = null
)

/** PUT /chats/{id}/decline */
data class ChatDeclineResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("conversacionId") val conversacionId: String? = null,
    @SerializedName("intentosRestantes") val intentosRestantes: Int = 0
)
