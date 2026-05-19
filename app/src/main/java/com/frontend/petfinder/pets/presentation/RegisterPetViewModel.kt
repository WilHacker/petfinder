package com.frontend.petfinder.pets.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.core.utils.ImageUtils
import com.frontend.petfinder.pets.data.PetApi
import com.frontend.petfinder.pets.data.dto.TipoMascotaDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class RegisterPetViewModel : ViewModel() {

    var nombre = MutableStateFlow("")
    var tiposMascota = MutableStateFlow<List<TipoMascotaDto>>(emptyList())
    var tipoSeleccionado = MutableStateFlow<TipoMascotaDto?>(null)

    var sexo = MutableStateFlow("")
    var colorPrimario = MutableStateFlow("")
    var rasgosParticulares = MutableStateFlow("")
    var fotosSeleccionadas = MutableStateFlow<List<Uri>>(emptyList())

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
            try {
                val api = RetrofitClient.instance.create(PetApi::class.java)
                val response = api.getTiposMascota()
                if (response.isSuccessful) {
                    response.body()?.let { tiposMascota.value = it }
                }
            } catch (e: Exception) {
                println("Error descargando tipos: ${e.message}")
            }
        }
    }

    fun registerPet(context: Context) {
        if (nombre.value.isBlank()) {
            _uiState.value = RegisterPetState.Error("El nombre de la mascota es obligatorio.")
            return
        }

        if (tipoSeleccionado.value == null) {
            _uiState.value = RegisterPetState.Error("Selecciona el tipo de mascota para continuar.")
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterPetState.Loading
            try {
                // Instanciamos el API manualmente sin usar @Inject
                val api = RetrofitClient.instance.create(PetApi::class.java)

                val nombreBody = nombre.value.toRequestBodyText()
                val tipoIdBody = tipoSeleccionado.value?.tipoId?.toString()?.toRequestBodyText()
                val sexoBody = sexo.value.ifBlank { null }?.toRequestBodyText()
                val colorBody = colorPrimario.value.ifBlank { null }?.toRequestBodyText()
                val rasgosBody = rasgosParticulares.value.ifBlank { null }?.toRequestBodyText()

                // 1. Usamos el ImageUtils creado anteriormente para comprimir y limitar a 4 fotos
                val fotosPart = ImageUtils.processImagesForUpload(context, fotosSeleccionadas.value)

                // 2. Solo enviamos el índice 0 si realmente hay fotos adjuntas
                val fotoIndexBody = if (fotosPart.isNotEmpty()) "0".toRequestBodyText() else null

                // 3. Llamada a la API pasando TODOS los parámetros, incluyendo rasgosParticulares
                val response = api.registerPet(
                    nombre = nombreBody,
                    tipoId = tipoIdBody,
                    sexo = sexoBody,
                    colorPrimario = colorBody,
                    rasgosParticulares = rasgosBody, // <- Falta corregida
                    fotoPrincipalIndex = fotoIndexBody,
                    fotos = fotosPart.ifEmpty { null }
                )

                if (response.isSuccessful) {
                    val mascotaCreada = response.body()
                    println("Mascota registrada: ${mascotaCreada?.mascotaId}")
                    _uiState.value = RegisterPetState.Success
                } else {
                    val errorDelServidor = response.errorBody()?.string() ?: "Error desconocido"
                    println("Rechazo del backend: $errorDelServidor")
                    _uiState.value = RegisterPetState.Error("No se pudo guardar la mascota. Verifica los datos e intenta de nuevo.")
                }
            } catch (e: Exception) {
                _uiState.value = RegisterPetState.Error("Sin conexión. Verifica tu internet e intenta de nuevo.")
            }
        }
    }

    private fun String.toRequestBodyText(): RequestBody {
        return this.toRequestBody(null)
    }
}