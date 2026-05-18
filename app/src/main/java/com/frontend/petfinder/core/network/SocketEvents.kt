package com.frontend.petfinder.core.network

import com.google.gson.annotations.SerializedName

// DTO para "pet:location-updated"
data class PetLocationUpdate(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("estado") val estado: String,
    @SerializedName("fechaActualizacion") val fechaActualizacion: String
)

// DTO para "owner:location-updated"
data class OwnerLocationUpdate(
    @SerializedName("personaId") val personaId: String,
    @SerializedName("usuarioId") val usuarioId: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("fechaActualizacion") val fechaActualizacion: String
)

// DTO para alertas de zonas (entered/exited)
data class ZoneAlertEvent(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("zonaId") val zonaId: Int,
    @SerializedName("fechaHora") val fechaHora: String,
    @SerializedName("duracionMinutos") val duracionMinutos: Int? = null
)