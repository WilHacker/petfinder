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
    @SerializedName("esPrincipal") val esPrincipal: Boolean
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
    @SerializedName("esPrincipal") val esPrincipal: Boolean
)

data class LocationRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)