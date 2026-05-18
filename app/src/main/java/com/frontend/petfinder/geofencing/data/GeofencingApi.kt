package com.frontend.petfinder.geofencing.data

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// =============================================================================
// DTOs COMPARTIDOS Y DEL MAPA PRINCIPAL (Snapshot)
// =============================================================================

data class PointDto(val lat: Double, val lng: Double)

// Hecho nullable (?) para evitar crashes si el backend no lo envía
data class GeometryDto(
    val type: String? = null,
    val coordinates: List<List<List<Double>>>? = emptyList()
)

data class UserMarkerDto(
    val personaId: String,
    val nombre: String,
    val fotoUrl: String?,
    val lat: Double,
    val lng: Double
)

data class PetMarkerDto(
    val reporteId: Int,
    val mascotaId: String,
    val nombre: String,
    val tipo: String,
    val fotoUrl: String?,
    val lat: Double,
    val lng: Double
)

data class ZonePetDto(
    val mascotaId: String,
    val nombre: String,
    val estado: String,
    val fotoUrl: String?,
    // ¡NUEVO! Lee la ubicación si existe, si es null no pasa nada
    val ubicacion: PointDto? = null
)

data class MarkersDto(
    val usuariosCompartidos: List<UserMarkerDto>,
    val desaparecidas: List<PetMarkerDto>
)

data class MapSnapshotResponse(
    val marcadores: MarkersDto,
    val zonas: List<ZoneDto>
)

// =============================================================================
// DTOs PARA ZONAS SEGURAS (Geovallas)
// =============================================================================

// Este modelo sirve tanto para el Snapshot del mapa como para el detalle de la zona.
data class ZoneDto(
    val zonaId: Int,
    val nombre: String? = null,
    val nombreZona: String? = null,
    val tipo: String? = null, // Nullable para mayor seguridad
    val centro: PointDto? = null,
    val radioMetros: Double? = null,
    val geometria: GeometryDto? = null,
    val estaActiva: Boolean? = null,
    val mascotas: List<ZonePetDto>? = null,
    val mascotaIds: List<String>? = null
)

// --- NUEVOS DTOs PARA GET /geofencing/zones (Snake Case) ---

data class ZonePetDetailDto(
    @SerializedName("mascota_id") val mascotaId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("tipo_mascota") val tipoMascota: String
)

data class ZoneWithPetsDto(
    @SerializedName("zona_id") val zonaId: Int,
    @SerializedName("nombre_zona") val nombreZona: String?,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("radio_metros") val radioMetros: Double?,
    @SerializedName("esta_activa") val estaActiva: Boolean?,
    @SerializedName("centro_lat") val centroLat: Double?,
    @SerializedName("centro_lng") val centroLng: Double?,
    @SerializedName("mascotas") val mascotas: List<ZonePetDetailDto>? = emptyList()
)

// --- DTOs PARA ENVIAR Y ACTUALIZAR DATOS ---

// DTO para ENVIAR una nueva zona al servidor
data class CreateZoneRequest(
    val nombreZona: String,
    val tipo: String, // "circulo" o "poligono"
    val lat: Double? = null,
    val lng: Double? = null,
    val radioMetros: Double? = null,
    val coordenadas: List<PointDto>? = null,
    val mascotaIds: List<String>? = null
)

// DTO para ACTUALIZAR una zona existente
data class UpdateZoneRequest(
    val nombreZona: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val radioMetros: Double? = null,
    val coordenadas: List<PointDto>? = null,
    val estaActiva: Boolean? = null
)

data class DeleteZoneResponse(
    val message: String
)

// =============================================================================
// INTERFAZ DE LA API (Retrofit)
// =============================================================================

interface GeofencingApi {

    // -------------------------------------------------------------------------
    // 1. MAPA PRINCIPAL
    // -------------------------------------------------------------------------
    @GET("map/snapshot")
    suspend fun getMapSnapshot(): Response<MapSnapshotResponse>

    // -------------------------------------------------------------------------
    // 2. GESTIÓN DE GEOVALLAS (Zonas Seguras)
    // -------------------------------------------------------------------------

    // Crear una nueva zona para una mascota
    @POST("geofencing/pets/{petId}/zones")
    suspend fun createZone(
        @Path("petId") petId: String,
        @Body request: CreateZoneRequest
    ): Response<ZoneDto>

    // Listar todas las zonas de una mascota específica
    @GET("geofencing/pets/{petId}/zones")
    suspend fun getPetZones(
        @Path("petId") petId: String
    ): Response<List<ZoneDto>>

    // ¡NUEVO ENDPOINT GLOBAL PARA LA PANTALLA DE ZONAS!
    @GET("geofencing/zones")
    suspend fun getAllUserZones(): Response<List<ZoneWithPetsDto>>

    // Obtener el detalle de una zona
    @GET("geofencing/zones/{id}")
    suspend fun getZoneDetail(
        @Path("id") zonaId: Int
    ): Response<ZoneDto>

    // Actualizar una zona (renombrar, cambiar radio, editar vértices, encender/apagar)
    @PUT("geofencing/zones/{id}")
    suspend fun updateZone(
        @Path("id") zonaId: Int,
        @Body request: UpdateZoneRequest
    ): Response<Any> // Usamos Any porque solo nos importa si es 200 OK

    // Eliminar la geovalla
    @DELETE("geofencing/zones/{id}")
    suspend fun deleteZone(
        @Path("id") zonaId: Int
    ): Response<DeleteZoneResponse>
}