package com.frontend.petfinder.pets.data.dto

import com.google.gson.annotations.SerializedName

// --- Respuestas ---

data class PublicPetCardDto(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("sexo") val sexo: String?,
    @SerializedName("colorPrimario") val colorPrimario: String?,
    @SerializedName("rasgosParticulares") val rasgosParticulares: String?,
    @SerializedName("estado") val estado: String,
    @SerializedName("fotos") val fotos: List<FotoMascotaDto>?,
    @SerializedName("propietarios") val propietarios: List<PropietarioPublicoDto>
)

data class FotoMascotaDto(
    @SerializedName("fotoId") val fotoId: Int,
    @SerializedName("fotoUrl") val fotoUrl: String,
    @SerializedName("esPrincipal") val esPrincipal: Boolean
)

data class PropietarioPublicoDto(
    @SerializedName("personaId") val personaId: String,
    @SerializedName("nombreCompleto") val nombreCompleto: String,
    @SerializedName("fotoPerfilUrl") val fotoPerfilUrl: String?,
    @SerializedName("tipoRelacion") val tipoRelacion: String
)

data class PetOwnerRelationDto(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("personaId") val personaId: String,
    @SerializedName("tipoRelacion") val tipoRelacion: String, // Dueño Principal, Familiar, Cuidador
    @SerializedName("recibeAlertas") val recibeAlertas: Boolean,
    @SerializedName("mostrarEnQr") val mostrarEnQr: Boolean
)

// --- Peticiones ---

data class UpdateStatusRequest(
    @SerializedName("estado") val estado: String // en_casa, en_paseo, extraviada, recuperada
)

data class AddOwnerRequest(
    @SerializedName("personaId") val personaId: String,
    @SerializedName("tipoRelacion") val tipoRelacion: String = "Cuidador",
    @SerializedName("recibeAlertas") val recibeAlertas: Boolean = true,
    @SerializedName("mostrarEnQr") val mostrarEnQr: Boolean = true
)