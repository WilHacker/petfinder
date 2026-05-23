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

// DTO para "pet:status-changed"
data class PetStatusUpdate(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("estado") val estado: String
)

// DTO para "owner:profile-updated"
data class OwnerProfileUpdate(
    @SerializedName("personaId") val personaId: String,
    @SerializedName("fotoPerfilUrl") val fotoPerfilUrl: String?,
    @SerializedName("fechaActualizacion") val fechaActualizacion: String? = null
)

// DTO para "pet:profile-updated"
data class PetProfileUpdate(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("fotoUrl") val fotoUrl: String? = null,
    @SerializedName("fechaActualizacion") val fechaActualizacion: String? = null
)