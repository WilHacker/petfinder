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
    private var socket: Socket? = null
    private val gson = Gson()

    // Canales reactivos para que el MapViewModel escuche los movimientos en vivo
    private val _petLocationFlow = MutableSharedFlow<PetLocationUpdate>(extraBufferCapacity = 1)
    val petLocationFlow = _petLocationFlow.asSharedFlow()

    private val _ownerLocationFlow = MutableSharedFlow<OwnerLocationUpdate>(extraBufferCapacity = 1)
    val ownerLocationFlow = _ownerLocationFlow.asSharedFlow()

    fun connect(jwtToken: String, context: Context) {
        if (socket?.connected() == true) return

        try {
            // Namespace /realtime definido en tu API_DOCUMENTATION.md
            val options = IO.Options.builder()
                .setTransports(arrayOf(io.socket.engineio.client.transports.WebSocket.NAME))
                .setAuth(mapOf("token" to "Bearer $jwtToken")) // Handshake con Token
                .build()

            socket = IO.socket("https://backend-petfinder.onrender.com/realtime", options)

            // --- LISTENERS DE EVENTOS ---

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketManager", "Conectado al ecosistema en tiempo real")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("SocketManager", "Error de conexión: ${args.firstOrNull()}")
            }

            // Movimiento de mascotas
            socket?.on("pet:location-updated") { args ->
                val data = args[0] as JSONObject
                val update = gson.fromJson(data.toString(), PetLocationUpdate::class.java)
                _petLocationFlow.tryEmit(update)
            }

            // Movimiento de co-propietarios
            socket?.on("owner:location-updated") { args ->
                val data = args[0] as JSONObject
                val update = gson.fromJson(data.toString(), OwnerLocationUpdate::class.java)
                _ownerLocationFlow.tryEmit(update)
            }

            // ALERTA CRÍTICA: Mascota abandona zona segura
            socket?.on("pet:exited-zone") { args ->
                val data = args[0] as JSONObject
                val alert = gson.fromJson(data.toString(), ZoneAlertEvent::class.java)
                dispararNotificacionPeligro(context, alert)
            }

            socket?.connect()

        } catch (e: Exception) {
            Log.e("SocketManager", "Fallo al inicializar Socket: ${e.message}")
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        Log.d("SocketManager", "Desconectado de WebSockets")
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