package com.frontend.petfinder.geofencing.presentation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.core.network.SocketManager
import com.frontend.petfinder.core.service.LocationTrackingService
import com.frontend.petfinder.core.service.TrackingManager
import com.frontend.petfinder.core.utils.PermissionHandler
import com.frontend.petfinder.geofencing.data.*
import com.frontend.petfinder.pets.data.PetApi
import com.frontend.petfinder.pets.data.dto.PetListItemDto
import com.frontend.petfinder.pets.data.dto.UpdateStatusRequest
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "MapViewModel"

class MapViewModel : ViewModel() {

    private val geoApi = RetrofitClient.instance.create(GeofencingApi::class.java)
    private val petApi = RetrofitClient.instance.create(PetApi::class.java)

    // --- Snapshot del mapa ---
    private val _snapshot = MutableStateFlow<MapSnapshotResponse?>(null)
    val snapshot: StateFlow<MapSnapshotResponse?> = _snapshot.asStateFlow()

    private val _pets = MutableStateFlow<List<PetListItemDto>>(emptyList())
    val pets: StateFlow<List<PetListItemDto>> = _pets.asStateFlow()

    // --- Ubicaciones en tiempo real ---
    private val _livePetLocations = MutableStateFlow<Map<String, LatLng>>(emptyMap())
    val livePetLocations: StateFlow<Map<String, LatLng>> = _livePetLocations.asStateFlow()

    private val _liveOwnerLocations = MutableStateFlow<Map<String, LatLng>>(emptyMap())
    val liveOwnerLocations: StateFlow<Map<String, LatLng>> = _liveOwnerLocations.asStateFlow()

    // --- Estado de dibujo de zonas (ahora correctamente encapsulado) ---
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

    // --- Zonas del usuario (pantalla Mis Zonas) ---
    private val _userZones = MutableStateFlow<List<ZoneWithPetsDto>>(emptyList())
    val userZones: StateFlow<List<ZoneWithPetsDto>> = _userZones.asStateFlow()

    private val _isZonesLoading = MutableStateFlow(false)
    val isZonesLoading: StateFlow<Boolean> = _isZonesLoading.asStateFlow()

    // --- Mascotas perdidas públicas (H13) ---
    private val _lostPets = MutableStateFlow<List<com.frontend.petfinder.geofencing.data.LostPetMarkerDto>>(emptyList())
    val lostPets: StateFlow<List<com.frontend.petfinder.geofencing.data.LostPetMarkerDto>> = _lostPets.asStateFlow()

    private val _showLostPets = MutableStateFlow(true)
    val showLostPets: StateFlow<Boolean> = _showLostPets.asStateFlow()

    fun toggleLostPetsLayer() {
        _showLostPets.value = !_showLostPets.value
    }

    // --- Alerta de zona (H19) ---
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

    // --- Tracking y errores ---
    val isTracking: StateFlow<Boolean> = TrackingManager.isTracking

    private val _trackingError = MutableStateFlow<String?>(null)
    val trackingError: StateFlow<String?> = _trackingError.asStateFlow()

    init {
        cargarDatosDelMapa()
        escucharEventosEnTiempoReal()
    }

    // --- WebSockets ---

    private fun escucharEventosEnTiempoReal() {
        viewModelScope.launch {
            SocketManager.petLocationFlow.collect { update ->
                _livePetLocations.update { it + (update.mascotaId to LatLng(update.lat, update.lng)) }
            }
        }
        viewModelScope.launch {
            SocketManager.ownerLocationFlow.collect { update ->
                _liveOwnerLocations.update { it + (update.personaId to LatLng(update.lat, update.lng)) }
            }
        }
        viewModelScope.launch {
            SocketManager.zoneExitFlow.collect { event ->
                val petName = _pets.value.find { it.mascotaId == event.mascotaId }?.nombre
                    ?: _snapshot.value?.zonas
                        ?.flatMap { it.mascotas ?: emptyList() }
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

    // --- Carga de datos ---

    fun cargarDatosDelMapa() {
        viewModelScope.launch {
            try {
                val res = geoApi.getMapSnapshot()
                if (res.isSuccessful) _snapshot.value = res.body()
                else Log.w(TAG, "getMapSnapshot error ${res.code()}")

                val petRes = petApi.getMyPets()
                if (petRes.isSuccessful) _pets.value = petRes.body() ?: emptyList()
                else Log.w(TAG, "getMyPets error ${petRes.code()}")

                val lostRes = geoApi.getPublicLostPets()
                if (lostRes.isSuccessful) _lostPets.value = lostRes.body() ?: emptyList()
                else Log.w(TAG, "getPublicLostPets error ${lostRes.code()}")
            } catch (e: Exception) {
                Log.e(TAG, "cargarDatosDelMapa: ${e.message}", e)
            }
        }
    }

    // --- Paseo / Tracking ---

    fun clearTrackingError() {
        _trackingError.value = null
    }

    fun togglePaseo(context: Context, mascotaId: String) {
        viewModelScope.launch {
            if (isTracking.value) {
                try {
                    val response = petApi.updatePetStatus(mascotaId, UpdateStatusRequest("en_casa"))
                    if (response.isSuccessful) {
                        detenerServicioRastreo(context)
                        cargarDatosDelMapa()
                    } else {
                        _trackingError.value = "No se pudo detener el paseo (${response.code()})"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "togglePaseo stop: ${e.message}", e)
                    _trackingError.value = "Error de red al detener el paseo"
                }
            } else {
                if (!PermissionHandler.isReadyForTracking(context)) {
                    _trackingError.value = "Faltan permisos de ubicación"
                    return@launch
                }
                try {
                    val response = petApi.updatePetStatus(mascotaId, UpdateStatusRequest("en_paseo"))
                    if (response.isSuccessful) {
                        iniciarServicioRastreo(context)
                        cargarDatosDelMapa()
                    } else {
                        _trackingError.value = "No se pudo iniciar el paseo (${response.code()})"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "togglePaseo start: ${e.message}", e)
                    _trackingError.value = "Error de red al iniciar el paseo"
                }
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

    // --- Dibujo de zonas ---

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

    // --- Guardar zonas ---

    fun saveZoneWithMultiplePets(nombreZona: String, selectedPetIds: List<String>) {
        if (selectedPetIds.isEmpty()) return
        val primaryPetId = selectedPetIds.first()

        viewModelScope.launch {
            try {
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

                val response = geoApi.createZone(primaryPetId, request)
                if (response.isSuccessful) {
                    cancelDrawing()
                    cargarDatosDelMapa()
                    loadAllUserZones()
                } else {
                    Log.w(TAG, "createZone error ${response.code()}")
                    _trackingError.value = "No se pudo guardar la zona (${response.code()})"
                }
            } catch (e: Exception) {
                Log.e(TAG, "saveZoneWithMultiplePets: ${e.message}", e)
                _trackingError.value = "Error de red al guardar la zona"
            }
        }
    }

    // --- Gestión de zonas ---

    fun loadAllUserZones() {
        viewModelScope.launch {
            _isZonesLoading.value = true
            try {
                val response = geoApi.getAllUserZones()
                if (response.isSuccessful) {
                    _userZones.value = response.body() ?: emptyList()
                } else {
                    Log.w(TAG, "getAllUserZones error ${response.code()}")
                    _userZones.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadAllUserZones: ${e.message}", e)
                _userZones.value = emptyList()
            } finally {
                _isZonesLoading.value = false
            }
        }
    }

    fun toggleZoneState(zonaId: Int, currentActiveState: Boolean) {
        viewModelScope.launch {
            try {
                val response = geoApi.updateZone(zonaId, UpdateZoneRequest(estaActiva = !currentActiveState))
                if (response.isSuccessful) {
                    loadAllUserZones()
                    cargarDatosDelMapa()
                } else {
                    Log.w(TAG, "toggleZoneState error ${response.code()}")
                    _trackingError.value = "No se pudo actualizar la zona"
                }
            } catch (e: Exception) {
                Log.e(TAG, "toggleZoneState: ${e.message}", e)
                _trackingError.value = "Error de red al actualizar la zona"
            }
        }
    }

    fun deleteZone(zonaId: Int) {
        viewModelScope.launch {
            try {
                val response = geoApi.deleteZone(zonaId)
                if (response.isSuccessful) {
                    loadAllUserZones()
                    cargarDatosDelMapa()
                } else {
                    Log.w(TAG, "deleteZone error ${response.code()}")
                    _trackingError.value = "No se pudo eliminar la zona"
                }
            } catch (e: Exception) {
                Log.e(TAG, "deleteZone: ${e.message}", e)
                _trackingError.value = "Error de red al eliminar la zona"
            }
        }
    }
}
