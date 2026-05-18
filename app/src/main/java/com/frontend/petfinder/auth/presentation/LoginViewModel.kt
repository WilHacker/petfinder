package com.frontend.petfinder.auth.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.auth.data.AuthApi
import com.frontend.petfinder.auth.data.LoginRequest
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.core.network.SocketManager // ¡NUEVO! Importamos el ecosistema en tiempo real
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    var correo = MutableStateFlow("")
    var clave = MutableStateFlow("")

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }

    private val _uiState = MutableStateFlow<LoginState>(LoginState.Idle)
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    // Modificamos la función para requerir el Context
    fun login(context: Context) {
        if (correo.value.isBlank() || clave.value.isBlank()) {
            _uiState.value = LoginState.Error("Por favor, ingresa tus credenciales.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginState.Loading
            try {
                val api = RetrofitClient.instance.create(AuthApi::class.java)
                val request = LoginRequest(
                    correoElectronico = correo.value,
                    clave = clave.value
                )

                val response = api.loginOwner(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.accessToken

                    if (token != null) {
                        // 1. Guardamos el token en el interceptor para futuras peticiones REST
                        RetrofitClient.authInterceptor.setToken(token)

                        // 2. ¡NUEVO! Conectamos el ecosistema bidireccional (WebSockets)
                        SocketManager.connect(token, context)

                        _uiState.value = LoginState.Success
                    } else {
                        _uiState.value = LoginState.Error("Error: El servidor no proporcionó un token.")
                    }
                } else {
                    _uiState.value = LoginState.Error("Credenciales incorrectas (Código: ${response.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = LoginState.Error("Error de red. Inténtalo de nuevo más tarde.")
            }
        }
    }
}