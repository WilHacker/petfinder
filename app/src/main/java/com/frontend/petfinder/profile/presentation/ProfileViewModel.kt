package com.frontend.petfinder.profile.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.auth.data.AuthApi
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.core.network.SocketManager
import com.frontend.petfinder.profile.data.UserApi
import com.frontend.petfinder.profile.data.dto.PersonaDto
import com.frontend.petfinder.profile.data.dto.UpdateProfileRequest
import com.frontend.petfinder.profile.data.dto.UserProfileDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private const val TAG = "ProfileViewModel"

class ProfileViewModel : ViewModel() {

    sealed class ProfileState {
        object Loading : ProfileState()
        data class Success(val profile: UserProfileDto) : ProfileState()
        data class Error(val message: String) : ProfileState()
    }

    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Saved : SaveState()
        data class Error(val message: String) : SaveState()
    }

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // Campos editables
    val nombre = MutableStateFlow("")
    val apellidoPaterno = MutableStateFlow("")
    val apellidoMaterno = MutableStateFlow("")

    private val api: UserApi by lazy {
        RetrofitClient.instance.create(UserApi::class.java)
    }

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                val response = api.getMyProfile()
                if (response.isSuccessful) {
                    val profile = response.body()!!
                    _profileState.value = ProfileState.Success(profile)
                    profile.persona?.let { p ->
                        nombre.value = p.nombre
                        apellidoPaterno.value = p.apellidoPaterno
                        apellidoMaterno.value = p.apellidoMaterno ?: ""
                    }
                } else {
                    _profileState.value = ProfileState.Error("Error al cargar el perfil (${response.code()})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadProfile: ${e.message}", e)
                _profileState.value = ProfileState.Error("Sin conexión")
            }
        }
    }

    fun saveProfile() {
        if (nombre.value.isBlank() || apellidoPaterno.value.isBlank()) {
            _saveState.value = SaveState.Error("Nombre y apellido son obligatorios.")
            return
        }
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                val response = api.updateProfile(
                    UpdateProfileRequest(
                        nombre = nombre.value.trim(),
                        apellidoPaterno = apellidoPaterno.value.trim(),
                        apellidoMaterno = apellidoMaterno.value.trim().ifBlank { null }
                    )
                )
                if (response.isSuccessful) {
                    _saveState.value = SaveState.Saved
                    loadProfile()
                } else {
                    _saveState.value = SaveState.Error("No se pudo guardar (${response.code()})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "saveProfile: ${e.message}", e)
                _saveState.value = SaveState.Error("Error de red")
            }
        }
    }

    fun uploadPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: return@launch
                val bytes = stream.readBytes()
                stream.close()
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("foto", "photo.jpg", requestBody)

                val response = api.updateProfilePhoto(part)
                if (response.isSuccessful) {
                    _saveState.value = SaveState.Saved
                    loadProfile()
                } else {
                    _saveState.value = SaveState.Error("No se pudo subir la foto (${response.code()})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "uploadPhoto: ${e.message}", e)
                _saveState.value = SaveState.Error("Error al subir la foto")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            try {
                RetrofitClient.instance.create(AuthApi::class.java).logout()
            } catch (e: Exception) {
                Log.w(TAG, "logout backend: ${e.message}")
            } finally {
                SocketManager.disconnect()
                PetFinderApp.sessionManager.clearSession()
                // NavGraph reacciona automáticamente al detectar isSessionValid = false
            }
        }
    }
}
