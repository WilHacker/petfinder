package com.frontend.petfinder.geofencing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.geofencing.data.GeofencingApi
import com.frontend.petfinder.geofencing.data.UpdateZoneRequest
import com.frontend.petfinder.geofencing.data.ZonePetsRequest
import com.frontend.petfinder.geofencing.data.ZoneWithPetsDto
import com.frontend.petfinder.pets.data.PetApi
import com.frontend.petfinder.pets.data.dto.PetListItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ZoneDetailViewModel : ViewModel() {

    private val geoApi = RetrofitClient.instance.create(GeofencingApi::class.java)
    private val petApi = RetrofitClient.instance.create(PetApi::class.java)

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
            try {
                // GET /geofencing/zones devuelve mascotas completas con snake_case
                val response = geoApi.getAllUserZones()
                if (response.isSuccessful) {
                    val zone = response.body()?.find { it.zonaId == zonaId }
                    if (zone != null) {
                        _zoneState.value = ZoneState.Success(zone)
                    } else {
                        _zoneState.value = ZoneState.Error("Zona no encontrada.")
                    }
                } else {
                    _zoneState.value = ZoneState.Error("No se pudo cargar la zona.")
                }
            } catch (e: Exception) {
                _zoneState.value = ZoneState.Error("Sin conexión. Verifica tu internet e intenta de nuevo.")
            }
        }
    }

    fun loadUserPets() {
        viewModelScope.launch {
            try {
                val response = petApi.getMyPets()
                if (response.isSuccessful) {
                    _userPets.value = response.body() ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    fun toggleActive(zonaId: Int, currentState: Boolean) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            try {
                val response = geoApi.updateZone(zonaId, UpdateZoneRequest(estaActiva = !currentState))
                if (response.isSuccessful) {
                    loadZone(zonaId)
                    _actionState.value = ActionState.Idle
                } else {
                    _actionState.value = ActionState.Error("No se pudo cambiar el estado.")
                }
            } catch (e: Exception) {
                _actionState.value = ActionState.Error("Sin conexión.")
            }
        }
    }

    fun addPets(zonaId: Int, mascotaIds: List<String>) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            try {
                val response = geoApi.addPetsToZone(zonaId, ZonePetsRequest(mascotaIds))
                if (response.isSuccessful) {
                    loadZone(zonaId)
                    _actionState.value = ActionState.Success
                } else {
                    _actionState.value = ActionState.Error("No se pudo agregar las mascotas.")
                }
            } catch (e: Exception) {
                _actionState.value = ActionState.Error("Sin conexión.")
            }
        }
    }

    fun removePet(zonaId: Int, mascotaId: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            try {
                val response = geoApi.removePetsFromZone(zonaId, ZonePetsRequest(listOf(mascotaId)))
                if (response.isSuccessful) {
                    loadZone(zonaId)
                    _actionState.value = ActionState.Idle
                } else {
                    _actionState.value = ActionState.Error("No se pudo quitar la mascota.")
                }
            } catch (e: Exception) {
                _actionState.value = ActionState.Error("Sin conexión.")
            }
        }
    }

    fun resetAction() {
        _actionState.value = ActionState.Idle
    }
}
