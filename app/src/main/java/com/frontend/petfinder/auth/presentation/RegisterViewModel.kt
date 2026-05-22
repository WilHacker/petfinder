package com.frontend.petfinder.auth.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.auth.data.AuthRepository
import com.frontend.petfinder.auth.data.MedioContactoDto
import com.frontend.petfinder.auth.data.RegisterRequest
import com.frontend.petfinder.core.network.SocketManager
import com.frontend.petfinder.core.network.toPrismaMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class RegisterViewModel : ViewModel() {

    private val _nombre = MutableStateFlow("")
    val nombre: StateFlow<String> = _nombre.asStateFlow()

    private val _apellidoPaterno = MutableStateFlow("")
    val apellidoPaterno: StateFlow<String> = _apellidoPaterno.asStateFlow()

    private val _apellidoMaterno = MutableStateFlow("")
    val apellidoMaterno: StateFlow<String> = _apellidoMaterno.asStateFlow()

    private val _ci = MutableStateFlow("")
    val ci: StateFlow<String> = _ci.asStateFlow()

    private val _correo = MutableStateFlow("")
    val correo: StateFlow<String> = _correo.asStateFlow()

    private val _clave = MutableStateFlow("")
    val clave: StateFlow<String> = _clave.asStateFlow()

    private val _telefono = MutableStateFlow("")
    val telefono: StateFlow<String> = _telefono.asStateFlow()

    fun onNombreChange(v: String) { _nombre.value = v }
    fun onApellidoPaternoChange(v: String) { _apellidoPaterno.value = v }
    fun onApellidoMaternoChange(v: String) { _apellidoMaterno.value = v }
    fun onCiChange(v: String) { _ci.value = v }
    fun onCorreoChange(v: String) { _correo.value = v }
    fun onClaveChange(v: String) { _clave.value = v }
    fun onTelefonoChange(v: String) { _telefono.value = v }

    sealed class RegisterState {
        object Idle : RegisterState()
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
    }

    private val _uiState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val uiState: StateFlow<RegisterState> = _uiState.asStateFlow()

    fun registerOwner(context: Context) {
        if (_nombre.value.isBlank() || _ci.value.isBlank() || _correo.value.isBlank() || _clave.value.isBlank()) {
            _uiState.value = RegisterState.Error("Faltan campos obligatorios.")
            return
        }
        viewModelScope.launch {
            _uiState.value = RegisterState.Loading
            val request = RegisterRequest(
                nombre = _nombre.value,
                apellidoPaterno = _apellidoPaterno.value,
                apellidoMaterno = _apellidoMaterno.value,
                ci = _ci.value,
                correoElectronico = _correo.value,
                clave = _clave.value,
                medioContacto = MedioContactoDto(tipo = "WhatsApp", valor = _telefono.value)
            )
            AuthRepository.register(request).fold(
                onSuccess = { body ->
                    PetFinderApp.sessionManager.saveSession(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        userId = body.usuario.usuarioId,
                        rol = body.usuario.rol,
                        nombre = body.usuario.nombre
                    )
                    SocketManager.connect(body.accessToken, context)
                    AuthRepository.registrarFcmToken()
                    _uiState.value = RegisterState.Success
                },
                onFailure = { e ->
                    val code = (e as? HttpException)?.code() ?: -1
                    _uiState.value = RegisterState.Error(
                        e.toPrismaMessage() ?: when (code) {
                            409 -> "Este correo ya está registrado."
                            400 -> "Datos inválidos. Revisa el formulario."
                            else -> "Error de red. Verifica tu conexión."
                        }
                    )
                }
            )
        }
    }
}
