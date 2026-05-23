package com.frontend.petfinder.geofencing.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.toPrismaMessage
import com.frontend.petfinder.geofencing.data.GeofencingRepository
import com.frontend.petfinder.geofencing.data.UpdateZoneRequest
import com.frontend.petfinder.geofencing.data.ZonePetsRequest
import com.frontend.petfinder.geofencing.data.ZoneWithPetsDto
import com.frontend.petfinder.pets.data.PetRepository
import com.frontend.petfinder.pets.data.dto.PetListItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "ZoneDetailViewModel"

class ZoneDetailViewModel : ViewModel() {

    sealed class ZoneState {
        object Loading : ZoneState()
        data class Success(val zone: ZoneWithPetsDto) : ZoneState()
        data class Error(val message: String) : ZoneState()
    }

    sealed class ActionState {
        object Idle : ActionState()
        object Loading : ActionState()
        object Success : ActionState()
        data class Error(val message: String) : ActionState()
    }

    private val _zoneState = MutableStateFlow<ZoneState>(ZoneState.Loading)
    val zoneState: StateFlow<ZoneState> = _zoneState.asStateFlow()

    private val _userPets = MutableStateFlow<List<PetListItemDto>>(emptyList())
    val userPets: StateFlow<List<PetListItemDto>> = _userPets.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    fun loadZone(zonaId: Int) {
        viewModelScope.launch {
            _zoneState.value = ZoneState.Loading
            GeofencingRepository.getAllUserZones().fold(
                onSuccess = { zones ->
                    val zone = zones.find { it.zonaId == zonaId }
                    _zoneState.value = if (zone != null) ZoneState.Success(zone)
                    else ZoneState.Error("Zona no encontrada.")
                },
                onFailure = { _zoneState.value = ZoneState.Error("No se pudo cargar la zona.") }
            )
        }
    }

    fun loadUserPets() {
        viewModelScope.launch {
            PetRepository.getMyPets().onSuccess { _userPets.value = it }
                .onFailure { e -> Log.w(TAG, "loadUserPets: ${e.message}") }
        }
    }

    fun toggleActive(zonaId: Int, currentState: Boolean) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            GeofencingRepository.updateZone(zonaId, UpdateZoneRequest(estaActiva = !currentState)).fold(
                onSuccess = {
                    loadZone(zonaId)
                    _actionState.value = ActionState.Idle
                },
                onFailure = { _actionState.value = ActionState.Error("No se pudo cambiar el estado.") }
            )
        }
    }

    fun addPets(zonaId: Int, mascotaIds: List<String>) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            GeofencingRepository.addPetsToZone(zonaId, ZonePetsRequest(mascotaIds)).fold(
                onSuccess = {
                    loadZone(zonaId)
                    _actionState.value = ActionState.Success
                },
                onFailure = { e ->
                    _actionState.value = ActionState.Error(
                        e.toPrismaMessage() ?: "No se pudo agregar las mascotas."
                    )
                }
            )
        }
    }

    fun removePet(zonaId: Int, mascotaId: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            GeofencingRepository.removePetsFromZone(zonaId, ZonePetsRequest(listOf(mascotaId))).fold(
                onSuccess = {
                    loadZone(zonaId)
                    _actionState.value = ActionState.Idle
                },
                onFailure = { _actionState.value = ActionState.Error("No se pudo quitar la mascota.") }
            )
        }
    }

    fun updateRadius(zonaId: Int, newRadius: Double) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            GeofencingRepository.updateZone(zonaId, UpdateZoneRequest(radioMetros = newRadius)).fold(
                onSuccess = {
                    loadZone(zonaId)
                    _actionState.value = ActionState.Success
                },
                onFailure = { _actionState.value = ActionState.Error("No se pudo actualizar el radio.") }
            )
        }
    }

    fun resetAction() {
        _actionState.value = ActionState.Idle
    }
}
