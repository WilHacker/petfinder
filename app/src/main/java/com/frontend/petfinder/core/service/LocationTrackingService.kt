package com.frontend.petfinder.core.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.frontend.petfinder.R
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.profile.data.UserApi
import com.frontend.petfinder.profile.data.dto.LocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest as GmsLocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var userApi: UserApi

    companion object {
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
        private const val NOTIFICATION_CHANNEL_ID = "petfinder_tracking_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Instanciamos la API usando tu Singleton
        userApi = RetrofitClient.instance.create(UserApi::class.java)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    Log.d("Tracker", "Coordenadas: ${location.latitude}, ${location.longitude}")
                    enviarUbicacionAlBackend(location.latitude, location.longitude)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        // START_STICKY hace que si el OS mata el servicio por memoria, lo reinicie cuando pueda
        return START_STICKY
    }

    @SuppressLint("MissingPermission") // Se asume que la UI ya pidió los permisos antes de lanzar el Intent
    private fun startTracking() {
        if (TrackingManager.isTracking.value) return // Evitar dobles ejecuciones

        createNotificationChannel()

        // La notificación persistente (Obligatoria para Foreground Services)
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("PetFinder: Paseo Activo")
            .setContentText("Protegiendo a tu mascota en tiempo real...")
            .setSmallIcon(R.mipmap.ic_launcher_round) // Tu ícono actual
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        TrackingManager.setTrackingState(true)

        // Petición GPS: Alta precisión, aprox cada 15 segundos
        val locationRequest = GmsLocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000L)
            .setMinUpdateIntervalMillis(10000L)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun enviarUbicacionAlBackend(lat: Double, lng: Double) {
        serviceScope.launch {
            try {
                // Al hacer PUT aquí, tu backend NestJS emite los Sockets de Geofencing
                val response = userApi.updateLocation(LocationRequest(lat, lng))
                if (response.isSuccessful) {
                    Log.d("Tracker", "Ubicación sincronizada con Render")
                } else {
                    Log.e("Tracker", "Rechazo del backend: ${response.code()}")
                }
            } catch (e: Exception) {
                // Silenciamos el crash. Si el usuario entra a un túnel y pierde 4G,
                // la app no debe crashear, solo fallará esta petición y reintentará a los 15s.
                Log.e("Tracker", "Sin conexión para sincronizar GPS")
            }
        }
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        TrackingManager.setTrackingState(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Rastreo de Mascotas (PetFinder)",
                NotificationManager.IMPORTANCE_LOW // Low: Muestra ícono pero no hace sonar/vibrar el celular
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}