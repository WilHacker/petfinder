package com.frontend.petfinder.pets.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.toPrismaMessage
import com.frontend.petfinder.core.utils.ImageUtils
import com.frontend.petfinder.pets.data.PetRepository
import com.frontend.petfinder.pets.data.dto.TipoMascotaDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.RequestBody.Companion.toRequestBody

private const val TAG = "RegisterPetViewModel"

class RegisterPetViewModel : ViewModel() {

    private val _nombre = MutableStateFlow("")
    val nombre: StateFlow<String> = _nombre.asStateFlow()

    private val _tiposMascota = MutableStateFlow<List<TipoMascotaDto>>(emptyList())
    val tiposMascota: StateFlow<List<TipoMascotaDto>> = _tiposMascota.asStateFlow()

    private val _tipoSeleccionado = MutableStateFlow<TipoMascotaDto?>(null)
    val tipoSeleccionado: StateFlow<TipoMascotaDto?> = _tipoSeleccionado.asStateFlow()

    private val _sexo = MutableStateFlow("")
    val sexo: StateFlow<String> = _sexo.asStateFlow()

    private val _colorPrimario = MutableStateFlow("")
    val colorPrimario: StateFlow<String> = _colorPrimario.asStateFlow()

    private val _rasgosParticulares = MutableStateFlow("")
    val rasgosParticulares: StateFlow<String> = _rasgosParticulares.asStateFlow()

    private val _fotosSeleccionadas = MutableStateFlow<List<Uri>>(emptyList())
    val fotosSeleccionadas: StateFlow<List<Uri>> = _fotosSeleccionadas.asStateFlow()

    fun onNombreChange(v: String) { _nombre.value = v }
    fun onTipoSeleccionado(tipo: TipoMascotaDto?) { _tipoSeleccionado.value = tipo }
    fun onSexoChange(v: String) { _sexo.value = v }
    fun onColorPrimarioChange(v: String) { _colorPrimario.value = v }
    fun onRasgosChange(v: String) { _rasgosParticulares.value = v }
    fun onFotosChange(fotos: List<Uri>) { _fotosSeleccionadas.value = fotos }
    fun onFotoRemoved(index: Int) {
        _fotosSeleccionadas.value = _fotosSeleccionadas.value.toMutableList().also { it.removeAt(index) }
    }

    sealed class RegisterPetState {
        object Idle : RegisterPetState()
        object Loading : RegisterPetState()
        object Success : RegisterPetState()
        data class Error(val message: String) : RegisterPetState()
    }

    private val _uiState = MutableStateFlow<RegisterPetState>(RegisterPetState.Idle)
    val uiState: StateFlow<RegisterPetState> = _uiState.asStateFlow()

    init {
        cargarTiposDeMascota()
    }

    private fun cargarTiposDeMascota() {
        viewModelScope.launch {
            PetRepository.getTiposMascota().fold(
                onSuccess = { _tiposMascota.value = it },
                onFailure = { e -> Log.w(TAG, "cargarTiposDeMascota: ${e.message}") }
            )
        }
    }

    fun registerPet(context: Context) {
        if (_nombre.value.isBlank()) {
            _uiState.value = RegisterPetState.Error("El nombre de la mascota es obligatorio.")
            return
        }
        if (_tipoSeleccionado.value == null) {
            _uiState.value = RegisterPetState.Error("Selecciona el tipo de mascota para continuar.")
            return
        }
        viewModelScope.launch {
            _uiState.value = RegisterPetState.Loading

            val nombreBody = _nombre.value.toRequestBody(null)
            val tipoIdBody = _tipoSeleccionado.value?.tipoId?.toString()?.toRequestBody(null)
            val sexoBody = _sexo.value.ifBlank { null }?.toRequestBody(null)
            val colorBody = _colorPrimario.value.ifBlank { null }?.toRequestBody(null)
            val rasgosBody = _rasgosParticulares.value.ifBlank { null }?.toRequestBody(null)
            val fotosPart = ImageUtils.processImagesForUpload(context, _fotosSeleccionadas.value)
            val fotoIndexBody = if (fotosPart.isNotEmpty()) "0".toRequestBody(null) else null

            PetRepository.registerPet(
                nombre = nombreBody,
                tipoId = tipoIdBody,
                sexo = sexoBody,
                colorPrimario = colorBody,
                rasgosParticulares = rasgosBody,
                fotoPrincipalIndex = fotoIndexBody,
                fotos = fotosPart.ifEmpty { null }
            ).fold(
                onSuccess = { _uiState.value = RegisterPetState.Success },
                onFailure = { e ->
                    _uiState.value = RegisterPetState.Error(
                        e.toPrismaMessage()
                            ?: "No se pudo guardar la mascota. Verifica los datos e intenta de nuevo."
                    )
                }
            )
        }
    }

}
