package com.frontend.petfinder.pets.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.pets.data.PetRepository
import com.frontend.petfinder.pets.data.dto.PetListItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

private const val TAG = "MyPetsViewModel"

class MyPetsViewModel : ViewModel() {

    private val _pets = MutableStateFlow<List<PetListItemDto>>(emptyList())
    val pets: StateFlow<List<PetListItemDto>> = _pets.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedQrBase64 = MutableStateFlow<String?>(null)
    val selectedQrBase64: StateFlow<String?> = _selectedQrBase64.asStateFlow()

    private val _qrErrorMessage = MutableStateFlow<String?>(null)
    val qrErrorMessage: StateFlow<String?> = _qrErrorMessage.asStateFlow()

    init {
        loadMyPets()
    }

    fun loadMyPets() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            PetRepository.getMyPets().fold(
                onSuccess = { _pets.value = it },
                onFailure = { e ->
                    Log.w(TAG, "loadMyPets: ${e.message}")
                    if (_pets.value.isEmpty()) {
                        _error.value = "No se pudo cargar tus mascotas. Verifica tu conexión."
                    }
                }
            )
            _isLoading.value = false
        }
    }

    fun clearError() { _error.value = null }

    fun loadPetQr(petId: String) {
        viewModelScope.launch {
            _selectedQrBase64.value = null
            _qrErrorMessage.value = null
            PetRepository.getPetQrCode(petId).fold(
                onSuccess = { _selectedQrBase64.value = it },
                onFailure = { e ->
                    val code = (e as? HttpException)?.code() ?: -1
                    _qrErrorMessage.value = when (code) {
                        401 -> "Sesión expirada. Por favor, vuelve a iniciar sesión."
                        404 -> "No se encontró la placa QR para esta mascota."
                        500 -> "Error interno del servidor. Inténtalo más tarde."
                        else -> "No se pudo conectar con el servidor o procesar la imagen."
                    }
                }
            )
        }
    }

    fun clearSelectedQr() {
        _selectedQrBase64.value = null
        _qrErrorMessage.value = null
    }
}
