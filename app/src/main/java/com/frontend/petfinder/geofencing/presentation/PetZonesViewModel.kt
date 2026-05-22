package com.frontend.petfinder.geofencing.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.geofencing.data.*
import com.frontend.petfinder.geofencing.data.GeofencingRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PetZonesViewModel"

class PetZonesViewModel : ViewModel() {

    var currentPetId: String = ""

    private val _zonas = MutableStateFlow<List<ZoneDto>>(emptyList())
    val zonas: StateFlow<List<ZoneDto>> = _zonas.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isDrawingMode = MutableStateFlow(false)
    val isDrawingMode: StateFlow<Boolean> = _isDrawingMode.asStateFlow()

    private val _drawingType = MutableStateFlow("circulo")
    val drawingType: StateFlow<String> = _drawingType.asStateFlow()

    private val _newZoneName = MutableStateFlow("")
    val newZoneName: StateFlow<String> = _newZoneName.asStateFlow()

    private val _tempCircleCenter = MutableStateFlow<LatLng?>(null)
    val tempCircleCenter: StateFlow<LatLng?> = _tempCircleCenter.asStateFlow()

    private val _tempCircleRadius = MutableStateFlow(80.0)
    val tempCircleRadius: StateFlow<Double> = _tempCircleRadius.asStateFlow()

    private val _tempPolygonPoints = MutableStateFlow<List<LatLng>>(emptyList())
    val tempPolygonPoints: StateFlow<List<LatLng>> = _tempPolygonPoints.asStateFlow()

    fun loadZones(petId: String) {
        currentPetId = petId
        viewModelScope.launch {
            _error.value = null
            GeofencingRepository.getPetZones(petId).fold(
                onSuccess = { _zonas.value = it },
                onFailure = { e ->
                    Log.w(TAG, "loadZones: ${e.message}")
                    _error.value = "No se pudieron cargar las zonas."
                }
            )
        }
    }

    fun startDrawing(nombre: String, tipo: String) {
        _newZoneName.value = nombre
        _drawingType.value = tipo
        _isDrawingMode.value = true
        _tempCircleCenter.value = null
        _tempPolygonPoints.value = emptyList()
    }

    fun cancelDrawing() {
        _isDrawingMode.value = false
        _tempCircleCenter.value = null
        _tempPolygonPoints.value = emptyList()
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
        val currentPoints = _tempPolygonPoints.value
        if (currentPoints.isNotEmpty()) {
            _tempPolygonPoints.value = currentPoints.dropLast(1)
        }
    }

    fun saveZone() {
        if (currentPetId.isEmpty()) return
        viewModelScope.launch {
            val request = if (_drawingType.value == "circulo") {
                val center = _tempCircleCenter.value ?: return@launch
                CreateZoneRequest(
                    nombreZona = _newZoneName.value,
                    tipo = "circulo",
                    lat = center.latitude,
                    lng = center.longitude,
                    radioMetros = _tempCircleRadius.value
                )
            } else {
                val points = _tempPolygonPoints.value
                if (points.size < 3) return@launch
                val closedPoints = points + points.first()
                val coordenadas = closedPoints.map { PointDto(lat = it.latitude, lng = it.longitude) }
                CreateZoneRequest(
                    nombreZona = _newZoneName.value,
                    tipo = "poligono",
                    coordenadas = coordenadas
                )
            }

            GeofencingRepository.createZone(currentPetId, request).fold(
                onSuccess = {
                    cancelDrawing()
                    loadZones(currentPetId)
                },
                onFailure = { e ->
                    Log.w(TAG, "saveZone: ${e.message}")
                    _error.value = "No se pudo guardar la zona. Inténtalo de nuevo."
                }
            )
        }
    }
}
