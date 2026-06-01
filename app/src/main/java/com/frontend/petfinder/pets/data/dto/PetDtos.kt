package com.frontend.petfinder.pets.data.dto

import com.google.gson.annotations.SerializedName

// =============================================================================
// DTOs de respuesta — Fotos
// =============================================================================

data class FotoMascotaDto(
    @SerializedName("fotoId") val fotoId: Int,
    @SerializedName("fotoUrl") val fotoUrl: String,
    @SerializedName("esPrincipal") val esPrincipal: Boolean
)

// =============================================================================
// DTOs de respuesta — Tipos de mascota
// =============================================================================

data class TipoMascotaDto(
    @SerializedName("tipoId") val tipoId: Int,
    @SerializedName("nombre") val nombre: String
)

data class TipoMascotaRefDto(
    @SerializedName("tipoId") val tipoId: Int,
    @SerializedName("nombre") val nombre: String
)

// =============================================================================
// DTOs de respuesta — Placa QR
// =============================================================================

data class PlacaQrDto(
    @SerializedName("placaId") val placaId: String,
    @SerializedName("tokenAcceso") val tokenAcceso: String,
    @SerializedName("estaActiva") val estaActiva: Boolean
)

// =============================================================================
// DTOs de respuesta — Registro y listado de mascotas
// =============================================================================

data class RegisterPetResponse(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("placaQr") val placaQr: PlacaQrDto?
)

data class PetListItemDto(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("tipoMascota") val tipoMascota: TipoMascotaRefDto?,
    @SerializedName("fotos") val fotos: List<FotoMascotaDto>?
)

// =============================================================================
// DTOs de respuesta — Ficha pública del QR
// =============================================================================

data class ContactoPublicoDto(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("valor") val valor: String
)

data class PropietarioPublicoDto(
    @SerializedName("personaId") val personaId: String,
    @SerializedName("nombreCompleto") val nombreCompleto: String,
    @SerializedName("fotoPerfilUrl") val fotoPerfilUrl: String?,
    @SerializedName("tipoRelacion") val tipoRelacion: String,
    @SerializedName("contactos") val contactos: List<ContactoPublicoDto> = emptyList()
)

data class FotoPublicaDto(
    @SerializedName("fotoId") val fotoId: Int,
    @SerializedName("url") val url: String,
    @SerializedName("esPrincipal") val esPrincipal: Boolean
)

data class FichaMedicaPublicaDto(
    @SerializedName("vacunado") val vacunado: Boolean? = null,
    @SerializedName("esterilizado") val esterilizado: Boolean? = null,
    @SerializedName("condicionEspecial") val condicionEspecial: String? = null,
    @SerializedName("alergia") val alergia: String? = null
)

data class RegistroMedicoPublicoDto(
    @SerializedName("registroId") val registroId: Int? = null,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("fecha") val fecha: String? = null,
    @SerializedName("veterinario") val veterinario: String? = null
)

data class ReporteActivoDto(
    @SerializedName("reporteId") val reporteId: Int,
    @SerializedName("recompensa") val recompensa: Double?,
    @SerializedName("fechaPerdida") val fechaPerdida: String?
)

data class PublicPetCardDto(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("sexo") val sexo: String?,
    @SerializedName("colorPrimario") val colorPrimario: String?,
    @SerializedName("rasgosParticulares") val rasgosParticulares: String?,
    @SerializedName("estado") val estado: String,
    @SerializedName("estaExtraviada") val estaExtraviada: Boolean,
    @SerializedName("reporteActivo") val reporteActivo: ReporteActivoDto? = null,
    @SerializedName("fotos") val fotos: List<FotoPublicaDto>?,
    @SerializedName("fichaMedica") val fichaMedica: FichaMedicaPublicaDto?,
    @SerializedName("registrosMedicos") val registrosMedicos: List<RegistroMedicoPublicoDto>?,
    @SerializedName("propietarios") val propietarios: List<PropietarioPublicoDto>
)

// =============================================================================
// DTOs de respuesta — Co-propietarios
// =============================================================================

data class PetOwnerRelationDto(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("personaId") val personaId: String,
    @SerializedName("tipoRelacion") val tipoRelacion: String,
    @SerializedName("recibeAlertas") val recibeAlertas: Boolean,
    @SerializedName("mostrarEnQr") val mostrarEnQr: Boolean
)

// =============================================================================
// DTOs de respuesta — Historial de escaneos QR (Historia 12)
// =============================================================================

data class PetScanDto(
    @SerializedName("escaneoId") val escaneoId: Int,
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?,
    @SerializedName("escaneadoEl") val escaneadoEl: String
)

// =============================================================================
// DTOs de respuesta — Historial de reportes de extravío (Historia 9/11)
// =============================================================================

data class PetReportDto(
    @SerializedName("reporte_id") val reporteId: Int,
    @SerializedName("fecha_perdida") val fechaPerdida: String,
    @SerializedName("recompensa") val recompensa: Double?,
    @SerializedName("estado_reporte") val estadoReporte: String,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?
)

// =============================================================================
// DTOs de petición
// =============================================================================

data class UpdateStatusRequest(
    @SerializedName("estado") val estado: String, // en_casa | en_paseo | extraviada | recuperada
    @SerializedName("recompensa") val recompensa: Double? = null
)

data class UpdateLocationRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class UpdatePetRequest(
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("tipoId") val tipoId: Int? = null,
    @SerializedName("colorPrimario") val colorPrimario: String? = null,
    @SerializedName("rasgosParticulares") val rasgosParticulares: String? = null,
    @SerializedName("sexo") val sexo: String? = null
)

// Respuesta de PUT /pets/{id}/photos/{fotoId}/principal
data class SetPrincipalResponse(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("fotoPrincipalUrl") val fotoPrincipalUrl: String? = null
)

data class AddOwnerRequest(
    @SerializedName("correoElectronico") val correoElectronico: String,
    @SerializedName("tipoRelacion") val tipoRelacion: String = "Cuidador",
    @SerializedName("recibeAlertas") val recibeAlertas: Boolean = true,
    @SerializedName("mostrarEnQr") val mostrarEnQr: Boolean = true
)

data class QrScanRequest(
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?
)

data class CommunityAlertRequest(
    @SerializedName("radio") val radio: Int? = null
)

data class CommunityAlertResponse(
    @SerializedName("message") val mensaje: String?,
    @SerializedName("usuariosNotificados") val alertados: Int? = null,
    @SerializedName("expiraEl") val expiraEl: String? = null,
    @SerializedName("razon") val razon: String? = null
)

data class UpdateRewardRequest(
    @SerializedName("recompensa") val recompensa: Double
)

data class UpdateRewardResponse(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("recompensa") val recompensa: Double
)

// =============================================================================
// DTOs de respuesta — Detalle completo de mascota (GET /pets/{id})
// =============================================================================

data class MedioContactoDetailDto(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("valor") val valor: String,
    @SerializedName("esPrincipal") val esPrincipal: Boolean = false
)

data class PersonaDetailDto(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellidoPaterno") val apellidoPaterno: String,
    @SerializedName("fotoPerfilUrl") val fotoPerfilUrl: String?,
    @SerializedName("mediosContacto") val mediosContacto: List<MedioContactoDetailDto> = emptyList()
)

data class PropietarioDetailDto(
    @SerializedName("personaId") val personaId: String,
    @SerializedName("tipoRelacion") val tipoRelacion: String,
    @SerializedName("recibeAlertas") val recibeAlertas: Boolean,
    @SerializedName("mostrarEnQr") val mostrarEnQr: Boolean,
    @SerializedName("persona") val persona: PersonaDetailDto
)

data class PlacaQrDetailDto(
    @SerializedName("placaId") val placaId: String,
    @SerializedName("tokenAcceso") val tokenAcceso: String,
    @SerializedName("estaActiva") val estaActiva: Boolean
)

data class UbicacionDto(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class PetDetailDto(
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("tipoId") val tipoId: Int?,
    @SerializedName("sexo") val sexo: String?,
    @SerializedName("colorPrimario") val colorPrimario: String?,
    @SerializedName("rasgosParticulares") val rasgosParticulares: String?,
    @SerializedName("estado") val estado: String,
    @SerializedName("fechaUltimaUbicacion") val fechaUltimaUbicacion: String?,
    @SerializedName("creadoEl") val creadoEl: String,
    @SerializedName("tipoMascota") val tipoMascota: TipoMascotaRefDto?,
    @SerializedName("placaQr") val placaQr: PlacaQrDetailDto?,
    @SerializedName("fotos") val fotos: List<FotoMascotaDto>?,
    @SerializedName("propietarios") val propietarios: List<PropietarioDetailDto> = emptyList(),
    @SerializedName("ubicacion") val ubicacion: UbicacionDto?
)

// =============================================================================
// DTOs — Historial médico (GET/POST/PUT /pets/{id}/medical)
// =============================================================================

data class MedicalRecordDto(
    @SerializedName("registroId") val registroId: Int,
    @SerializedName("mascotaId") val mascotaId: String,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("fecha") val fecha: String?,
    @SerializedName("veterinario") val veterinario: String?,
    @SerializedName("creadoEl") val creadoEl: String
)

data class CreateMedicalRecordRequest(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("fecha") val fecha: String? = null,
    @SerializedName("veterinario") val veterinario: String? = null
)

data class UpdateMedicalRecordRequest(
    @SerializedName("tipo") val tipo: String? = null,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("fecha") val fecha: String? = null,
    @SerializedName("veterinario") val veterinario: String? = null
)
