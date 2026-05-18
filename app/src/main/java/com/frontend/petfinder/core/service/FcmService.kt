package com.frontend.petfinder.core.service

import android.util.Log
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.profile.data.UserApi
import com.frontend.petfinder.profile.data.dto.FcmTokenRequest
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "FcmService"

class FcmService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token renovado")
        serviceScope.launch {
            try {
                PetFinderApp.sessionManager.saveFcmToken(token)
                val isValid = PetFinderApp.sessionManager.isSessionValid().first()
                if (isValid) {
                    RetrofitClient.instance.create(UserApi::class.java)
                        .updateFcmToken(FcmTokenRequest(token))
                    Log.d(TAG, "Nuevo FCM token registrado en el backend")
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo actualizar el FCM token: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: return

        Log.d(TAG, "Push recibido — tipo: $type, data: $data")

        when (type) {
            "qr_scan" -> handleQrScan(data)
            "pet_lost" -> handlePetLost(data)
            "zone_exit" -> handleZoneExit(data)
            else -> Log.w(TAG, "Tipo de notificación desconocido: $type")
        }
    }

    private fun handleQrScan(data: Map<String, String>) {
        val petName = data["petName"] ?: "Tu mascota"
        val lat = data["lat"]
        val lng = data["lng"]
        val location = if (lat != null && lng != null) " en ($lat, $lng)" else ""
        Log.d(TAG, "QR escaneado para $petName$location")
        // TODO(Sprint2-H12): mostrar notificación local con PendingIntent a PetDetailScreen
    }

    private fun handlePetLost(data: Map<String, String>) {
        val petName = data["petName"] ?: "Una mascota"
        Log.d(TAG, "Reporte de mascota extraviada: $petName")
        // TODO(Sprint2-H13): mostrar notificación local con PendingIntent al mapa
    }

    private fun handleZoneExit(data: Map<String, String>) {
        val petName = data["petName"] ?: "Tu mascota"
        val zoneName = data["zoneName"] ?: "una zona"
        Log.d(TAG, "$petName salió de $zoneName")
        // TODO(Sprint2-H19): mostrar notificación local de alerta de zona
    }
}
