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
