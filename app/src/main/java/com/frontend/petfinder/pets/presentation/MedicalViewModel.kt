package com.frontend.petfinder.pets.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.pets.data.PetApi
import com.frontend.petfinder.pets.data.dto.CreateMedicalRecordRequest
import com.frontend.petfinder.pets.data.dto.MedicalRecordDto
import com.frontend.petfinder.pets.data.dto.UpdateMedicalRecordRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MedicalViewModel : ViewModel() {

    private val petApi = RetrofitClient.instance.create(PetApi::class.java)

    private val _records = MutableStateFlow<List<MedicalRecordDto>>(emptyList())
    val records: StateFlow<List<MedicalRecordDto>> = _records.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    sealed class FormState {
        object Idle : FormState()
        object Saving : FormState()
        object Success : FormState()
        data class Error(val message: String) : FormState()
    }

    private val _formState = MutableStateFlow<FormState>(FormState.Idle)
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    fun loadRecords(petId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = petApi.getMedicalRecords(petId)
                if (response.isSuccessful) {
                    _records.value = response.body() ?: emptyList()
                } else {
                    _error.value = "No se pudo cargar el historial médico."
                }
            } catch (e: Exception) {
                _error.value = "Sin conexión. Verifica tu internet e intenta de nuevo."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createRecord(petId: String, request: CreateMedicalRecordRequest) {
        viewModelScope.launch {
            _formState.value = FormState.Saving
            try {
                val response = petApi.createMedicalRecord(petId, request)
                if (response.isSuccessful) {
                    _formState.value = FormState.Success
                    loadRecords(petId)
                } else {
                    _formState.value = FormState.Error("No se pudo guardar el registro.")
                }
            } catch (e: Exception) {
                _formState.value = FormState.Error("Sin conexión. Verifica tu internet e intenta de nuevo.")
            }
        }
    }

    fun updateRecord(petId: String, registroId: Int, request: UpdateMedicalRecordRequest) {
        viewModelScope.launch {
            _formState.value = FormState.Saving
            try {
                val response = petApi.updateMedicalRecord(petId, registroId, request)
                if (response.isSuccessful) {
                    _formState.value = FormState.Success
                    loadRecords(petId)
                } else {
                    _formState.value = FormState.Error("No se pudo actualizar el registro.")
                }
            } catch (e: Exception) {
                _formState.value = FormState.Error("Sin conexión. Verifica tu internet e intenta de nuevo.")
            }
        }
    }

    fun deleteRecord(petId: String, registroId: Int) {
        viewModelScope.launch {
            try {
                petApi.deleteMedicalRecord(petId, registroId)
                _records.value = _records.value.filter { it.registroId != registroId }
            } catch (e: Exception) {
                _error.value = "No se pudo eliminar el registro."
            }
        }
    }

    fun resetFormState() {
        _formState.value = FormState.Idle
    }
}
