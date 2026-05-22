package com.frontend.petfinder.pets.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.pets.data.PetRepository
import com.frontend.petfinder.pets.data.dto.PetDetailDto
import com.frontend.petfinder.pets.data.dto.PetReportDto
import com.frontend.petfinder.pets.data.dto.PetScanDto
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PetDetailViewModel"

class PetDetailViewModel : ViewModel() {

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

    fun load(petId: String) {
        viewModelScope.launch {
            _state.value = DetailState.Loading

            val detailJob  = async { PetRepository.getPetDetail(petId) }
            val scansJob   = async { PetRepository.getPetScans(petId) }
            val reportsJob = async { PetRepository.getPetReports(petId) }

            detailJob.await().fold(
                onSuccess = { _state.value = DetailState.Success(it) },
                onFailure = { _state.value = DetailState.Error("No se pudo cargar la información de la mascota.") }
            )
            scansJob.await().onSuccess   { _scans.value = it }
            reportsJob.await().onSuccess { _reports.value = it }
        }
    }

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
}
