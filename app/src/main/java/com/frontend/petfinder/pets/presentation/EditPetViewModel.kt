package com.frontend.petfinder.pets.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.pets.data.PetRepository
import com.frontend.petfinder.pets.data.dto.FotoMascotaDto
import com.frontend.petfinder.pets.data.dto.TipoMascotaDto
import com.frontend.petfinder.pets.data.dto.UpdatePetRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class EditPetViewModel : ViewModel() {

    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }

    sealed class DeleteState {
        object Idle : DeleteState()
        object Confirming : DeleteState()
        object Deleting : DeleteState()
        object Success : DeleteState()
        data class Error(val message: String) : DeleteState()
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _nombre = MutableStateFlow("")
    val nombre: StateFlow<String> = _nombre.asStateFlow()

    private val _tiposMascota = MutableStateFlow<List<TipoMascotaDto>>(emptyList())
    val tiposMascota: StateFlow<List<TipoMascotaDto>> = _tiposMascota.asStateFlow()

    private val _tipoSeleccionado = MutableStateFlow<TipoMascotaDto?>(null)
    val tipoSeleccionado: StateFlow<TipoMascotaDto?> = _tipoSeleccionado.asStateFlow()

    private val _sexo = MutableStateFlow<String?>(null)
    val sexo: StateFlow<String?> = _sexo.asStateFlow()

    private val _colorPrimario = MutableStateFlow("")
    val colorPrimario: StateFlow<String> = _colorPrimario.asStateFlow()

    private val _rasgosParticulares = MutableStateFlow("")
    val rasgosParticulares: StateFlow<String> = _rasgosParticulares.asStateFlow()

    private val _existingPhotos = MutableStateFlow<List<FotoMascotaDto>>(emptyList())
    val existingPhotos: StateFlow<List<FotoMascotaDto>> = _existingPhotos.asStateFlow()

    private val _newPhotos = MutableStateFlow<List<Uri>>(emptyList())
    val newPhotos: StateFlow<List<Uri>> = _newPhotos.asStateFlow()

    private val _photoDeletingIds = MutableStateFlow<Set<Int>>(emptySet())
    val photoDeletingIds: StateFlow<Set<Int>> = _photoDeletingIds.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    fun load(petId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val detailJob = async { PetRepository.getPetDetail(petId) }
            val tiposJob  = async { PetRepository.getTiposMascota() }

            tiposJob.await().onSuccess { tipos -> _tiposMascota.value = tipos }

            detailJob.await().onSuccess { pet ->
                _nombre.value = pet.nombre
                _sexo.value = pet.sexo?.let { if (it == "F") "H" else it }
                _colorPrimario.value = pet.colorPrimario ?: ""
                _rasgosParticulares.value = pet.rasgosParticulares ?: ""
                _existingPhotos.value = pet.fotos ?: emptyList()
                _tipoSeleccionado.value = _tiposMascota.value.find { it.tipoId == pet.tipoId }
            }
            _isLoading.value = false
        }
    }

    fun onNombreChange(v: String) { _nombre.value = v }
    fun onTipoSeleccionado(t: TipoMascotaDto) { _tipoSeleccionado.value = t }
    fun onSexoChange(v: String) { _sexo.value = if (_sexo.value == v) null else v }
    fun onColorPrimarioChange(v: String) { _colorPrimario.value = v }
    fun onRasgosChange(v: String) { _rasgosParticulares.value = v }

    fun addNewPhotos(uris: List<Uri>) {
        val available = 4 - _existingPhotos.value.size - _newPhotos.value.size
        if (available > 0) _newPhotos.value = _newPhotos.value + uris.take(available)
    }

    fun removeNewPhoto(index: Int) {
        _newPhotos.value = _newPhotos.value.toMutableList().also { it.removeAt(index) }
    }

    fun deleteExistingPhoto(petId: String, fotoId: Int) {
        val totalPhotos = _existingPhotos.value.size + _newPhotos.value.size
        if (totalPhotos <= 1) return
        viewModelScope.launch {
            _photoDeletingIds.value = _photoDeletingIds.value + fotoId
            PetRepository.deletePetPhoto(petId, fotoId).onSuccess {
                _existingPhotos.value = _existingPhotos.value.filter { it.fotoId != fotoId }
            }
            _photoDeletingIds.value = _photoDeletingIds.value - fotoId
        }
    }

    fun save(context: Context, petId: String) {
        if (_nombre.value.isBlank()) {
            _saveState.value = SaveState.Error("El nombre es obligatorio")
            return
        }
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            PetRepository.updatePet(
                petId,
                UpdatePetRequest(
                    nombre = _nombre.value.trim(),
                    colorPrimario = _colorPrimario.value.trim().ifBlank { null },
                    rasgosParticulares = _rasgosParticulares.value.trim().ifBlank { null },
                    sexo = _sexo.value
                )
            ).fold(
                onSuccess = {
                    uploadNewPhotosIfAny(context, petId)
                    _saveState.value = SaveState.Success
                },
                onFailure = { e ->
                    _saveState.value = SaveState.Error(e.message ?: "No se pudieron guardar los cambios")
                }
            )
        }
    }

    private suspend fun uploadNewPhotosIfAny(context: Context, petId: String) {
        if (_newPhotos.value.isEmpty()) return
        val parts = _newPhotos.value.mapIndexed { i, uri ->
            val cr = context.contentResolver
            val mime = cr.getType(uri) ?: "image/jpeg"
            val ext = when { mime.contains("png") -> "png"; mime.contains("webp") -> "webp"; else -> "jpg" }
            val bytes = cr.openInputStream(uri)?.readBytes() ?: return@mapIndexed null
            MultipartBody.Part.createFormData("fotos", "foto_$i.$ext", bytes.toRequestBody(mime.toMediaType()))
        }.filterNotNull()
        if (parts.isNotEmpty()) PetRepository.addPetPhotos(petId, parts)
    }

    fun confirmDelete() { _deleteState.value = DeleteState.Confirming }
    fun cancelDelete()  { _deleteState.value = DeleteState.Idle }

    fun deletePet(petId: String) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting
            PetRepository.deletePet(petId).fold(
                onSuccess  = { _deleteState.value = DeleteState.Success },
                onFailure  = { _deleteState.value = DeleteState.Error("No se pudo eliminar la mascota") }
            )
        }
    }

    fun resetSaveError() { _saveState.value = SaveState.Idle }
}
