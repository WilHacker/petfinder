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

    suspend fun getPetQrCode(petId: String): Result<String> = runCatching {
        val r = ApiServices.pets.getPetQrCode(petId)
        if (r.isSuccessful) r.body()?.string()?.replace("\"", "")
            ?: throw IllegalStateException("QR body vacío")
        else throw HttpException(r)
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

    suspend fun updatePetStatus(petId: String, estado: String): Result<Unit> = runCatching {
        val r = ApiServices.pets.updatePetStatus(petId, UpdateStatusRequest(estado))
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
}
