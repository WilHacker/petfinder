package com.frontend.petfinder.geofencing.presentation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.SocketManager
import com.frontend.petfinder.core.service.LocationTrackingService
import com.frontend.petfinder.core.service.TrackingManager
import com.frontend.petfinder.core.utils.PermissionHandler
import com.frontend.petfinder.geofencing.data.*
import com.frontend.petfinder.geofencing.data.GeofencingRepository
import com.frontend.petfinder.pets.data.PetRepository
import com.frontend.petfinder.pets.data.dto.PetListItemDto
import com.frontend.petfinder.pets.data.dto.TipoMascotaDto
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "MapViewModel"

class MapViewModel : ViewModel() {

    private val _snapshot = MutableStateFlow<MapSnapshotResponse?>(null)
    val snapshot: StateFlow<MapSnapshotResponse?> = _snapshot.asStateFlow()

    private val _pets = MutableStateFlow<List<PetListItemDto>>(emptyList())
    val pets: StateFlow<List<PetListItemDto>> = _pets.asStateFlow()

    private val _livePetLocations = MutableStateFlow<Map<String, LatLng>>(emptyMap())
    val livePetLocations: StateFlow<Map<String, LatLng>> = _livePetLocations.asStateFlow()

    private val _liveOwnerLocations = MutableStateFlow<Map<String, LatLng>>(emptyMap())
    val liveOwnerLocations: StateFlow<Map<String, LatLng>> = _liveOwnerLocations.asStateFlow()

    private val _isDrawingMode = MutableStateFlow(false)
    val isDrawingMode: StateFlow<Boolean> = _isDrawingMode.asStateFlow()

    private val _drawingType = MutableStateFlow("circulo")
    val drawingType: StateFlow<String> = _drawingType.asStateFlow()

    private val _tempCircleCenter = MutableStateFlow<LatLng?>(null)
    val tempCircleCenter: StateFlow<LatLng?> = _tempCircleCenter.asStateFlow()

    private val _tempPolygonPoints = MutableStateFlow<List<LatLng>>(emptyList())
    val tempPolygonPoints: StateFlow<List<LatLng>> = _tempPolygonPoints.asStateFlow()

    private val _circleRadius = MutableStateFlow(80.0)
    val circleRadius: StateFlow<Double> = _circleRadius.asStateFlow()

    private val _userZones = MutableStateFlow<List<ZoneWithPetsDto>>(emptyList())
    val userZones: StateFlow<List<ZoneWithPetsDto>> = _userZones.asStateFlow()

    private val _isZonesLoading = MutableStateFlow(false)
    val isZonesLoading: StateFlow<Boolean> = _isZonesLoading.asStateFlow()

    private val _lostPets = MutableStateFlow<List<LostPetMarkerDto>>(emptyList())
    val lostPets: StateFlow<List<LostPetMarkerDto>> = _lostPets.asStateFlow()

    private val _showLostPets = MutableStateFlow(false)
    val showLostPets: StateFlow<Boolean> = _showLostPets.asStateFlow()

    fun toggleLostPetsLayer() {
        _showLostPets.value = !_showLostPets.value
    }

    private val _tiposMascota = MutableStateFlow<List<TipoMascotaDto>>(emptyList())
    val tiposMascota: StateFlow<List<TipoMascotaDto>> = _tiposMascota.asStateFlow()

    private val _speciesFilter = MutableStateFlow<Int?>(null)
    val speciesFilter: StateFlow<Int?> = _speciesFilter.asStateFlow()

    fun setSpeciesFilter(tipoId: Int?) {
        _speciesFilter.value = tipoId
        cargarDatosDelMapa()
    }

    private val _zoneExitAlert = MutableStateFlow<ZoneExitAlertUi?>(null)
    val zoneExitAlert: StateFlow<ZoneExitAlertUi?> = _zoneExitAlert.asStateFlow()

    data class ZoneExitAlertUi(
        val mascotaId: String,
        val zonaId: Int,
        val petName: String,
        val zoneName: String
    )

    fun dismissZoneAlert() {
        _zoneExitAlert.value = null
    }

    val isTracking: StateFlow<Boolean> = TrackingManager.isTracking

    private val _trackingError = MutableStateFlow<String?>(null)
    val trackingError: StateFlow<String?> = _trackingError.asStateFlow()

    init {
        cargarDatosDelMapa()
        escucharEventosEnTiempoReal()
        loadTiposMascota()
    }

    private fun loadTiposMascota() {
        viewModelScope.launch {
            PetRepository.getTiposMascota().onSuccess { _tiposMascota.value = it }
        }
    }

    private fun escucharEventosEnTiempoReal() {
        viewModelScope.launch {
            SocketManager.petLocationFlow.collect { update ->
                _livePetLocations.update { it + (update.mascotaId to LatLng(update.lat, update.lng)) }
            }
        }

        viewModelScope.launch {
            SocketManager.petStatusFlow.collect { update ->
                if (_snapshot.value == null) _snapshot.first { it != null }
                _snapshot.update { current ->
                    current?.copy(
                        misMascotas = current.misMascotas.map { pet ->
                            if (pet.mascotaId == update.mascotaId) pet.copy(estado = update.estado)
                            else pet
                        }
                    )
                }
                GeofencingRepository.getMapSnapshot(_speciesFilter.value).fold(
                    onSuccess = { _snapshot.value = it },
                    onFailure = { e -> Log.w(TAG, "petStatusFlow refresh: ${e.message}") }
                )
            }
        }

        viewModelScope.launch {
            SocketManager.ownerLocationFlow.collect { update ->
                _liveOwnerLocations.update { it + (update.personaId to LatLng(update.lat, update.lng)) }
            }
        }

        viewModelScope.launch {
            SocketManager.petProfileFlow.collect { update ->
                _snapshot.update { current ->
                    current?.copy(
                        misMascotas = current.misMascotas.map { pet ->
                            if (pet.mascotaId == update.mascotaId) pet.copy(
                                nombre = update.nombre ?: pet.nombre,
                                estado = update.estado ?: pet.estado,
                                fotoUrl = update.fotoUrl ?: pet.fotoUrl
                            ) else pet
                        }
                    )
                }
            }
        }
        viewModelScope.launch {
            SocketManager.zoneExitFlow.collect { event ->
                val petName = _pets.value.find { it.mascotaId == event.mascotaId }?.nombre
                    ?: _snapshot.value?.misMascotas
                        ?.find { it.mascotaId == event.mascotaId }?.nombre
                    ?: "Tu mascota"
                val zoneName = _snapshot.value?.zonas
                    ?.find { it.zonaId == event.zonaId }
                    ?.let { it.nombre ?: it.nombreZona }
                    ?: _userZones.value.find { it.zonaId == event.zonaId }?.nombreZona
                    ?: "la zona segura"
                _zoneExitAlert.value = ZoneExitAlertUi(
                    mascotaId = event.mascotaId,
                    zonaId = event.zonaId,
                    petName = petName,
                    zoneName = zoneName
                )
            }
        }
    }

    fun cargarDatosDelMapa() {
        viewModelScope.launch {
            val filter = _speciesFilter.value
            val snapshotJob = async { GeofencingRepository.getMapSnapshot(filter) }
            val petsJob     = async { PetRepository.getMyPets() }
            val lostPetsJob = async { GeofencingRepository.getPublicLostPets(filter) }

            snapshotJob.await().fold(
                onSuccess = { _snapshot.value = it },
                onFailure = { e ->
                    Log.w(TAG, "getMapSnapshot: ${e.message}")
                    _trackingError.value = "No se pudo cargar el mapa. Verifica tu conexión."
                }
            )
            petsJob.await().fold(
                onSuccess = { _pets.value = it },
                onFailure = { e -> Log.w(TAG, "getMyPets: ${e.message}") }
            )
            lostPetsJob.await().fold(
                onSuccess = { _lostPets.value = it },
                onFailure = { e -> Log.w(TAG, "getPublicLostPets: ${e.message}") }
            )
        }
    }

    fun clearTrackingError() {
        _trackingError.value = null
    }

    fun togglePaseo(context: Context, mascotaId: String) {
        viewModelScope.launch {
            if (isTracking.value) {
                PetRepository.updatePetStatus(mascotaId, "en_casa").fold(
                    onSuccess = {
                        detenerServicioRastreo(context)
                        cargarDatosDelMapa()
                    },
                    onFailure = { e ->
                        Log.e(TAG, "togglePaseo stop: ${e.message}", e)
                        _trackingError.value = "No se pudo detener el paseo"
                    }
                )
            } else {
                if (!PermissionHandler.isReadyForTracking(context)) {
                    _trackingError.value = "Faltan permisos de ubicación"
                    return@launch
                }
                PetRepository.updatePetStatus(mascotaId, "en_paseo").fold(
                    onSuccess = {
                        iniciarServicioRastreo(context)
                        cargarDatosDelMapa()
                    },
                    onFailure = { e ->
                        Log.e(TAG, "togglePaseo start: ${e.message}", e)
                        _trackingError.value = "No se pudo iniciar el paseo"
                    }
                )
            }
        }
    }

    private fun iniciarServicioRastreo(context: Context) {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(intent)
        else
            context.startService(intent)
    }

    private fun detenerServicioRastreo(context: Context) {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun startDrawing(tipo: String) {
        _drawingType.value = tipo
        _isDrawingMode.value = true
        _tempCircleCenter.value = null
        _tempPolygonPoints.value = emptyList()
        _circleRadius.value = 80.0
    }

    fun cancelDrawing() {
        _isDrawingMode.value = false
    }

    fun handleMapClick(latLng: LatLng) {
        if (!_isDrawingMode.value) return
        if (_drawingType.value == "circulo") {
            _tempCircleCenter.value = latLng
        } else {
            _tempPolygonPoints.value = _tempPolygonPoints.value + latLng
        }
    }

    fun undoLastPolygonPoint() {
        if (_tempPolygonPoints.value.isNotEmpty())
            _tempPolygonPoints.value = _tempPolygonPoints.value.dropLast(1)
    }

    fun updateCircleRadius(radius: Double) {
        _circleRadius.value = radius
    }

    fun saveZoneWithMultiplePets(nombreZona: String, selectedPetIds: List<String>) {
        if (selectedPetIds.isEmpty()) return
        val primaryPetId = selectedPetIds.first()

        viewModelScope.launch {
            val request = if (_drawingType.value == "circulo") {
                val center = _tempCircleCenter.value ?: return@launch
                CreateZoneRequest(
                    nombreZona = nombreZona,
                    tipo = "circulo",
                    lat = center.latitude,
                    lng = center.longitude,
                    radioMetros = _circleRadius.value,
                    mascotaIds = selectedPetIds
                )
            } else {
                val points = _tempPolygonPoints.value
                if (points.size < 3) return@launch
                val coordenadas = (points + points.first()).map {
                    PointDto(lat = it.latitude, lng = it.longitude)
                }
                CreateZoneRequest(
                    nombreZona = nombreZona,
                    tipo = "poligono",
                    coordenadas = coordenadas,
                    mascotaIds = selectedPetIds
                )
            }

            GeofencingRepository.createZone(primaryPetId, request).fold(
                onSuccess = {
                    cancelDrawing()
                    cargarDatosDelMapa()
                    loadAllUserZones()
                },
                onFailure = { e ->
                    Log.w(TAG, "saveZoneWithMultiplePets: ${e.message}")
                    _trackingError.value = "No se pudo guardar la zona"
                }
            )
        }
    }

    fun loadAllUserZones() {
        viewModelScope.launch {
            _isZonesLoading.value = true
            GeofencingRepository.getAllUserZones().fold(
                onSuccess = { _userZones.value = it },
                onFailure = { e ->
                    Log.w(TAG, "loadAllUserZones: ${e.message}")
                    _userZones.value = emptyList()
                }
            )
            _isZonesLoading.value = false
        }
    }

    fun toggleZoneState(zonaId: Int, currentActiveState: Boolean) {
        viewModelScope.launch {
            GeofencingRepository.updateZone(zonaId, UpdateZoneRequest(estaActiva = !currentActiveState)).fold(
                onSuccess = {
                    loadAllUserZones()
                    cargarDatosDelMapa()
                },
                onFailure = { e ->
                    Log.w(TAG, "toggleZoneState: ${e.message}")
                    _trackingError.value = "No se pudo actualizar la zona"
                }
            )
        }
    }

    fun deleteZone(zonaId: Int) {
        viewModelScope.launch {
            GeofencingRepository.deleteZone(zonaId).fold(
                onSuccess = {
                    loadAllUserZones()
                    cargarDatosDelMapa()
                },
                onFailure = { e ->
                    Log.w(TAG, "deleteZone: ${e.message}")
                    _trackingError.value = "No se pudo eliminar la zona"
                }
            )
        }
    }

    private val _focusMascotaId = MutableStateFlow<String?>(null)
    val focusMascotaId: StateFlow<String?> = _focusMascotaId.asStateFlow()

    fun focusOnPet(mascotaId: String) {
        _focusMascotaId.value = mascotaId
    }

    fun clearFocus() {
        _focusMascotaId.value = null
    }
}
