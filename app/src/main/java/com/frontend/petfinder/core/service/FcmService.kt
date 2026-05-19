package com.frontend.petfinder.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import com.frontend.petfinder.MainActivity
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.R
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
private const val CHANNEL_QR = "qr_scan"
private const val CHANNEL_ALERT = "zone_alert"

class FcmService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

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
        Log.d(TAG, "Push recibido — tipo: $type")

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
        val locationText = if (lat != null && lng != null) " (ubicación incluida)" else ""

        val mapsIntent = if (lat != null && lng != null) {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(petName)})")
            )
        } else {
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, mapsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        showNotification(
            channelId = CHANNEL_QR,
            notificationId = System.currentTimeMillis().toInt(),
            title = "¡Alguien vio a $petName!",
            body = "Tu mascota fue escaneada$locationText. Toca para ver la ubicación.",
            pendingIntent = pendingIntent
        )
    }

    private fun handlePetLost(data: Map<String, String>) {
        val petName = data["petName"] ?: "Una mascota"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        showNotification(
            channelId = CHANNEL_ALERT,
            notificationId = System.currentTimeMillis().toInt(),
            title = "Mascota extraviada cerca",
            body = "$petName fue reportada como perdida en tu zona.",
            pendingIntent = pendingIntent
        )
    }

    private fun handleZoneExit(data: Map<String, String>) {
        val petName = data["petName"] ?: "Tu mascota"
        val zoneName = data["zoneName"] ?: "una zona segura"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        showNotification(
            channelId = CHANNEL_ALERT,
            notificationId = System.currentTimeMillis().toInt(),
            title = "¡Alerta de zona!",
            body = "$petName salió de $zoneName.",
            pendingIntent = pendingIntent
        )
    }

    private fun showNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        pendingIntent: PendingIntent
    ) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(notificationId, notification)
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(
            CHANNEL_QR,
            "Escaneos QR",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones cuando alguien escanea el QR de tu mascota"
            manager.createNotificationChannel(this)
        }

        NotificationChannel(
            CHANNEL_ALERT,
            "Alertas de zona",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertas cuando tu mascota sale de una zona segura"
            manager.createNotificationChannel(this)
        }
    }
}
