package com.frontend.petfinder.sightings.data

import com.google.gson.annotations.SerializedName

data class SightingDto(
    @SerializedName("avistamientoId") val avistamientoId: String,
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("mensajeRescatista") val mensajeRescatista: String?,
    @SerializedName("fotoEvidenciaUrl") val fotoEvidenciaUrl: String?,
    @SerializedName("fechaAvistamiento") val fechaAvistamiento: String,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?
)

// ── Agradecimientos (thanks) ─────────────────────────────────────────────────

data class ThanksAutorPersonaDto(
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("apellidoPaterno") val apellidoPaterno: String?,
    @SerializedName("fotoPerfilUrl") val fotoPerfilUrl: String?
)

data class ThanksAutorDto(
    @SerializedName("usuarioId") val usuarioId: String,
    @SerializedName("persona") val persona: ThanksAutorPersonaDto?
)

data class ThanksDto(
    @SerializedName("agradecimientoId") val agradecimientoId: Int,
    @SerializedName("avistamientoId") val avistamientoId: String,
    @SerializedName("mensaje") val mensaje: String?,
    @SerializedName("creadoEl") val creadoEl: String,
    @SerializedName("autor") val autor: ThanksAutorDto?
)

data class CreateThanksRequest(
    @SerializedName("mensaje") val mensaje: String? = null
)

// ── Comentarios ───────────────────────────────────────────────────────────────

data class SightingAutorDto(
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("apellidoPaterno") val apellidoPaterno: String?,
    @SerializedName("fotoPerfilUrl") val fotoPerfilUrl: String?
)

data class SightingCommentDto(
    @SerializedName("comentarioId") val comentarioId: String,
    @SerializedName("avistamientoId") val avistamientoId: String,
    @SerializedName("autorUsuarioId") val autorUsuarioId: String?,
    @SerializedName("replyToUserId") val replyToUserId: String?,
    @SerializedName("mensaje") val mensaje: String?,
    @SerializedName("fotoUrl") val fotoUrl: String?,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?,
    @SerializedName("creadoEl") val creadoEl: String,
    @SerializedName("autor") val autor: SightingAutorDto?
)

// ── Chat: pestaña "Mis mascotas" ─────────────────────────────────────────────

data class SightingThreadMascotaDto(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("estado") val estado: String?,
    @SerializedName("fotoUrl") val fotoUrl: String?
)

data class SightingThreadInfoDto(
    @SerializedName("avistamientoId") val avistamientoId: String,
    @SerializedName("fechaAvistamiento") val fechaAvistamiento: String,
    @SerializedName("totalHilos") val totalHilos: Int,
    @SerializedName("ultimaActividad") val ultimaActividad: String,
    @SerializedName("ultimoMensaje") val ultimoMensaje: String?,
    @SerializedName("noLeidos") val noLeidos: Int
)

data class SightingThreadDto(
    @SerializedName("mascota") val mascota: SightingThreadMascotaDto,
    @SerializedName("avistamiento") val avistamiento: SightingThreadInfoDto?
)

// ── Chat: pestaña "Ayudé" ────────────────────────────────────────────────────

data class ParticipationMascotaDto(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("estado") val estado: String?,
    @SerializedName("fotoUrl") val fotoUrl: String?
)

data class ParticipationDuenoDto(
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("fotoPerfilUrl") val fotoPerfilUrl: String?
)

data class MyParticipationDto(
    @SerializedName("avistamientoId") val avistamientoId: String,
    @SerializedName("mascota") val mascota: ParticipationMascotaDto,
    @SerializedName("dueno") val dueno: ParticipationDuenoDto?,
    @SerializedName("miUltimoMensaje") val miUltimoMensaje: String?,
    @SerializedName("ultimaRespuesta") val ultimaRespuesta: String?,
    @SerializedName("ultimaActividad") val ultimaActividad: String,
    @SerializedName("noLeidos") val noLeidos: Int
)

// ── Badge de no leídos ───────────────────────────────────────────────────────

data class UnreadCountDto(
    @SerializedName("total") val total: Int,
    @SerializedName("comoDueno") val comoDueno: Int,
    @SerializedName("comoRescatista") val comoRescatista: Int
)
