package com.frontend.petfinder.core.network

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.frontend.petfinder.R
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

object SocketManager {
    @Volatile private var socket: Socket? = null
    private val gson = Gson()

    // Canales reactivos para que el MapViewModel escuche los movimientos en vivo
    private val _petLocationFlow = MutableSharedFlow<PetLocationUpdate>(extraBufferCapacity = 1)
    val petLocationFlow = _petLocationFlow.asSharedFlow()

    private val _ownerLocationFlow = MutableSharedFlow<OwnerLocationUpdate>(extraBufferCapacity = 1)
    val ownerLocationFlow = _ownerLocationFlow.asSharedFlow()

    // Alerta de zona — buffer mayor para no perder alertas críticas
    private val _zoneExitFlow = MutableSharedFlow<ZoneAlertEvent>(extraBufferCapacity = 8)
    val zoneExitFlow = _zoneExitFlow.asSharedFlow()

    fun connect(jwtToken: String, context: Context) {
        // Siempre limpia el socket anterior — garantiza listeners frescos y token actualizado
        disconnectClean()

        try {
            val options = IO.Options.builder()
                .setTransports(arrayOf(io.socket.engineio.client.transports.WebSocket.NAME))
                .setAuth(mapOf("token" to "Bearer $jwtToken"))
                .build()

            socket = IO.socket(AppConfig.WS_URL, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketManager", "Conectado al ecosistema en tiempo real")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("SocketManager", "Error de conexión: ${args.firstOrNull()}")
            }

            socket?.on("pet:location-updated") { args ->
                val data = args[0] as JSONObject
                val update = gson.fromJson(data.toString(), PetLocationUpdate::class.java)
                _petLocationFlow.tryEmit(update)
            }

            socket?.on("owner:location-updated") { args ->
                val data = args[0] as JSONObject
                val update = gson.fromJson(data.toString(), OwnerLocationUpdate::class.java)
                _ownerLocationFlow.tryEmit(update)
            }

            // ALERTA CRÍTICA: Mascota abandona zona segura
            socket?.on("pet:exited-zone") { args ->
                val data = args[0] as JSONObject
                val alert = gson.fromJson(data.toString(), ZoneAlertEvent::class.java)
                _zoneExitFlow.tryEmit(alert)
                dispararNotificacionPeligro(context, alert)
            }

            socket?.connect()

        } catch (e: Exception) {
            Log.e("SocketManager", "Fallo al inicializar Socket: ${e.message}")
            socket = null
        }
    }

    fun disconnect() {
        disconnectClean()
    }

    private fun disconnectClean() {
        socket?.let { s ->
            s.off(Socket.EVENT_CONNECT)
            s.off(Socket.EVENT_CONNECT_ERROR)
            s.off("pet:location-updated")
            s.off("owner:location-updated")
            s.off("pet:exited-zone")
            s.disconnect()
        }
        if (socket != null) {
            socket = null
            Log.d("SocketManager", "Socket desconectado y listeners eliminados")
        }
    }

    private fun dispararNotificacionPeligro(context: Context, alert: ZoneAlertEvent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, "petfinder_tracking_channel")
            .setContentTitle("¡ALERTA DE SEGURIDAD!")
            .setContentText("Tu mascota ha salido de la zona segura. Abre el mapa inmediatamente.")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Fuerza sonido y vibración
            .build()

        // Usamos la ID de zona combinada para no sobreescribir múltiples alertas
        manager.notify(alert.zonaId + 5000, notification)
    }
}
