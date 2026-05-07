package com.frontend.petfinder.pets.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.pets.data.PetApi
import com.frontend.petfinder.pets.data.PetListItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyPetsViewModel : ViewModel() {

    private val _pets = MutableStateFlow<List<PetListItemDto>>(emptyList())
    val pets: StateFlow<List<PetListItemDto>> = _pets.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Nueva variable para manejar errores de conexión en la pantalla
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadMyPets() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val api = RetrofitClient.instance.create(PetApi::class.java)
                val response = api.getMyPets()

                if (response.isSuccessful) {
                    response.body()?.let { _pets.value = it }
                } else {
                    _errorMessage.value = "Error al cargar tu lista (Código: ${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Verifica tu conexión a internet."
                println("Error cargando mascotas: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}