package com.frontend.petfinder.geofencing.data

import com.frontend.petfinder.core.network.ApiServices
import com.frontend.petfinder.core.network.PrismaException
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response

object GeofencingRepository {

    /** Extrae el `message` legible del cuerpo de error del backend (NestJS). */
    private fun backendMessage(response: Response<*>, fallback: String): String =
        runCatching {
            val body = response.errorBody()?.string().orEmpty()
            when (val msg = JSONObject(body).opt("message")) {
                is String -> msg.ifBlank { fallback }
                is org.json.JSONArray -> (0 until msg.length())
                    .joinToString(" ") { msg.getString(it) }
                    .ifBlank { fallback }
                else -> fallback
            }
        }.getOrDefault(fallback)

    suspend fun getMapSnapshot(tipoId: Int? = null): Result<MapSnapshotResponse> = runCatching {
        val r = ApiServices.geo.getMapSnapshot(tipoId)
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }

    // Card de detalle de una mascota (propia / compartida / comunidad).
    // 404 = ajena y no extraviada (no consultable) → mensaje amigable del backend.
    suspend fun getMapPetCard(mascotaId: String): Result<MapPetCardDto> = runCatching {
        val r = ApiServices.geo.getMapPetCard(mascotaId)
        if (r.isSuccessful) r.body()!!
        else throw PrismaException(
            backendMessage(r, "No se pudo cargar la información de la mascota."),
            code = r.code().toString()
        )
    }

    // Card de detalle de un colaborador. 403 = no compartes ninguna mascota con esa persona.
    suspend fun getMapCollaboratorCard(personaId: String): Result<MapCollaboratorCardDto> = runCatching {
        val r = ApiServices.geo.getMapCollaboratorCard(personaId)
        if (r.isSuccessful) r.body()!!
        else throw PrismaException(
            backendMessage(r, "No se pudo cargar la información del colaborador."),
            code = r.code().toString()
        )
    }

    suspend fun getPublicLostPets(tipoId: Int? = null): Result<List<LostPetMarkerDto>> = runCatching {
        val r = ApiServices.geo.getPublicLostPets(tipoId)
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
    }

    suspend fun getAllUserZones(): Result<List<ZoneWithPetsDto>> = runCatching {
        val r = ApiServices.geo.getAllUserZones()
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
    }

    suspend fun getPetZones(petId: String): Result<List<ZoneDto>> = runCatching {
        val r = ApiServices.geo.getPetZones(petId)
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
    }

    suspend fun createZone(petId: String, request: CreateZoneRequest): Result<ZoneDto> = runCatching {
        val r = ApiServices.geo.createZone(petId, request)
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }

    suspend fun updateZone(zonaId: Int, request: UpdateZoneRequest): Result<Unit> = runCatching {
        val r = ApiServices.geo.updateZone(zonaId, request)
        if (r.isSuccessful) Unit else throw HttpException(r)
    }

    suspend fun deleteZone(zonaId: Int): Result<Unit> = runCatching {
        val r = ApiServices.geo.deleteZone(zonaId)
        if (r.isSuccessful) Unit else throw HttpException(r)
    }

    suspend fun addPetsToZone(zonaId: Int, request: ZonePetsRequest): Result<Unit> = runCatching {
        val r = ApiServices.geo.addPetsToZone(zonaId, request)
        if (r.isSuccessful) Unit else throw HttpException(r)
    }

    suspend fun removePetsFromZone(zonaId: Int, request: ZonePetsRequest): Result<Unit> = runCatching {
        val r = ApiServices.geo.removePetsFromZone(zonaId, request)
        if (r.isSuccessful) Unit else throw HttpException(r)
    }
}
