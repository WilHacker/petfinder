package com.frontend.petfinder.auth.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.auth.data.AuthApi
import com.frontend.petfinder.auth.data.MedioContactoDto
import com.frontend.petfinder.auth.data.RegisterRequest
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.core.network.SocketManager
import com.frontend.petfinder.profile.data.UserApi
import com.frontend.petfinder.profile.data.dto.FcmTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "RegisterViewModel"

class RegisterViewModel : ViewModel() {

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

    fun registerOwner(context: Context) {
        if (nombre.value.isBlank() || ci.value.isBlank() || correo.value.isBlank() || clave.value.isBlank()) {
            _uiState.value = RegisterState.Error("Faltan campos obligatorios.")
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterState.Loading
            try {
                val api = RetrofitClient.instance.create(AuthApi::class.java)
                val request = RegisterRequest(
                    nombre = nombre.value,
                    apellidoPaterno = apellidoPaterno.value,
                    apellidoMaterno = apellidoMaterno.value,
                    ci = ci.value,
                    correoElectronico = correo.value,
                    clave = clave.value,
                    medioContacto = MedioContactoDto(tipo = "WhatsApp", valor = telefono.value)
                )

                val response = api.registerOwner(request)

                if (response.isSuccessful) {
                    val body = response.body()!!

                    // Persistir sesión — el backend ya devuelve tokens al registrarse
                    PetFinderApp.sessionManager.saveSession(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        userId = body.usuario.usuarioId,
                        rol = body.usuario.rol,
                        nombre = body.usuario.nombre
                    )

                    // Conectar WebSocket
                    SocketManager.connect(body.accessToken, context)

                    // Registrar FCM token (silencioso si falla)
                    registrarFcmToken()

                    _uiState.value = RegisterState.Success
                } else {
                    val msg = when (response.code()) {
                        409 -> "Este correo ya está registrado."
                        400 -> "Datos inválidos. Revisa el formulario."
                        else -> "Error del servidor (${response.code()})"
                    }
                    _uiState.value = RegisterState.Error(msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "registerOwner: ${e.message}", e)
                _uiState.value = RegisterState.Error("Error de red. Verifica tu conexión.")
            }
        }
    }

    private suspend fun registrarFcmToken() {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            PetFinderApp.sessionManager.saveFcmToken(fcmToken)
            val userApi = RetrofitClient.instance.create(UserApi::class.java)
            userApi.updateFcmToken(FcmTokenRequest(fcmToken))
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo registrar el FCM token: ${e.message}")
        }
    }
}
