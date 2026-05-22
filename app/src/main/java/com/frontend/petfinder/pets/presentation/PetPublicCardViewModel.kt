package com.frontend.petfinder.pets.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.pets.data.PetRepository
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
import retrofit2.HttpException

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

    fun loadCard(token: String) {
        viewModelScope.launch {
            _cardState.value = CardState.Loading
            PetRepository.getPublicPetCard(token).fold(
                onSuccess = { _cardState.value = CardState.Success(it) },
                onFailure = { e ->
                    val code = (e as? HttpException)?.code() ?: -1
                    _cardState.value = CardState.Error(when (code) {
                        404 -> "Este código QR no existe o fue desactivado."
                        else -> "Error al cargar la información (${code})"
                    })
                }
            )
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

                val request = QrScanRequest(lat = location?.latitude, lng = location?.longitude)
                PetRepository.registerQrScan(token, request).fold(
                    onSuccess = {
                        _scanRegistered.value = true
                        Log.d(TAG, "Escaneo registrado — lat=${request.lat}, lng=${request.lng}")
                    },
                    onFailure = { e -> Log.w(TAG, "No se pudo registrar el escaneo: ${e.message}") }
                )
            } catch (e: Exception) {
                Log.w(TAG, "registerScan falló inesperadamente: ${e.message}")
            }
        }
    }
}
