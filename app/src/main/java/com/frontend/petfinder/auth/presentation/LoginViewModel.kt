package com.frontend.petfinder.auth.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.auth.data.AuthRepository
import com.frontend.petfinder.auth.data.LoginRequest
import com.frontend.petfinder.core.network.AppConfig
import com.frontend.petfinder.core.network.SocketManager
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginViewModel : ViewModel() {

    private val _correo = MutableStateFlow("")
    val correo: StateFlow<String> = _correo.asStateFlow()

    private val _clave = MutableStateFlow("")
    val clave: StateFlow<String> = _clave.asStateFlow()

    fun onCorreoChange(v: String) { _correo.value = v }
    fun onClaveChange(v: String) { _clave.value = v }

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }

    private val _uiState = MutableStateFlow<LoginState>(LoginState.Idle)
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    fun login(context: Context) {
        if (_correo.value.isBlank() || _clave.value.isBlank()) {
            _uiState.value = LoginState.Error("Completa el correo y la contraseña para continuar.")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginState.Loading
            AuthRepository.login(LoginRequest(_correo.value, _clave.value)).fold(
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
                    _uiState.value = LoginState.Success
                },
                onFailure = { e ->
                    val code = (e as? HttpException)?.code() ?: -1
                    _uiState.value = LoginState.Error(
                        if (code == 401) "El correo o la contraseña no son correctos."
                        else "Algo salió mal en el servidor. Inténtalo en un momento."
                    )
                }
            )
        }
    }

    fun loginWithGoogle(context: Context) {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, Uri.parse(AppConfig.GOOGLE_AUTH_URL))
    }
}
