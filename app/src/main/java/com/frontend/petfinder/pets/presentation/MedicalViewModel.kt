package com.frontend.petfinder.pets.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.pets.data.PetRepository
import com.frontend.petfinder.pets.data.dto.CreateMedicalRecordRequest
import com.frontend.petfinder.pets.data.dto.MedicalRecordDto
import com.frontend.petfinder.pets.data.dto.UpdateMedicalRecordRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MedicalViewModel : ViewModel() {

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
            PetRepository.getMedicalRecords(petId).fold(
                onSuccess = { _records.value = it },
                onFailure = { _error.value = "No se pudo cargar el historial médico." }
            )
            _isLoading.value = false
        }
    }

    fun createRecord(petId: String, request: CreateMedicalRecordRequest) {
        viewModelScope.launch {
            _formState.value = FormState.Saving
            PetRepository.createMedicalRecord(petId, request).fold(
                onSuccess = {
                    _formState.value = FormState.Success
                    loadRecords(petId)
                },
                onFailure = { _formState.value = FormState.Error("No se pudo guardar el registro.") }
            )
        }
    }

    fun updateRecord(petId: String, registroId: Int, request: UpdateMedicalRecordRequest) {
        viewModelScope.launch {
            _formState.value = FormState.Saving
            PetRepository.updateMedicalRecord(petId, registroId, request).fold(
                onSuccess = {
                    _formState.value = FormState.Success
                    loadRecords(petId)
                },
                onFailure = { _formState.value = FormState.Error("No se pudo actualizar el registro.") }
            )
        }
    }

    fun deleteRecord(petId: String, registroId: Int) {
        viewModelScope.launch {
            PetRepository.deleteMedicalRecord(petId, registroId).fold(
                onSuccess = { _records.value = _records.value.filter { it.registroId != registroId } },
                onFailure = { _error.value = "No se pudo eliminar el registro." }
            )
        }
    }

    fun resetFormState() {
        _formState.value = FormState.Idle
    }
}
