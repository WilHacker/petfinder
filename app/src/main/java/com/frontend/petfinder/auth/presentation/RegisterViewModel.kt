package com.frontend.petfinder.auth.presentation

import android.content.Context
import android.util.Patterns
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

    // Errores por campo
    private val _nombreError = MutableStateFlow<String?>(null)
    val nombreError: StateFlow<String?> = _nombreError.asStateFlow()

    private val _apellidoPaternoError = MutableStateFlow<String?>(null)
    val apellidoPaternoError: StateFlow<String?> = _apellidoPaternoError.asStateFlow()

    private val _ciError = MutableStateFlow<String?>(null)
    val ciError: StateFlow<String?> = _ciError.asStateFlow()

    private val _correoError = MutableStateFlow<String?>(null)
    val correoError: StateFlow<String?> = _correoError.asStateFlow()

    private val _claveError = MutableStateFlow<String?>(null)
    val claveError: StateFlow<String?> = _claveError.asStateFlow()

    private val _telefonoError = MutableStateFlow<String?>(null)
    val telefonoError: StateFlow<String?> = _telefonoError.asStateFlow()

    fun onNombreChange(v: String) { _nombre.value = v; _nombreError.value = null }
    fun onApellidoPaternoChange(v: String) { _apellidoPaterno.value = v; _apellidoPaternoError.value = null }
    fun onApellidoMaternoChange(v: String) { _apellidoMaterno.value = v }
    fun onCiChange(v: String) { _ci.value = v.filter { it.isDigit() }; _ciError.value = null }
    fun onCorreoChange(v: String) { _correo.value = v.filter { it != ' ' }.lowercase(); _correoError.value = null }
    fun onClaveChange(v: String) { _clave.value = v; _claveError.value = null }
    fun onTelefonoChange(v: String) { _telefono.value = v.filter { it.isDigit() }; _telefonoError.value = null }

    sealed class RegisterState {
        object Idle : RegisterState()
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
    }

    private val _uiState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val uiState: StateFlow<RegisterState> = _uiState.asStateFlow()

    fun registerOwner(context: Context) {
        var hasError = false

        if (_nombre.value.trim().isBlank()) {
            _nombreError.value = "El nombre es obligatorio"
            hasError = true
        }
        if (_apellidoPaterno.value.trim().isBlank()) {
            _apellidoPaternoError.value = "El apellido paterno es obligatorio"
            hasError = true
        }
        if (_ci.value.isBlank()) {
            _ciError.value = "La cédula de identidad es obligatoria"
            hasError = true
        }
        if (_correo.value.isBlank()) {
            _correoError.value = "El correo es obligatorio"
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(_correo.value).matches()) {
            _correoError.value = "Ingresa un correo válido"
            hasError = true
        }
        if (_clave.value.length < 6) {
            _claveError.value = if (_clave.value.isBlank()) "La contraseña es obligatoria"
                else "Mínimo 6 caracteres"
            hasError = true
        }
        if (_telefono.value.isNotBlank() && _telefono.value.length < 7) {
            _telefonoError.value = "Ingresa un número válido (mínimo 7 dígitos)"
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.value = RegisterState.Loading
            val request = RegisterRequest(
                nombre = _nombre.value.trim(),
                apellidoPaterno = _apellidoPaterno.value.trim(),
                apellidoMaterno = _apellidoMaterno.value.trim().ifBlank { "" },
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
