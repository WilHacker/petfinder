package com.frontend.petfinder.sightings.data

import com.frontend.petfinder.core.network.ApiServices
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

object SightingsRepository {

    private val textPlain = "text/plain".toMediaType()

    // ── Avistamientos ─────────────────────────────────────────────────────────

    suspend fun getSightings(petId: String): Result<List<SightingDto>> = runCatching {
        val r = ApiServices.sightings.getSightings(petId)
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
    }

    suspend fun reportSighting(
        petId: String,
        lat: Double,
        lng: Double,
        mensajeRescatista: String? = null,
        foto: MultipartBody.Part? = null
    ): Result<SightingDto> = runCatching {
        val latBody = lat.toString().toRequestBody(textPlain)
        val lngBody = lng.toString().toRequestBody(textPlain)
        val mensajeBody = mensajeRescatista?.takeIf { it.isNotBlank() }?.toRequestBody(textPlain)
        val r = ApiServices.sightings.reportSighting(petId, latBody, lngBody, mensajeBody, foto)
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }

    // ── Agradecimientos (legacy) ──────────────────────────────────────────────

    suspend fun getThanks(avistamientoId: String): Result<List<ThanksDto>> = runCatching {
        val r = ApiServices.sightings.getThanks(avistamientoId)
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
    }

    suspend fun sendThanks(avistamientoId: String, mensaje: String?): Result<ThanksDto> = runCatching {
        val r = ApiServices.sightings.sendThanks(avistamientoId, CreateThanksRequest(mensaje))
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }
}
