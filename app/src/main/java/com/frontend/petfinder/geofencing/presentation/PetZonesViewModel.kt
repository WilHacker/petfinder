package com.frontend.petfinder.geofencing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.geofencing.data.CreateZoneRequest
import com.frontend.petfinder.geofencing.data.GeofencingApi
import com.frontend.petfinder.geofencing.data.PointDto
import com.frontend.petfinder.geofencing.data.ZoneDto
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PetZonesViewModel : ViewModel() {
    private val api = RetrofitClient.instance.create(GeofencingApi::class.java)

    var currentPetId: String = ""

    private val _zonas = MutableStateFlow<List<ZoneDto>>(emptyList())
    val zonas: StateFlow<List<ZoneDto>> = _zonas.asStateFlow()

    // --- Estados del Modo Dibujo ---
    var isDrawingMode = MutableStateFlow(false)
    var drawingType = MutableStateFlow("circulo") // "circulo" o "poligono"
    var newZoneName = MutableStateFlow("")

    var tempCircleCenter = MutableStateFlow<LatLng?>(null)
    var tempCircleRadius = MutableStateFlow(80.0) // 80 metros por defecto

    var tempPolygonPoints = MutableStateFlow<List<LatLng>>(emptyList())

    fun loadZones(petId: String) {
        currentPetId = petId
        viewModelScope.launch {
            try {
                val response = api.getPetZones(petId)
                if (response.isSuccessful) {
                    _zonas.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                println("Error cargando zonas: ${e.message}")
            }
        }
    }

    fun startDrawing(nombre: String, tipo: String) {
        newZoneName.value = nombre
        drawingType.value = tipo
        isDrawingMode.value = true
        tempCircleCenter.value = null
        tempPolygonPoints.value = emptyList()
    }

    fun cancelDrawing() {
        isDrawingMode.value = false
        tempCircleCenter.value = null
        tempPolygonPoints.value = emptyList()
    }

    // Registra el toque del usuario en la pantalla
    fun handleMapClick(latLng: LatLng) {
        if (!isDrawingMode.value) return

        if (drawingType.value == "circulo") {
            tempCircleCenter.value = latLng
        } else {
            tempPolygonPoints.value = tempPolygonPoints.value + latLng
        }
    }

    fun undoLastPolygonPoint() {
        val currentPoints = tempPolygonPoints.value
        if (currentPoints.isNotEmpty()) {
            tempPolygonPoints.value = currentPoints.dropLast(1)
        }
    }

    fun saveZone() {
        if (currentPetId.isEmpty()) return

        viewModelScope.launch {
            try {
                val request = if (drawingType.value == "circulo") {
                    val center = tempCircleCenter.value ?: return@launch
                    CreateZoneRequest(
                        nombreZona = newZoneName.value,
                        tipo = "circulo",
                        lat = center.latitude,
                        lng = center.longitude,
                        radioMetros = tempCircleRadius.value
                    )
                } else {
                    val points = tempPolygonPoints.value
                    if (points.size < 3) return@launch // Un polígono necesita al menos 3 puntos

                    // Android Studio dibuja el polígono abierto, pero tu backend (GeoJSON)
                    // exige que el primer y último punto sean exactamente iguales para "cerrarlo".
                    val closedPoints = points + points.first()
                    val coordenadas = closedPoints.map { PointDto(lat = it.latitude, lng = it.longitude) }

                    CreateZoneRequest(
                        nombreZona = newZoneName.value,
                        tipo = "poligono",
                        coordenadas = coordenadas
                    )
                }

                val response = api.createZone(currentPetId, request)
                if (response.isSuccessful) {
                    cancelDrawing()
                    loadZones(currentPetId) // Recargamos para ver la zona guardada oficialmente
                }
            } catch (e: Exception) {
                println("Error guardando zona: ${e.message}")
            }
        }
    }
}