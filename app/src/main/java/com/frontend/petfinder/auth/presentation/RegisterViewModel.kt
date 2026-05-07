package com.frontend.petfinder.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.auth.data.MedioContactoDto
import com.frontend.petfinder.auth.data.RegisterRequest
import com.frontend.petfinder.core.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    // Todos los campos del formulario
    var nombre = MutableStateFlow("")
    var apellidoPaterno = MutableStateFlow("")
    var apellidoMaterno = MutableStateFlow("")
    var ci = MutableStateFlow("")
    var correo = MutableStateFlow("")
    var clave = MutableStateFlow("")
    var telefono = MutableStateFlow("")

    sealed class RegisterState {
        object Idle : RegisterState()
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
    }

    private val _uiState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val uiState: StateFlow<RegisterState> = _uiState.asStateFlow()

    fun registerOwner() {
        if (nombre.value.isBlank() || ci.value.isBlank() || correo.value.isBlank() || clave.value.isBlank()) {
            _uiState.value = RegisterState.Error("Faltan campos obligatorios.")
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterState.Loading
            try {
                val api = RetrofitClient.instance.create(com.frontend.petfinder.auth.data.AuthApi::class.java)

                // Construimos el objeto anidado para el contacto
                val medioContacto = MedioContactoDto(
                    tipo = "WhatsApp", // Tipo por defecto según tu JSON
                    valor = telefono.value
                )

                // Ensamblamos el payload completo
                val request = RegisterRequest(
                    nombre = nombre.value,
                    apellidoPaterno = apellidoPaterno.value,
                    apellidoMaterno = apellidoMaterno.value,
                    ci = ci.value,
                    correoElectronico = correo.value,
                    clave = clave.value,
                    medioContacto = medioContacto
                )

                val response = api.registerOwner(request)

                if (response.isSuccessful) {
                    _uiState.value = RegisterState.Success
                } else {
                    _uiState.value = RegisterState.Error("Error en el registro: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = RegisterState.Error("Error de red. Verifica tu conexión.")
            }
        }
    }
}