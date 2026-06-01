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

// DTO para "sighting:new"
data class SightingNewEvent(
    @SerializedName("avistamientoId") val avistamientoId: String,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?,
    @SerializedName("fotoUrl") val fotoUrl: String?,
    @SerializedName("mensaje") val mensaje: String?,
    @SerializedName("fechaAvistamiento") val fechaAvistamiento: String
)

// DTO para "sighting:comment-new"
data class SightingCommentEvent(
    @SerializedName("comentarioId") val comentarioId: String,
    @SerializedName("avistamientoId") val avistamientoId: String,
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("fotoUrl") val fotoUrl: String?,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?,
    @SerializedName("creadoEl") val creadoEl: String
)

// DTO para "community:alert-activated" — broadcast cuando un dueño pide ayuda a la comunidad
data class CommunityAlertActivatedEvent(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("radioMetros") val radioMetros: Double,
    @SerializedName("expiraEl") val expiraEl: String
)

// DTO para "map:lost-pet-added" — broadcast cuando una mascota pasa a extraviada
data class MapLostPetAddedEvent(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("fotoUrl") val fotoUrl: String?,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("fechaPerdida") val fechaPerdida: String?,
    @SerializedName("recompensa") val recompensa: Double?
)

// DTO para "map:lost-pet-removed" — broadcast cuando una mascota deja de estar extraviada
data class MapLostPetRemovedEvent(
    @SerializedName("mascotaId") val mascotaId: String
)