package com.frontend.petfinder.profile.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.auth.data.AuthRepository
import com.frontend.petfinder.core.network.SocketManager
import com.frontend.petfinder.profile.data.ProfileRepository
import com.frontend.petfinder.profile.data.dto.ContactoDto
import com.frontend.petfinder.profile.data.dto.CreateContactRequest
import com.frontend.petfinder.profile.data.dto.UpdateContactRequest
import com.frontend.petfinder.profile.data.dto.UpdateProfileRequest
import com.frontend.petfinder.profile.data.dto.UserProfileDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

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

    private val _nombre = MutableStateFlow("")
    val nombre: StateFlow<String> = _nombre.asStateFlow()

    private val _apellidoPaterno = MutableStateFlow("")
    val apellidoPaterno: StateFlow<String> = _apellidoPaterno.asStateFlow()

    private val _apellidoMaterno = MutableStateFlow("")
    val apellidoMaterno: StateFlow<String> = _apellidoMaterno.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactoDto>>(emptyList())
    val contacts: StateFlow<List<ContactoDto>> = _contacts.asStateFlow()

    private val _contactsLoading = MutableStateFlow(false)
    val contactsLoading: StateFlow<Boolean> = _contactsLoading.asStateFlow()

    private val _contactError = MutableStateFlow<String?>(null)
    val contactError: StateFlow<String?> = _contactError.asStateFlow()

    fun onNombreChange(v: String) { _nombre.value = v }
    fun onApellidoPaternoChange(v: String) { _apellidoPaterno.value = v }
    fun onApellidoMaternoChange(v: String) { _apellidoMaterno.value = v }

    init {
        loadProfile()
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _contactsLoading.value = true
            ProfileRepository.getEmergencyContacts().fold(
                onSuccess = { _contacts.value = it },
                onFailure = {}
            )
            _contactsLoading.value = false
        }
    }

    fun addEmergencyContact(tipo: String, valor: String) {
        if (valor.isBlank()) { _contactError.value = "El valor no puede estar vacío"; return }
        viewModelScope.launch {
            _contactsLoading.value = true
            ProfileRepository.addContact(CreateContactRequest(tipo = tipo, valor = valor.trim(), esPrincipal = _contacts.value.isEmpty(), esEmergencia = true)).fold(
                onSuccess = { loadContacts() },
                onFailure = { _contactError.value = "No se pudo agregar el contacto" }
            )
            _contactsLoading.value = false
        }
    }

    fun deleteContact(contactoId: Int) {
        viewModelScope.launch {
            ProfileRepository.deleteContact(contactoId).fold(
                onSuccess = { _contacts.value = _contacts.value.filter { it.contactoId != contactoId } },
                onFailure = { _contactError.value = "No se pudo eliminar el contacto" }
            )
        }
    }

    fun clearContactError() {
        _contactError.value = null
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            ProfileRepository.getMyProfile().fold(
                onSuccess = { profile ->
                    _profileState.value = ProfileState.Success(profile)
                    profile.persona?.let { p ->
                        _nombre.value = p.nombre
                        _apellidoPaterno.value = p.apellidoPaterno
                        _apellidoMaterno.value = p.apellidoMaterno ?: ""
                    }
                },
                onFailure = { e ->
                    _profileState.value = ProfileState.Error("Error al cargar el perfil")
                }
            )
        }
    }

    fun saveProfile() {
        if (_nombre.value.isBlank() || _apellidoPaterno.value.isBlank()) {
            _saveState.value = SaveState.Error("Nombre y apellido son obligatorios.")
            return
        }
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            ProfileRepository.updateProfile(
                UpdateProfileRequest(
                    nombre = _nombre.value.trim(),
                    apellidoPaterno = _apellidoPaterno.value.trim(),
                    apellidoMaterno = _apellidoMaterno.value.trim().ifBlank { null }
                )
            ).fold(
                onSuccess = {
                    _saveState.value = SaveState.Saved
                    (_profileState.value as? ProfileState.Success)?.let { current ->
                        _profileState.value = current.copy(
                            profile = current.profile.copy(
                                persona = current.profile.persona?.copy(
                                    nombre = _nombre.value.trim(),
                                    apellidoPaterno = _apellidoPaterno.value.trim(),
                                    apellidoMaterno = _apellidoMaterno.value.trim().ifBlank { null }
                                )
                            )
                        )
                    }
                },
                onFailure = {
                    _saveState.value = SaveState.Error("No se pudo guardar")
                }
            )
        }
    }

    fun uploadPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                val part = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                    val bytes = stream.readBytes()
                    stream.close()
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("foto", "photo.jpg", requestBody)
                } ?: return@launch

                ProfileRepository.updateProfilePhoto(part).fold(
                    onSuccess = {
                        _saveState.value = SaveState.Saved
                        (_profileState.value as? ProfileState.Success)?.let { current ->
                            _profileState.value = current.copy(
                                profile = current.profile.copy(
                                    persona = current.profile.persona?.copy(
                                        fotoPerfilUrl = uri.toString()
                                    )
                                )
                            )
                        }
                    },
                    onFailure = {
                        _saveState.value = SaveState.Error("No se pudo subir la foto")
                    }
                )
            } catch (e: Exception) {
                _saveState.value = SaveState.Error("Error al subir la foto")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            AuthRepository.logout()
            SocketManager.disconnect()
            PetFinderApp.sessionManager.clearSession()
        }
    }
}
