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

    // ── Comentarios ───────────────────────────────────────────────────────────

    suspend fun getComments(avistamientoId: String): Result<List<SightingCommentDto>> = runCatching {
        val r = ApiServices.sightings.getComments(avistamientoId)
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
    }

    suspend fun addComment(
        avistamientoId: String,
        mensaje: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        replyToUserId: String? = null,
        foto: MultipartBody.Part? = null
    ): Result<SightingCommentDto> = runCatching {
        val mensajeBody = mensaje?.takeIf { it.isNotBlank() }?.toRequestBody(textPlain)
        val latBody = lat?.toString()?.toRequestBody(textPlain)
        val lngBody = lng?.toString()?.toRequestBody(textPlain)
        val replyBody = replyToUserId?.toRequestBody(textPlain)
        val r = ApiServices.sightings.addComment(avistamientoId, mensajeBody, latBody, lngBody, replyBody, foto)
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }

    // ── No leídos ─────────────────────────────────────────────────────────────

    suspend fun markAsRead(avistamientoId: String): Result<Unit> = runCatching {
        val r = ApiServices.sightings.markAsRead(avistamientoId)
        if (!r.isSuccessful) throw HttpException(r)
    }

    suspend fun getUnreadCount(): Result<UnreadCountDto> = runCatching {
        val r = ApiServices.sightings.getUnreadCount()
        if (r.isSuccessful) r.body() ?: UnreadCountDto(0, 0, 0) else throw HttpException(r)
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    suspend fun getMyPetsThreads(): Result<List<SightingThreadDto>> = runCatching {
        val r = ApiServices.sightings.getMyPetsThreads()
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
    }

    suspend fun getMyParticipations(): Result<List<MyParticipationDto>> = runCatching {
        val r = ApiServices.sightings.getMyParticipations()
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
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
