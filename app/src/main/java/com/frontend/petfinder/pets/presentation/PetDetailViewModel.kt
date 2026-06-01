package com.frontend.petfinder.pets.presentation

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.pets.data.PetRepository
import com.frontend.petfinder.pets.data.dto.PetDetailDto
import com.frontend.petfinder.pets.data.dto.PetReportDto
import com.frontend.petfinder.pets.data.dto.PetScanDto
import com.frontend.petfinder.core.network.SocketManager
import com.frontend.petfinder.sightings.data.SightingDto
import com.frontend.petfinder.sightings.data.SightingsRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import retrofit2.HttpException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

private const val TAG = "PetDetailViewModel"

class PetDetailViewModel : ViewModel() {

    init { observeSightingEvents() }

    sealed class DetailState {
        object Loading : DetailState()
        data class Success(val pet: PetDetailDto) : DetailState()
        data class Error(val message: String) : DetailState()
    }

    private val _state = MutableStateFlow<DetailState>(DetailState.Loading)
    val state: StateFlow<DetailState> = _state.asStateFlow()

    private val _qrBase64 = MutableStateFlow<String?>(null)
    val qrBase64: StateFlow<String?> = _qrBase64.asStateFlow()

    private val _qrError = MutableStateFlow<String?>(null)
    val qrError: StateFlow<String?> = _qrError.asStateFlow()

    private val _statusChanging = MutableStateFlow(false)
    val statusChanging: StateFlow<Boolean> = _statusChanging.asStateFlow()

    private val _scans = MutableStateFlow<List<PetScanDto>>(emptyList())
    val scans: StateFlow<List<PetScanDto>> = _scans.asStateFlow()

    private val _reports = MutableStateFlow<List<PetReportDto>>(emptyList())
    val reports: StateFlow<List<PetReportDto>> = _reports.asStateFlow()

    private val _locationUpdating = MutableStateFlow(false)
    val locationUpdating: StateFlow<Boolean> = _locationUpdating.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    private val _qrDownloading = MutableStateFlow(false)
    val qrDownloading: StateFlow<Boolean> = _qrDownloading.asStateFlow()

    private val _qrDownloadResult = MutableStateFlow<String?>(null)
    val qrDownloadResult: StateFlow<String?> = _qrDownloadResult.asStateFlow()

    fun load(petId: String) {
        viewModelScope.launch {
            _state.value = DetailState.Loading

            val detailJob   = async { PetRepository.getPetDetail(petId) }
            val scansJob    = async { PetRepository.getPetScans(petId) }
            val reportsJob  = async { PetRepository.getPetReports(petId) }
            val sightingJob = async { SightingsRepository.getSightings(petId) }

            detailJob.await().fold(
                onSuccess = { _state.value = DetailState.Success(it) },
                onFailure = { _state.value = DetailState.Error("No se pudo cargar la información de la mascota.") }
            )
            scansJob.await().onSuccess    { _scans.value = it }
            reportsJob.await().onSuccess  { _reports.value = it }
            sightingJob.await().onSuccess { _sightings.value = it }
        }
    }

    fun reportSighting(
        petId: String,
        lat: Double,
        lng: Double,
        mensajeRescatista: String? = null,
        foto: okhttp3.MultipartBody.Part? = null
    ) {
        viewModelScope.launch {
            _sightingSubmitting.value = true
            SightingsRepository.reportSighting(petId, lat, lng, mensajeRescatista, foto).fold(
                onSuccess = {
                    _sightings.value = listOf(it) + _sightings.value
                    _sightingError.value = null
                },
                onFailure = { _sightingError.value = "No se pudo publicar el avistamiento" }
            )
            _sightingSubmitting.value = false
        }
    }

    fun sendThanks(avistamientoId: String, mensaje: String?) {
        viewModelScope.launch {
            SightingsRepository.sendThanks(avistamientoId, mensaje).fold(
                onSuccess = { _sightingError.value = null },
                onFailure = { _sightingError.value = "No se pudo enviar el agradecimiento" }
            )
        }
    }

    fun clearSightingError() { _sightingError.value = null }

    fun updateLocation(petId: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            _locationUpdating.value = true
            _locationError.value = null
            PetRepository.updatePetLocation(petId, lat, lng).fold(
                onSuccess = {},
                onFailure = { e ->
                    _locationError.value = "No se pudo actualizar la ubicación."
                    Log.w(TAG, "updateLocation: ${e.message}")
                }
            )
            _locationUpdating.value = false
        }
    }

    fun clearLocationError() {
        _locationError.value = null
    }

    fun updateStatus(petId: String, estado: String) {
        viewModelScope.launch {
            _statusChanging.value = true
            PetRepository.updatePetStatus(petId, estado).onSuccess { load(petId) }
            _statusChanging.value = false
        }
    }

    fun loadQr(petId: String) {
        viewModelScope.launch {
            _qrBase64.value = null
            _qrError.value = null
            PetRepository.getPetQrCode(petId).fold(
                onSuccess = { _qrBase64.value = it },
                onFailure = { _qrError.value = "No se pudo cargar el código QR." }
            )
        }
    }

    fun clearQr() {
        _qrBase64.value = null
        _qrError.value = null
    }

    private val _sightings = MutableStateFlow<List<SightingDto>>(emptyList())
    val sightings: StateFlow<List<SightingDto>> = _sightings.asStateFlow()

    private val _sightingError = MutableStateFlow<String?>(null)
    val sightingError: StateFlow<String?> = _sightingError.asStateFlow()

    private val _sightingSubmitting = MutableStateFlow(false)
    val sightingSubmitting: StateFlow<Boolean> = _sightingSubmitting.asStateFlow()

    private val _ownerError = MutableStateFlow<String?>(null)
    val ownerError: StateFlow<String?> = _ownerError.asStateFlow()

    private val _ownerLoading = MutableStateFlow(false)
    val ownerLoading: StateFlow<Boolean> = _ownerLoading.asStateFlow()

    fun addOwner(petId: String, correoElectronico: String, tipoRelacion: String = "Cuidador") {
        if (correoElectronico.isBlank()) { _ownerError.value = "Ingresa el correo electrónico"; return }
        viewModelScope.launch {
            _ownerLoading.value = true
            PetRepository.addOwner(petId, correoElectronico.trim(), tipoRelacion).fold(
                onSuccess = { load(petId) },
                onFailure = { e -> _ownerError.value = "No se pudo agregar: ${e.message?.take(60)}" }
            )
            _ownerLoading.value = false
        }
    }

    fun removeOwner(petId: String, personaId: String) {
        viewModelScope.launch {
            _ownerLoading.value = true
            PetRepository.removeOwner(petId, personaId).fold(
                onSuccess = { load(petId) },
                onFailure = { _ownerError.value = "No se pudo quitar al cuidador" }
            )
            _ownerLoading.value = false
        }
    }

    fun clearOwnerError() { _ownerError.value = null }

    fun downloadQr(context: Context, petId: String, petName: String) {
        viewModelScope.launch {
            _qrDownloading.value = true
            _qrDownloadResult.value = null
            PetRepository.getPetQrCode(petId, size = 800).fold(
                onSuccess = { base64 ->
                    val saved = withContext(Dispatchers.IO) {
                        saveBase64ImageToGallery(context, base64, "QR_${petName.replace(" ", "_")}")
                    }
                    _qrDownloadResult.value = if (saved) "QR guardado en la galería" else "No se pudo guardar el QR"
                },
                onFailure = { _qrDownloadResult.value = "No se pudo generar el QR de alta resolución" }
            )
            _qrDownloading.value = false
        }
    }

    fun clearQrDownloadResult() {
        _qrDownloadResult.value = null
    }

    private val _communityAlertSending = MutableStateFlow(false)
    val communityAlertSending: StateFlow<Boolean> = _communityAlertSending.asStateFlow()

    private val _communityAlertResult = MutableStateFlow<String?>(null)
    val communityAlertResult: StateFlow<String?> = _communityAlertResult.asStateFlow()

    // Dialog para razon/errores con texto largo
    data class CommunityAlertDialog(val title: String, val message: String)
    private val _communityAlertDialog = MutableStateFlow<CommunityAlertDialog?>(null)
    val communityAlertDialog: StateFlow<CommunityAlertDialog?> = _communityAlertDialog.asStateFlow()

    fun sendCommunityAlert(petId: String, radio: Int? = null) {
        viewModelScope.launch {
            _communityAlertSending.value = true
            PetRepository.sendCommunityAlert(petId, radio).fold(
                onSuccess = { resp ->
                    if ((resp.alertados ?: 0) > 0) {
                        _communityAlertResult.value = resp.mensaje ?: "Alerta enviada a ${resp.alertados} usuario(s) cercano(s)"
                    } else {
                        _communityAlertDialog.value = CommunityAlertDialog(
                            title = resp.mensaje ?: "Sin usuarios notificados",
                            message = resp.razon ?: "No hubo usuarios cercanos para notificar."
                        )
                    }
                },
                onFailure = { e ->
                    val serverMsg = (e as? HttpException)?.let { httpEx ->
                        runCatching {
                            val body = httpEx.response()?.errorBody()?.string()
                            val json = JSONObject(body ?: "")
                            val raw = json.opt("message")
                            when {
                                raw is String && raw.isNotBlank() -> raw
                                raw is org.json.JSONArray && raw.length() > 0 ->
                                    (0 until raw.length()).joinToString("\n") { raw.getString(it) }
                                else -> null
                            }
                        }.getOrNull()
                    }
                    val msg = serverMsg ?: "No se pudo enviar la alerta comunitaria."
                    _communityAlertDialog.value = CommunityAlertDialog(title = "Aviso", message = msg)
                }
            )
            _communityAlertSending.value = false
        }
    }

    fun clearCommunityAlertDialog() { _communityAlertDialog.value = null }

    fun clearCommunityAlertResult() { _communityAlertResult.value = null }

    private val _rewardUpdating = MutableStateFlow(false)
    val rewardUpdating: StateFlow<Boolean> = _rewardUpdating.asStateFlow()

    private val _rewardResult = MutableStateFlow<String?>(null)
    val rewardResult: StateFlow<String?> = _rewardResult.asStateFlow()

    fun updateReward(petId: String, recompensa: Double) {
        viewModelScope.launch {
            _rewardUpdating.value = true
            PetRepository.updateReward(petId, recompensa).fold(
                onSuccess = {
                    _rewardResult.value = if (recompensa > 0)
                        "Recompensa actualizada: Bs. %.0f".format(recompensa)
                    else
                        "Recompensa eliminada"
                    load(petId)
                },
                onFailure = { e ->
                    val msg = when {
                        e.message?.contains("400") == true -> "La mascota no tiene un reporte activo"
                        e.message?.contains("403") == true -> "Sin permiso para modificar la recompensa"
                        else -> "No se pudo actualizar la recompensa"
                    }
                    _rewardResult.value = msg
                }
            )
            _rewardUpdating.value = false
        }
    }

    fun clearRewardResult() { _rewardResult.value = null }


    private fun observeSightingEvents() {
        viewModelScope.launch {
            SocketManager.sightingNewFlow.collect { event ->
                val nuevo = SightingDto(
                    avistamientoId = event.avistamientoId,
                    mascotaId = "",
                    mensajeRescatista = event.mensaje,
                    fotoEvidenciaUrl = event.fotoUrl,
                    fechaAvistamiento = event.fechaAvistamiento,
                    lat = event.lat,
                    lng = event.lng
                )
                _sightings.update { current ->
                    if (current.any { it.avistamientoId == event.avistamientoId }) current
                    else listOf(nuevo) + current
                }
            }
        }
    }

    private fun saveBase64ImageToGallery(context: Context, base64: String, fileName: String): Boolean {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PetFinder")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
                context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PetFinder")
                dir.mkdirs()
                val file = File(dir, "$fileName.png")
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "saveBase64ImageToGallery: ${e.message}")
            false
        }
    }
}
