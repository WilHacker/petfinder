package com.frontend.petfinder.sightings.data

import com.google.gson.annotations.SerializedName

data class SightingDto(
    @SerializedName("avistamientoId") val avistamientoId: Int,
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("reporteId") val reporteId: Int?,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?,
    @SerializedName("fotoEvidenciaUrl") val fotoEvidenciaUrl: String?,
    @SerializedName("creadoEl") val creadoEl: String,
    @SerializedName("reportadoPor") val reportadoPor: ReporterDto? = null,
    @SerializedName("agradecimientos") val agradecimientos: Int = 0
)

data class ReporterDto(
    @SerializedName("personaId") val personaId: String,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("fotoPerfilUrl") val fotoPerfilUrl: String?
)

data class ThanksDto(
    @SerializedName("agradecimientoId") val agradecimientoId: Int,
    @SerializedName("avistamientoId") val avistamientoId: Int,
    @SerializedName("mensaje") val mensaje: String?,
    @SerializedName("creadoEl") val creadoEl: String
)

data class CreateSightingRequest(
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null
)

data class CreateThanksRequest(
    @SerializedName("mensaje") val mensaje: String? = null
)
