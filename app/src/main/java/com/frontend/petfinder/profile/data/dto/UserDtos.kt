package com.frontend.petfinder.profile.data.dto

import com.google.gson.annotations.SerializedName

data class UserProfileDto(
    @SerializedName("usuarioId") val usuarioId: String,
    @SerializedName("correoElectronico") val correoElectronico: String,
    @SerializedName("estadoCuenta") val estadoCuenta: String,
    @SerializedName("persona") val persona: PersonaDto?
)

data class PersonaDto(
    @SerializedName("personaId") val personaId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellidoPaterno") val apellidoPaterno: String,
    @SerializedName("apellidoMaterno") val apellidoMaterno: String?,
    @SerializedName("ci") val ci: String?,
    @SerializedName("fotoPerfilUrl") val fotoPerfilUrl: String?,
    @SerializedName("fechaNacimiento") val fechaNacimiento: String?,
    @SerializedName("mediosContacto") val mediosContacto: List<ContactoDto> = emptyList()
)

data class ContactoDto(
    @SerializedName("contactoId") val contactoId: Int,
    @SerializedName("tipo") val tipo: String, // WhatsApp, Celular, Fijo, Telegram
    @SerializedName("valor") val valor: String,
    @SerializedName("esPrincipal") val esPrincipal: Boolean,
    @SerializedName("esEmergencia") val esEmergencia: Boolean = false
)

data class UpdateContactRequest(
    @SerializedName("tipo") val tipo: String? = null,
    @SerializedName("valor") val valor: String? = null,
    @SerializedName("esPrincipal") val esPrincipal: Boolean? = null,
    @SerializedName("esEmergencia") val esEmergencia: Boolean? = null
)

data class UpdateProfileRequest(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("apellidoPaterno") val apellidoPaterno: String? = null,
    @SerializedName("apellidoMaterno") val apellidoMaterno: String? = null,
    @SerializedName("ci") val ci: String? = null,
    @SerializedName("fechaNacimiento") val fechaNacimiento: String? = null
)

data class CreateContactRequest(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("valor") val valor: String,
    @SerializedName("esPrincipal") val esPrincipal: Boolean,
    @SerializedName("esEmergencia") val esEmergencia: Boolean = false
)

data class LocationRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class FcmTokenRequest(
    @SerializedName("tokenFcm") val tokenFcm: String
)

// =============================================================================
// DTOs — Perfil público del usuario (GET /users/{personaId}/card)
// =============================================================================

data class UserCardContactoDto(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("valor") val valor: String
)

data class UserCardPetDto(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("fotoUrl") val fotoUrl: String?
)

data class UserCardDto(
    @SerializedName("personaId") val personaId: String,
    @SerializedName("nombreCompleto") val nombreCompleto: String,
    @SerializedName("fotoPerfilUrl") val fotoPerfilUrl: String?,
    @SerializedName("contactos") val contactos: List<UserCardContactoDto> = emptyList(),
    @SerializedName("mascotas") val mascotas: List<UserCardPetDto> = emptyList()
)