package com.frontend.petfinder.geofencing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.geofencing.data.CreateZoneRequest
import com.frontend.petfinder.geofencing.data.GeofencingApi
import com.frontend.petfinder.geofencing.data.PointDto
import com.frontend.petfinder.geofencing.data.ZoneDto
import com.frontend.petfinder.pets.data.PetApi
import com.frontend.petfinder.pets.data.PetListItemDto
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyZonesViewModel : ViewModel() {
    private val geoApi = RetrofitClient.instance.create(GeofencingApi::class.java)
    private val petApi = RetrofitClient.instance.create(PetApi::class.java)

    // Datos globales
    private val _zonas = MutableStateFlow<List<ZoneDto>>(emptyList())
    val zonas: StateFlow<List<ZoneDto>> = _zonas.asStateFlow()

    private val _pets = MutableStateFlow<List<PetListItemDto>>(emptyList())
    val pets: StateFlow<List<PetListItemDto>> = _pets.asStateFlow()

    // Estados de Dibujo
    var isDrawingMode = MutableStateFlow(false)
    var drawingType = MutableStateFlow("circulo")
    var tempCircleCenter = MutableStateFlow<LatLng?>(null)
    var tempPolygonPoints = MutableStateFlow<List<LatLng>>(emptyList())

    fun loadData() {
        viewModelScope.launch {
            try {
                // 1. Cargar las zonas desde el Snapshot global
                val mapResponse = geoApi.getMapSnapshot()
                if (mapResponse.isSuccessful) {
                    _zonas.value = mapResponse.body()?.zonas ?: emptyList()
                }

                // 2. Cargar mis mascotas para el menú de checkboxes
                val petResponse = petApi.getMyPets()
                if (petResponse.isSuccessful) {
                    _pets.value = petResponse.body() ?: emptyList()
                }
            } catch (e: Exception) {
                println("Error cargando datos: ${e.message}")
            }
        }
    }

    fun handleMapClick(latLng: LatLng) {
        if (!isDrawingMode.value) return
        if (drawingType.value == "circulo") {
            tempCircleCenter.value = latLng
        } else {
            tempPolygonPoints.value = tempPolygonPoints.value + latLng
        }
    }

    fun startDrawing(tipo: String) {
        drawingType.value = tipo
        isDrawingMode.value = true
        tempCircleCenter.value = null
        tempPolygonPoints.value = emptyList()
    }

    fun cancelDrawing() {
        isDrawingMode.value = false
    }

    fun undoLastPolygonPoint() {
        if (tempPolygonPoints.value.isNotEmpty()) {
            tempPolygonPoints.value = tempPolygonPoints.value.dropLast(1)
        }
    }

    fun saveZoneWithMultiplePets(nombreZona: String, selectedPetIds: List<String>) {
        if (selectedPetIds.isEmpty()) return

        // La API requiere un petId primario en la URL, y el resto va en la lista
        val primaryPetId = selectedPetIds.first()

        viewModelScope.launch {
            try {
                val request = if (drawingType.value == "circulo") {
                    val center = tempCircleCenter.value ?: return@launch
                    CreateZoneRequest(
                        nombreZona = nombreZona,
                        tipo = "circulo",
                        lat = center.latitude,
                        lng = center.longitude,
                        radioMetros = 80.0, // Radio fijo para simplificar
                        mascotaIds = selectedPetIds
                    )
                } else {
                    val points = tempPolygonPoints.value
                    if (points.size < 3) return@launch

                    val closedPoints = points + points.first()
                    val coordenadas = closedPoints.map { PointDto(lat = it.latitude, lng = it.longitude) }

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
                    loadData() // Recargar el mapa para ver la zona nueva
                }
            } catch (e: Exception) {
                println("Error guardando zona: ${e.message}")
            }
        }
    }
}