package com.frontend.petfinder.geofencing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.geofencing.data.GeofencingApi
import com.frontend.petfinder.geofencing.data.MapSnapshotResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    private val _snapshot = MutableStateFlow<MapSnapshotResponse?>(null)
    val snapshot: StateFlow<MapSnapshotResponse?> = _snapshot

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        cargarDatosDelMapa()
    }

    fun cargarDatosDelMapa() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val api = RetrofitClient.instance.create(GeofencingApi::class.java)
                val response = api.getMapSnapshot()
                if (response.isSuccessful) {
                    _snapshot.value = response.body()
                }
            } catch (e: Exception) {
                // Manejar error de conexión
            } finally {
                _isLoading.value = false
            }
        }
    }
}