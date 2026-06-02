package com.frontend.petfinder.geofencing.data

// =============================================================================
// DTOs de las CARDS DE DETALLE DEL MAPA
//   GET /map/pets/{mascotaId}         -> MapPetCardDto
//   GET /map/collaborators/{personaId} -> MapCollaboratorCardDto
// Reutiliza PointDto (definido en GeofencingApi.kt) para las ubicaciones.
// =============================================================================

// ── Card de mascota ──────────────────────────────────────────────────────────
data class MapPetCardDto(
    val mascotaId: String,
    val relacion: String,            // "tuya" | "compartida" | "comunidad"
    val nombre: String,
    val tipo: String? = null,
    val sexo: String? = null,
    val colorPrimario: String? = null,
    val rasgosParticulares: String? = null,
    val estado: String,
    // Listas nullable: Gson IGNORA los defaults de Kotlin, así que un campo ausente en el
    // JSON queda null (no emptyList). La respuesta "comunidad" NO trae propietarios → null.
    val fotos: List<MapCardFotoDto>? = null,
    val recompensa: Double? = null,
    val alertaComunidad: MapCardAlertaDto? = null,
    val fechaPerdida: String? = null,
    val ultimoAvistamiento: MapCardAvistamientoDto? = null,
    val propietarios: List<MapCardPropietarioDto>? = null,
    val dueno: MapCardDuenoDto? = null,        // solo en relacion = "comunidad"
    val fichaMedica: MapCardFichaMedicaDto? = null // solo en relacion = "tuya" y si existe
)

data class MapCardFotoDto(
    val fotoId: Int? = null,
    val fotoUrl: String? = null,
    val esPrincipal: Boolean = false
)

data class MapCardAlertaDto(
    val activa: Boolean? = null,
    val expiraEl: String? = null
)

data class MapCardAvistamientoDto(
    val lat: Double? = null,
    val lng: Double? = null,
    val fecha: String? = null
)

data class MapCardPropietarioDto(
    val nombre: String? = null,
    val apellidoPaterno: String? = null,
    val fotoUrl: String? = null,
    val tipoRelacion: String? = null
)

data class MapCardContactoDto(
    val tipo: String? = null,
    val valor: String? = null
)

data class MapCardDuenoDto(
    val nombre: String? = null,
    val apellidoPaterno: String? = null,
    val fotoUrl: String? = null,
    val contactoPrincipal: MapCardContactoDto? = null
)

data class MapCardFichaMedicaDto(
    val fichaId: Int? = null,
    val mascotaId: String? = null,
    val alergias: String? = null,
    val enfermedadesCronicas: String? = null,
    val medicacionDiaria: String? = null,
    val tipoSangre: String? = null,
    val notasVeterinarias: String? = null
)

// ── Card de colaborador ──────────────────────────────────────────────────────
data class MapCollaboratorCardDto(
    val personaId: String,
    val nombre: String,
    val apellidoPaterno: String? = null,
    val fotoPerfilUrl: String? = null,
    val tipoRelacion: String? = null,
    val mascotasCompartidas: List<MapCardSharedPetDto>? = null,
    val mediosContacto: List<MapCardMedioContactoDto>? = null,
    val ubicacion: PointDto? = null
)

data class MapCardSharedPetDto(
    val mascotaId: String,
    val nombre: String? = null,
    val fotoUrl: String? = null
)

data class MapCardMedioContactoDto(
    val tipo: String? = null,
    val valor: String? = null,
    val esPrincipal: Boolean = false,
    val esEmergencia: Boolean = false
)
