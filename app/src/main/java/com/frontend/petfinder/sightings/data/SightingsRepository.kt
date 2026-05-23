package com.frontend.petfinder.sightings.data

import com.frontend.petfinder.core.network.ApiServices
import retrofit2.HttpException

object SightingsRepository {

    suspend fun getSightings(petId: String): Result<List<SightingDto>> = runCatching {
        val r = ApiServices.sightings.getSightings(petId)
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
    }

    suspend fun reportSighting(petId: String, request: CreateSightingRequest): Result<SightingDto> = runCatching {
        val r = ApiServices.sightings.reportSighting(petId, request)
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }

    suspend fun getThanks(avistamientoId: Int): Result<List<ThanksDto>> = runCatching {
        val r = ApiServices.sightings.getThanks(avistamientoId)
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
    }

    suspend fun sendThanks(avistamientoId: Int, mensaje: String?): Result<ThanksDto> = runCatching {
        val r = ApiServices.sightings.sendThanks(avistamientoId, CreateThanksRequest(mensaje))
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }
}
