package com.frontend.petfinder.pets.data

import com.frontend.petfinder.core.network.ApiServices
import com.frontend.petfinder.pets.data.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException



object PetRepository {

    suspend fun getMyPets(): Result<List<PetListItemDto>> = runCatching {
        val r = ApiServices.pets.getMyPets()
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
    }

    suspend fun getPetQrCode(petId: String, size: Int? = null): Result<String> = runCatching {
        val r = ApiServices.pets.getPetQrCode(petId, size)
        if (r.isSuccessful) {
            val raw = r.body()?.string()?.trim()?.replace("\"", "")
                ?: throw IllegalStateException("QR body vacío")
            // Strip data URI prefix if server returns "data:image/...;base64,<data>"
            if (raw.contains("base64,")) raw.substringAfter("base64,").trim() else raw
        } else throw HttpException(r)
    }

    suspend fun getPublicPetCard(token: String): Result<PublicPetCardDto> = runCatching {
        val r = ApiServices.pets.getPublicPetCard(token)
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }

    suspend fun registerQrScan(token: String, request: QrScanRequest): Result<Unit> = runCatching {
        val r = ApiServices.pets.registerQrScan(token, request)
        if (r.isSuccessful) Unit else throw HttpException(r)
    }

    suspend fun getPetDetail(petId: String): Result<PetDetailDto> = runCatching {
        val r = ApiServices.pets.getPetDetail(petId)
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }

    suspend fun getPetScans(petId: String): Result<List<PetScanDto>> = runCatching {
        val r = ApiServices.pets.getPetScans(petId)
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
    }

    suspend fun getPetReports(petId: String): Result<List<PetReportDto>> = runCatching {
        val r = ApiServices.pets.getPetReports(petId)
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
    }

    suspend fun updatePetStatus(petId: String, estado: String, recompensa: Double? = null): Result<Unit> = runCatching {
        val r = ApiServices.pets.updatePetStatus(petId, UpdateStatusRequest(estado, recompensa))
        if (r.isSuccessful) Unit else throw HttpException(r)
    }

    suspend fun updatePetLocation(petId: String, lat: Double, lng: Double): Result<Unit> = runCatching {
        val r = ApiServices.pets.updatePetLocation(petId, UpdateLocationRequest(lat, lng))
        if (r.isSuccessful) Unit else throw HttpException(r)
    }

    suspend fun getTiposMascota(): Result<List<TipoMascotaDto>> = runCatching {
        val r = ApiServices.pets.getTiposMascota()
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
    }

    suspend fun registerPet(
        nombre: RequestBody,
        tipoId: RequestBody?,
        sexo: RequestBody?,
        colorPrimario: RequestBody?,
        rasgosParticulares: RequestBody?,
        fotoPrincipalIndex: RequestBody?,
        fotos: List<MultipartBody.Part>?
    ): Result<RegisterPetResponse> = runCatching {
        val r = ApiServices.pets.registerPet(
            nombre, tipoId, sexo, colorPrimario, rasgosParticulares, fotoPrincipalIndex, fotos
        )
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }

    suspend fun getMedicalRecords(petId: String): Result<List<MedicalRecordDto>> = runCatching {
        val r = ApiServices.pets.getMedicalRecords(petId)
        if (r.isSuccessful) r.body() ?: emptyList() else throw HttpException(r)
    }

    suspend fun createMedicalRecord(petId: String, request: CreateMedicalRecordRequest): Result<Unit> = runCatching {
        val r = ApiServices.pets.createMedicalRecord(petId, request)
        if (r.isSuccessful) Unit else throw HttpException(r)
    }

    suspend fun updateMedicalRecord(petId: String, registroId: Int, request: UpdateMedicalRecordRequest): Result<Unit> = runCatching {
        val r = ApiServices.pets.updateMedicalRecord(petId, registroId, request)
        if (r.isSuccessful) Unit else throw HttpException(r)
    }

    suspend fun deleteMedicalRecord(petId: String, registroId: Int): Result<Unit> = runCatching {
        ApiServices.pets.deleteMedicalRecord(petId, registroId)
        Unit
    }

    suspend fun addOwner(petId: String, correoElectronico: String, tipoRelacion: String): Result<PetOwnerRelationDto> = runCatching {
        val r = ApiServices.pets.addPetOwner(petId, AddOwnerRequest(correoElectronico = correoElectronico, tipoRelacion = tipoRelacion))
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }

    suspend fun removeOwner(petId: String, personaId: String): Result<Unit> = runCatching {
        val r = ApiServices.pets.removePetOwner(petId, personaId)
        if (r.isSuccessful) Unit else throw HttpException(r)
    }

    suspend fun updateReward(petId: String, recompensa: Double): Result<UpdateRewardResponse> = runCatching {
        val r = ApiServices.pets.updateReward(petId, UpdateRewardRequest(recompensa))
        if (r.isSuccessful) r.body() ?: UpdateRewardResponse(petId, recompensa)
        else throw HttpException(r)
    }

    suspend fun sendCommunityAlert(petId: String, radio: Int? = null): Result<CommunityAlertResponse> = runCatching {
        val r = ApiServices.pets.sendCommunityAlert(petId, CommunityAlertRequest(radio))
        if (r.isSuccessful) r.body() ?: CommunityAlertResponse(mensaje = "Alerta comunitaria enviada")
        else throw HttpException(r)
    }
}
