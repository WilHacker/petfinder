package com.frontend.petfinder.pets.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.pets.data.PetApi
import com.frontend.petfinder.pets.data.dto.PublicPetCardDto
import com.frontend.petfinder.pets.data.dto.QrScanRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "PetPublicCardViewModel"

class PetPublicCardViewModel : ViewModel() {

    sealed class CardState {
        object Loading : CardState()
        data class Success(val card: PublicPetCardDto) : CardState()
        data class Error(val message: String) : CardState()
    }

    private val _cardState = MutableStateFlow<CardState>(CardState.Loading)
    val cardState: StateFlow<CardState> = _cardState.asStateFlow()

    private val _scanRegistered = MutableStateFlow(false)
    val scanRegistered: StateFlow<Boolean> = _scanRegistered.asStateFlow()

    private val api: PetApi by lazy {
        RetrofitClient.instance.create(PetApi::class.java)
    }

    fun loadCard(token: String) {
        viewModelScope.launch {
            _cardState.value = CardState.Loading
            try {
                val response = api.getPublicPetCard(token)
                if (response.isSuccessful) {
                    _cardState.value = CardState.Success(response.body()!!)
                } else {
                    val msg = when (response.code()) {
                        404 -> "Este código QR no existe o fue desactivado."
                        else -> "Error al cargar la información (${response.code()})"
                    }
                    _cardState.value = CardState.Error(msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadCard: ${e.message}", e)
                _cardState.value = CardState.Error("Sin conexión. Verifica tu internet.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun registerScan(token: String, context: Context) {
        if (_scanRegistered.value) return
        viewModelScope.launch {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                val location = try {
                    fusedClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cts.token
                    ).await()
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo obtener GPS: ${e.message}")
                    null
                }

                val request = QrScanRequest(
                    lat = location?.latitude,
                    lng = location?.longitude
                )
                val response = api.registerQrScan(token, request)
                if (response.isSuccessful) {
                    _scanRegistered.value = true
                    Log.d(TAG, "Escaneo registrado — lat=${request.lat}, lng=${request.lng}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "registerScan silencioso: ${e.message}")
            }
        }
    }
}
