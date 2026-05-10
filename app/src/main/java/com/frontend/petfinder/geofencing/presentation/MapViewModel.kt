package com.frontend.petfinder.geofencing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.geofencing.data.*
import com.frontend.petfinder.pets.data.PetApi
import com.frontend.petfinder.pets.data.PetListItemDto
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {
    private val geoApi = RetrofitClient.instance.create(GeofencingApi::class.java)
    private val petApi = RetrofitClient.instance.create(PetApi::class.java)

    private val _snapshot = MutableStateFlow<MapSnapshotResponse?>(null)
    val snapshot: StateFlow<MapSnapshotResponse?> = _snapshot.asStateFlow()

    // Mascotas para el diálogo de checkboxes
    private val _pets = MutableStateFlow<List<PetListItemDto>>(emptyList())
    val pets: StateFlow<List<PetListItemDto>> = _pets.asStateFlow()

    // --- ESTADOS DEL MODO DIBUJO ---
    var isDrawingMode = MutableStateFlow(false)
    var drawingType = MutableStateFlow("circulo")
    var tempCircleCenter = MutableStateFlow<LatLng?>(null)
    var tempPolygonPoints = MutableStateFlow<List<LatLng>>(emptyList())

    init {
        cargarDatosDelMapa()
    }

    fun cargarDatosDelMapa() {
        viewModelScope.launch {
            try {
                val res = geoApi.getMapSnapshot()
                if (res.isSuccessful) _snapshot.value = res.body()

                val petRes = petApi.getMyPets()
                if (petRes.isSuccessful) _pets.value = petRes.body() ?: emptyList()
            } catch (e: Exception) {
                println("Error de red: ${e.message}")
            }
        }
    }

    // --- LÓGICA DE DIBUJO ---
    fun startDrawing(tipo: String) {
        drawingType.value = tipo
        isDrawingMode.value = true
        tempCircleCenter.value = null
        tempPolygonPoints.value = emptyList()
    }

    fun cancelDrawing() {
        isDrawingMode.value = false
    }

    fun handleMapClick(latLng: LatLng) {
        if (!isDrawingMode.value) return
        if (drawingType.value == "circulo") tempCircleCenter.value = latLng
        else tempPolygonPoints.value = tempPolygonPoints.value + latLng
    }

    fun undoLastPolygonPoint() {
        if (tempPolygonPoints.value.isNotEmpty()) {
            tempPolygonPoints.value = tempPolygonPoints.value.dropLast(1)
        }
    }

    fun saveZoneWithMultiplePets(nombreZona: String, selectedPetIds: List<String>) {
        if (selectedPetIds.isEmpty()) return
        val primaryPetId = selectedPetIds.first()

        viewModelScope.launch {
            try {
                val request = if (drawingType.value == "circulo") {
                    val center = tempCircleCenter.value ?: return@launch
                    CreateZoneRequest(
                        nombreZona = nombreZona, tipo = "circulo",
                        lat = center.latitude, lng = center.longitude,
                        radioMetros = 80.0, mascotaIds = selectedPetIds
                    )
                } else {
                    val points = tempPolygonPoints.value
                    if (points.size < 3) return@launch
                    val closedPoints = points + points.first()
                    val coordenadas = closedPoints.map { PointDto(lat = it.latitude, lng = it.longitude) }
                    CreateZoneRequest(
                        nombreZona = nombreZona, tipo = "poligono",
                        coordenadas = coordenadas, mascotaIds = selectedPetIds
                    )
                }

                val response = geoApi.createZone(primaryPetId, request)
                if (response.isSuccessful) {
                    cancelDrawing()
                    cargarDatosDelMapa() // Recargar el mapa con la nueva zona
                }
            } catch (e: Exception) {
                println("Error guardando zona: ${e.message}")
            }
        }
    }

    fun deleteZone(zonaId: Int) {
        viewModelScope.launch {
            try {
                val res = geoApi.deleteZone(zonaId)
                if (res.isSuccessful) cargarDatosDelMapa()
            } catch (e: Exception) {
                println("Error al eliminar: ${e.message}")
            }
        }
    }
}