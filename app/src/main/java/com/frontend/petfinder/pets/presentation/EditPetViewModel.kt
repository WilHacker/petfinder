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

    /** Selección de foto principal: una existente (por fotoId) o una nueva (por índice). */
    sealed class Principal {
        data class Existing(val fotoId: Int) : Principal()
        data class New(val index: Int) : Principal()
        object None : Principal()
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

    // Fotos tal como están en el servidor (al cargar)
    private val _existingPhotos = MutableStateFlow<List<FotoMascotaDto>>(emptyList())
    val existingPhotos: StateFlow<List<FotoMascotaDto>> = _existingPhotos.asStateFlow()

    // Fotos existentes marcadas para quitar (se aplica al guardar)
    private val _photosToRemove = MutableStateFlow<Set<Int>>(emptySet())
    val photosToRemove: StateFlow<Set<Int>> = _photosToRemove.asStateFlow()

    // Fotos nuevas seleccionadas (aún no subidas)
    private val _newPhotos = MutableStateFlow<List<Uri>>(emptyList())
    val newPhotos: StateFlow<List<Uri>> = _newPhotos.asStateFlow()

    // Foto principal seleccionada (existente o nueva)
    private val _principal = MutableStateFlow<Principal>(Principal.None)
    val principal: StateFlow<Principal> = _principal.asStateFlow()
    private var originalPrincipalId: Int? = null

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    /** Cantidad final de fotos tras aplicar quitar/agregar — para validación y límite. */
    val finalPhotoCount: Int
        get() = _existingPhotos.value.count { it.fotoId !in _photosToRemove.value } + _newPhotos.value.size

    fun load(petId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val detailJob = async { PetRepository.getPetDetail(petId) }
            val tiposJob  = async { PetRepository.getTiposMascota() }

            tiposJob.await().onSuccess { tipos -> _tiposMascota.value = tipos }

            detailJob.await().onSuccess { pet ->
                _nombre.value = pet.nombre
                _sexo.value = pet.sexo   // canónico "M" | "F" tal como lo espera el backend
                _colorPrimario.value = pet.colorPrimario ?: ""
                _rasgosParticulares.value = pet.rasgosParticulares ?: ""
                _existingPhotos.value = pet.fotos ?: emptyList()
                _tipoSeleccionado.value = _tiposMascota.value.find { it.tipoId == pet.tipoId }
                // Principal por defecto = la actual del servidor
                originalPrincipalId = pet.fotos?.find { it.esPrincipal }?.fotoId
                _principal.value = originalPrincipalId?.let { Principal.Existing(it) } ?: Principal.None
            }
            _isLoading.value = false
        }
    }

    fun onNombreChange(v: String) { _nombre.value = v }
    fun onTipoSeleccionado(t: TipoMascotaDto) { _tipoSeleccionado.value = t }
    fun onSexoChange(v: String) { _sexo.value = if (_sexo.value == v) null else v }
    fun onColorPrimarioChange(v: String) { _colorPrimario.value = v }
    fun onRasgosChange(v: String) { _rasgosParticulares.value = v }

    // ── Fotos (todo diferido hasta Guardar) ───────────────────────────────────

    fun addNewPhotos(uris: List<Uri>) {
        val available = 4 - finalPhotoCount
        if (available > 0) _newPhotos.value = _newPhotos.value + uris.take(available)
    }

    fun removeNewPhoto(index: Int) {
        _newPhotos.value = _newPhotos.value.toMutableList().also { it.removeAt(index) }
        // Reajustar la selección de principal si apuntaba a una foto nueva
        when (val p = _principal.value) {
            is Principal.New -> _principal.value = when {
                p.index == index -> Principal.None
                p.index > index  -> Principal.New(p.index - 1)
                else -> p
            }
            else -> {}
        }
    }

    /** Marca/desmarca una foto existente para quitar (se aplica al guardar). */
    fun toggleRemoveExisting(fotoId: Int) {
        val set = _photosToRemove.value.toMutableSet()
        if (fotoId in set) set.remove(fotoId) else set.add(fotoId)
        _photosToRemove.value = set
        // Si la principal quedó marcada para quitar, mover la selección
        if ((_principal.value as? Principal.Existing)?.fotoId == fotoId && fotoId in set) {
            reassignPrincipalAfterRemoval()
        }
    }

    fun setPrincipalExisting(fotoId: Int) {
        if (fotoId in _photosToRemove.value) return
        _principal.value = Principal.Existing(fotoId)
    }

    fun setPrincipalNew(index: Int) {
        if (index in _newPhotos.value.indices) _principal.value = Principal.New(index)
    }

    private fun reassignPrincipalAfterRemoval() {
        val keptExisting = _existingPhotos.value.firstOrNull { it.fotoId !in _photosToRemove.value }
        _principal.value = when {
            keptExisting != null -> Principal.Existing(keptExisting.fotoId)
            _newPhotos.value.isNotEmpty() -> Principal.New(0)
            else -> Principal.None
        }
    }

    fun save(context: Context, petId: String) {
        if (_nombre.value.isBlank()) {
            _saveState.value = SaveState.Error("El nombre es obligatorio")
            return
        }
        val count = finalPhotoCount
        if (count < 1) {
            _saveState.value = SaveState.Error("La mascota debe conservar al menos una foto")
            return
        }
        if (count > 4) {
            _saveState.value = SaveState.Error("Máximo 4 fotos por mascota")
            return
        }

        viewModelScope.launch {
            _saveState.value = SaveState.Saving

            PetRepository.updatePet(
                petId,
                UpdatePetRequest(
                    nombre = _nombre.value.trim(),
                    tipoId = _tipoSeleccionado.value?.tipoId,
                    colorPrimario = _colorPrimario.value.trim().ifBlank { null },
                    rasgosParticulares = _rasgosParticulares.value.trim().ifBlank { null },
                    sexo = _sexo.value
                )
            ).fold(
                onSuccess = {
                    runCatching { applyPhotoChanges(context, petId) }.fold(
                        onSuccess = { _saveState.value = SaveState.Success },
                        onFailure = { e ->
                            _saveState.value = SaveState.Error(e.message ?: "No se pudieron actualizar las fotos")
                        }
                    )
                },
                onFailure = { e ->
                    _saveState.value = SaveState.Error(e.message ?: "No se pudieron guardar los cambios")
                }
            )
        }
    }

    /**
     * Aplica los cambios de fotos en un orden seguro: intercala subidas y borrados
     * para no superar 4 ni bajar de 1 en ningún momento. Luego fija la principal.
     */
    private suspend fun applyPhotoChanges(context: Context, petId: String) {
        val keptExisting = _existingPhotos.value.filter { it.fotoId !in _photosToRemove.value }

        // Caso "reemplazar todo el álbum": no se conserva ninguna existente y hay nuevas.
        // Endpoint atómico — resuelve incluso el caso de 1 sola foto (DELETE+POST no puede).
        if (keptExisting.isEmpty() && _newPhotos.value.isNotEmpty()) {
            val parts = _newPhotos.value.mapNotNull { buildPart(context, it) }
            if (parts.isEmpty()) throw IllegalStateException("No se pudieron leer las fotos seleccionadas")
            val principalIdx = (_principal.value as? Principal.New)?.index ?: 0
            PetRepository.replacePetPhotos(petId, parts, principalIdx).getOrThrow()
            return
        }

        val toDelete = _photosToRemove.value.toMutableList()
        // Cola de subidas con marca de cuál debe quedar como principal
        val principalNewIdx = (_principal.value as? Principal.New)?.index
        val toUpload = _newPhotos.value.mapIndexed { i, uri ->
            UploadItem(uri = uri, isPrincipal = i == principalNewIdx)
        }.toMutableList()

        if (toDelete.isEmpty() && toUpload.isEmpty()) {
            applyExistingPrincipal(petId)
            return
        }

        var currentCount = _existingPhotos.value.size

        while (toDelete.isNotEmpty() || toUpload.isNotEmpty()) {
            val canDelete = toDelete.isNotEmpty() && currentCount > 1
            val canUpload = toUpload.isNotEmpty() && currentCount < 4
            when {
                canDelete -> {
                    val id = toDelete.removeAt(0)
                    PetRepository.deletePetPhoto(petId, id).getOrThrow()
                    currentCount--
                }
                canUpload -> {
                    val item = toUpload.removeAt(0)
                    val part = buildPart(context, item.uri) ?: continue
                    val idx = if (item.isPrincipal) 0 else null
                    PetRepository.addPetPhotos(petId, listOf(part), idx).getOrThrow()
                    currentCount++
                }
                else -> {
                    // No se puede avanzar sin violar 1..4 — no debería ocurrir tras validar
                    throw IllegalStateException("No se pudieron aplicar los cambios de fotos")
                }
            }
        }

        // Si la principal es una existente que se mantiene, fijarla al final
        applyExistingPrincipal(petId)
    }

    private suspend fun applyExistingPrincipal(petId: String) {
        val sel = _principal.value as? Principal.Existing ?: return
        if (sel.fotoId != originalPrincipalId) {
            PetRepository.setPrincipalPhoto(petId, sel.fotoId).getOrThrow()
        }
    }

    private fun buildPart(context: Context, uri: Uri): MultipartBody.Part? {
        val cr = context.contentResolver
        val mime = cr.getType(uri) ?: "image/jpeg"
        val ext = when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            else -> "jpg"
        }
        val bytes = cr.openInputStream(uri)?.readBytes() ?: return null
        return MultipartBody.Part.createFormData(
            "fotos", "foto_${System.currentTimeMillis()}.$ext", bytes.toRequestBody(mime.toMediaType())
        )
    }

    private data class UploadItem(val uri: Uri, val isPrincipal: Boolean)

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
