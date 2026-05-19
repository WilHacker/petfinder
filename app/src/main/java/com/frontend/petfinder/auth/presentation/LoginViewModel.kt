package com.frontend.petfinder.auth.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.auth.data.AuthApi
import com.frontend.petfinder.auth.data.LoginRequest
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

private const val TAG = "LoginViewModel"

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

    fun login(context: Context) {
        if (correo.value.isBlank() || clave.value.isBlank()) {
            _uiState.value = LoginState.Error("Por favor, ingresa tus credenciales.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginState.Loading
            try {
                val api = RetrofitClient.instance.create(AuthApi::class.java)
                val response = api.loginOwner(LoginRequest(correo.value, clave.value))

                if (response.isSuccessful) {
                    val body = response.body()!!

                    // 1. Persistir sesión en DataStore
                    PetFinderApp.sessionManager.saveSession(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        userId = body.usuario.usuarioId,
                        rol = body.usuario.rol,
                        nombre = body.usuario.nombre
                    )

                    // 2. Conectar WebSocket con el token fresco
                    SocketManager.connect(body.accessToken, context)

                    // 3. Registrar token FCM en el backend (silencioso si falla)
                    registrarFcmToken()

                    _uiState.value = LoginState.Success
                } else {
                    _uiState.value = LoginState.Error(
                        if (response.code() == 401) "Credenciales incorrectas."
                        else "Error del servidor (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "login: ${e.message}", e)
                _uiState.value = LoginState.Error("Error de red. Inténtalo de nuevo.")
            }
        }
    }

    fun loginWithGoogle(context: Context) {
        val googleAuthUrl = "https://backend-petfinder.onrender.com/auth/google"
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, Uri.parse(googleAuthUrl))
    }

    private suspend fun registrarFcmToken() {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            PetFinderApp.sessionManager.saveFcmToken(fcmToken)
            val userApi = RetrofitClient.instance.create(UserApi::class.java)
            userApi.updateFcmToken(FcmTokenRequest(fcmToken))
            Log.d(TAG, "FCM token registrado en el backend")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo registrar el FCM token: ${e.message}")
        }
    }
}
