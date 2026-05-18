package com.frontend.petfinder.pets.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.pets.data.PetApi
import com.frontend.petfinder.pets.data.dto.PetListItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyPetsViewModel : ViewModel() {
    private val petApi = RetrofitClient.instance.create(PetApi::class.java)

    private val _pets = MutableStateFlow<List<PetListItemDto>>(emptyList())
    val pets: StateFlow<List<PetListItemDto>> = _pets.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Estado para el QR y para el mensaje de error específico
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
            try {
                val response = petApi.getMyPets()
                if (response.isSuccessful) {
                    _pets.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                println("Error cargando mascotas: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPetQr(petId: String) {
        viewModelScope.launch {
            _selectedQrBase64.value = null
            _qrErrorMessage.value = null // Limpiamos errores previos

            try {
                val response = petApi.getPetQrCode(petId)

                if (response.isSuccessful) {
                    // ¡CAMBIO AQUÍ! Usamos .string() sobre el ResponseBody para extraer el texto crudo
                    val rawQr = response.body()?.string() ?: ""
                    _selectedQrBase64.value = rawQr.replace("\"", "")
                } else {
                    // Capturamos el error según el código HTTP
                    _qrErrorMessage.value = when(response.code()) {
                        401 -> "Sesión expirada. Por favor, vuelve a iniciar sesión."
                        404 -> "No se encontró la placa QR para esta mascota."
                        500 -> "Error interno del servidor. Inténtalo más tarde."
                        else -> "Error del servidor: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                // Error de conexión (offline, servidor caído, etc.)
                println("Fallo al descargar QR: ${e.message}")
                _qrErrorMessage.value = "No se pudo conectar con el servidor o procesar la imagen."
            }
        }
    }

    fun clearSelectedQr() {
        _selectedQrBase64.value = null
        _qrErrorMessage.value = null
    }
}