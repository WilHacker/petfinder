package com.frontend.petfinder.geofencing.presentation

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.core.network.SocketManager
import com.frontend.petfinder.core.service.LocationTrackingService
import com.frontend.petfinder.core.service.TrackingManager
import com.frontend.petfinder.core.utils.PermissionHandler
import com.frontend.petfinder.geofencing.data.*
import com.frontend.petfinder.pets.data.PetApi
import com.frontend.petfinder.pets.data.PetListItemDto
import com.frontend.petfinder.pets.data.dto.UpdateStatusRequest
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {
    private val geoApi = RetrofitClient.instance.create(GeofencingApi::class.java)
    private val petApi = RetrofitClient.instance.create(PetApi::class.java)

    private val _snapshot = MutableStateFlow<MapSnapshotResponse?>(null)
    val snapshot: StateFlow<MapSnapshotResponse?> = _snapshot.asStateFlow()

    private val _pets = MutableStateFlow<List<PetListItemDto>>(emptyList())
    val pets: StateFlow<List<PetListItemDto>> = _pets.asStateFlow()

    private val _livePetLocations = MutableStateFlow<Map<String, LatLng>>(emptyMap())
    val livePetLocations: StateFlow<Map<String, LatLng>> = _livePetLocations.asStateFlow()

    private val _liveOwnerLocations = MutableStateFlow<Map<String, LatLng>>(emptyMap())
    val liveOwnerLocations: StateFlow<Map<String, LatLng>> = _liveOwnerLocations.asStateFlow()

    var isDrawingMode = MutableStateFlow(false)
    var drawingType = MutableStateFlow("circulo")
    var tempCircleCenter = MutableStateFlow<LatLng?>(null)
    var tempPolygonPoints = MutableStateFlow<List<LatLng>>(emptyList())
    var circleRadius = MutableStateFlow(80.0)

    // --- NUEVOS ESTADOS FASE 6 (API GLOBAL) ---
    private val _userZones = MutableStateFlow<List<ZoneWithPetsDto>>(emptyList())
    val userZones: StateFlow<List<ZoneWithPetsDto>> = _userZones.asStateFlow()

    private val _isZonesLoading = MutableStateFlow(false)
    val isZonesLoading: StateFlow<Boolean> = _isZonesLoading.asStateFlow()

    val isTracking: StateFlow<Boolean> = TrackingManager.isTracking
    private val _trackingError = MutableStateFlow<String?>(null)
    val trackingError: StateFlow<String?> = _trackingError.asStateFlow()

    init {
        cargarDatosDelMapa()
        escucharEventosEnTiempoReal()
    }

    private fun escucharEventosEnTiempoReal() {
        viewModelScope.launch {
            SocketManager.petLocationFlow.collect { update ->
                val nuevaUbicacion = LatLng(update.lat, update.lng)
                _livePetLocations.update { currentMap -> currentMap + (update.mascotaId to nuevaUbicacion) }
            }
        }
        viewModelScope.launch {
            SocketManager.ownerLocationFlow.collect { update ->
                val nuevaUbicacion = LatLng(update.lat, update.lng)
                _liveOwnerLocations.update { currentMap -> currentMap + (update.personaId to nuevaUbicacion) }
            }
        }
    }

    fun cargarDatosDelMapa() {
        viewModelScope.launch {
            try {
                val res = geoApi.getMapSnapshot()
                if (res.isSuccessful) _snapshot.value = res.body()

                val petRes = petApi.getMyPets()
                if (petRes.isSuccessful) _pets.value = petRes.body() ?: emptyList()
            } catch (e: Exception) { println("Error: ${e.message}") }
        }
    }

    fun clearTrackingError() { _trackingError.value = null }

    fun togglePaseo(context: Context, mascotaId: String) {
        viewModelScope.launch {
            if (isTracking.value) {
                try {
                    val response = petApi.updatePetStatus(mascotaId, UpdateStatusRequest("en_casa"))
                    if (response.isSuccessful) { detenerServicioRastreo(context); cargarDatosDelMapa() }
                } catch (e: Exception) { }
            } else {
                if (!PermissionHandler.isReadyForTracking(context)) { _trackingError.value = "Faltan permisos"; return@launch }
                try {
                    val response = petApi.updatePetStatus(mascotaId, UpdateStatusRequest("en_paseo"))
                    if (response.isSuccessful) { iniciarServicioRastreo(context); cargarDatosDelMapa() }
                } catch (e: Exception) { }
            }
        }
    }

    private fun iniciarServicioRastreo(context: Context) {
        val intent = Intent(context, LocationTrackingService::class.java).apply { action = LocationTrackingService.ACTION_START }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
    }

    private fun detenerServicioRastreo(context: Context) {
        val intent = Intent(context, LocationTrackingService::class.java).apply { action = LocationTrackingService.ACTION_STOP }
        context.startService(intent)
    }

    fun startDrawing(tipo: String) {
        drawingType.value = tipo; isDrawingMode.value = true; tempCircleCenter.value = null; tempPolygonPoints.value = emptyList(); circleRadius.value = 80.0
    }
    fun cancelDrawing() { isDrawingMode.value = false }
    fun handleMapClick(latLng: LatLng) {
        if (!isDrawingMode.value) return
        if (drawingType.value == "circulo") tempCircleCenter.value = latLng else tempPolygonPoints.value = tempPolygonPoints.value + latLng
    }
    fun undoLastPolygonPoint() { if (tempPolygonPoints.value.isNotEmpty()) tempPolygonPoints.value = tempPolygonPoints.value.dropLast(1) }

    fun saveZoneWithMultiplePets(nombreZona: String, selectedPetIds: List<String>) {
        if (selectedPetIds.isEmpty()) return
        val primaryPetId = selectedPetIds.first()
        viewModelScope.launch {
            try {
                val request = if (drawingType.value == "circulo") {
                    val center = tempCircleCenter.value ?: return@launch
                    CreateZoneRequest(nombreZona = nombreZona, tipo = "circulo", lat = center.latitude, lng = center.longitude, radioMetros = circleRadius.value, mascotaIds = selectedPetIds)
                } else {
                    val points = tempPolygonPoints.value
                    if (points.size < 3) return@launch
                    val coordenadas = (points + points.first()).map { PointDto(lat = it.latitude, lng = it.longitude) }
                    CreateZoneRequest(nombreZona = nombreZona, tipo = "poligono", coordenadas = coordenadas, mascotaIds = selectedPetIds)
                }
                val response = geoApi.createZone(primaryPetId, request)
                if (response.isSuccessful) { cancelDrawing(); cargarDatosDelMapa(); loadAllUserZones() }
            } catch (e: Exception) { }
        }
    }

    // --- LÓGICA CON EL NUEVO ENDPOINT GLOBAL ---
    fun loadAllUserZones() {
        viewModelScope.launch {
            _isZonesLoading.value = true
            try {
                val response = geoApi.getAllUserZones()
                if (response.isSuccessful) {
                    _userZones.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
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
                }
            } catch (e: Exception) { }
        }
    }

    fun deleteZone(zonaId: Int) {
        viewModelScope.launch {
            try {
                val res = geoApi.deleteZone(zonaId)
                if (res.isSuccessful) {
                    loadAllUserZones()
                    cargarDatosDelMapa()
                }
            } catch (e: Exception) { }
        }
    }
}