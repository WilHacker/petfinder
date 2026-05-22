package com.frontend.petfinder.geofencing.data

import com.frontend.petfinder.core.network.ApiServices
import retrofit2.HttpException

object GeofencingRepository {

    suspend fun getMapSnapshot(): Result<MapSnapshotResponse> = runCatching {
        val r = ApiServices.geo.getMapSnapshot()
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }

    suspend fun getPublicLostPets(): Result<List<LostPetMarkerDto>> = runCatching {
        val r = ApiServices.geo.getPublicLostPets()
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
