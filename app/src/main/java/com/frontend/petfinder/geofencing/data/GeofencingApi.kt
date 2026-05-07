package com.frontend.petfinder.geofencing.data

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

data class GeometryDto(val type: String, val coordinates: List<List<List<Double>>>)

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
    val fotoUrl: String?
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
// Usamos valores nulos (?) por defecto para los campos que varían entre rutas.
data class ZoneDto(
    val zonaId: Int,
    val nombre: String? = null,       // Lo envía el endpoint del Snapshot
    val nombreZona: String? = null,   // Lo envía el endpoint de CRUD de Zonas
    val tipo: String,                 // "circulo" o "poligono"
    val centro: PointDto? = null,
    val radioMetros: Double? = null,
    val geometria: GeometryDto? = null,
    val estaActiva: Boolean? = null,
    val mascotas: List<ZonePetDto>? = null, // Mascotas en el Snapshot
    val mascotaIds: List<String>? = null    // IDs en el CRUD de Zonas
)

// DTO para ENVIAR una nueva zona al servidor
data class CreateZoneRequest(
    val nombreZona: String,
    val tipo: String, // "circulo" o "poligono"

    // Solo para círculos
    val lat: Double? = null,
    val lng: Double? = null,
    val radioMetros: Double? = null,

    // Solo para polígonos
    val coordenadas: List<PointDto>? = null,

    // Opcional: para asignar la misma zona a Firulais y a Migel a la vez
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

    // Obtener el detalle de una zona
    @GET("geofencing/zones/{id}")
    suspend fun getZoneDetail(
        @Path("id") zonaId: Int
    ): Response<ZoneDto>

    // Actualizar una zona (renombrar, cambiar radio, editar vértices)
    @PUT("geofencing/zones/{id}")
    suspend fun updateZone(
        @Path("id") zonaId: Int,
        @Body request: UpdateZoneRequest
    ): Response<ZoneDto>

    // Eliminar la geovalla
    @DELETE("geofencing/zones/{id}")
    suspend fun deleteZone(
        @Path("id") zonaId: Int
    ): Response<DeleteZoneResponse>
}