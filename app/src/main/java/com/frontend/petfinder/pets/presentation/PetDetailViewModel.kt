package com.frontend.petfinder.pets.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.pets.data.PetApi
import com.frontend.petfinder.pets.data.dto.PetDetailDto
import com.frontend.petfinder.pets.data.dto.PetReportDto
import com.frontend.petfinder.pets.data.dto.PetScanDto
import com.frontend.petfinder.pets.data.dto.UpdateLocationRequest
import com.frontend.petfinder.pets.data.dto.UpdateStatusRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PetDetailViewModel : ViewModel() {

    private val petApi = RetrofitClient.instance.create(PetApi::class.java)

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
            try {
                val response = petApi.getPetDetail(petId)
                if (response.isSuccessful) {
                    _state.value = DetailState.Success(response.body()!!)
                    loadScans(petId)
                    loadReports(petId)
                } else {
                    _state.value = DetailState.Error("No se pudo cargar la información de la mascota.")
                }
            } catch (e: Exception) {
                _state.value = DetailState.Error("Sin conexión. Verifica tu internet e intenta de nuevo.")
            }
        }
    }

    private fun loadScans(petId: String) {
        viewModelScope.launch {
            try {
                val r = petApi.getPetScans(petId)
                if (r.isSuccessful) _scans.value = r.body() ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    private fun loadReports(petId: String) {
        viewModelScope.launch {
            try {
                val r = petApi.getPetReports(petId)
                if (r.isSuccessful) _reports.value = r.body() ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun updateLocation(petId: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            _locationUpdating.value = true
            _locationError.value = null
            try {
                val response = petApi.updatePetLocation(petId, UpdateLocationRequest(lat, lng))
                if (!response.isSuccessful) {
                    _locationError.value = "No se pudo actualizar la ubicación (${response.code()})."
                }
            } catch (e: Exception) {
                _locationError.value = "Sin conexión. Verifica tu internet."
            } finally {
                _locationUpdating.value = false
            }
        }
    }

    fun clearLocationError() {
        _locationError.value = null
    }

    fun updateStatus(petId: String, estado: String) {
        viewModelScope.launch {
            _statusChanging.value = true
            try {
                val response = petApi.updatePetStatus(petId, UpdateStatusRequest(estado))
                if (response.isSuccessful) {
                    load(petId)
                }
            } catch (e: Exception) {
                // No derriba la pantalla — el estado visual se restaura solo
            } finally {
                _statusChanging.value = false
            }
        }
    }

    fun loadQr(petId: String) {
        viewModelScope.launch {
            _qrBase64.value = null
            _qrError.value = null
            try {
                val response = petApi.getPetQrCode(petId)
                if (response.isSuccessful) {
                    _qrBase64.value = response.body()?.string()?.replace("\"", "") ?: ""
                } else {
                    _qrError.value = "No se pudo cargar el código QR."
                }
            } catch (e: Exception) {
                _qrError.value = "Sin conexión. Verifica tu internet e intenta de nuevo."
            }
        }
    }

    fun clearQr() {
        _qrBase64.value = null
        _qrError.value = null
    }
}
