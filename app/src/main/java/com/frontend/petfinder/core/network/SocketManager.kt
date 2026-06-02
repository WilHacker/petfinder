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

    // El cliente Java fija el token (auth) al construir el socket; la reconexión interna
    // reusa ese token. Guardamos con qué token se construyó y el contexto para poder
    // recrear el socket con un token fresco si el JWT rota (ver EVENT_CONNECT_ERROR).
    @Volatile private var connectedWithToken: String? = null
    @Volatile private var appContext: Context? = null

    // Emite en cada (re)conexión establecida (EVENT_CONNECT). El cliente Java NO soporta
    // Connection State Recovery, así que cada reconexión es una sesión NUEVA: los eventos
    // perdidos no se reenvían. Quien escuche este flow debe re-sincronizar su estado por REST.
    private val _connectionFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val connectionFlow = _connectionFlow.asSharedFlow()

    // Canales reactivos para que el MapViewModel escuche los movimientos en vivo
    private val _petLocationFlow = MutableSharedFlow<PetLocationUpdate>(extraBufferCapacity = 1)
    val petLocationFlow = _petLocationFlow.asSharedFlow()

    private val _ownerLocationFlow = MutableSharedFlow<OwnerLocationUpdate>(extraBufferCapacity = 1)
    val ownerLocationFlow = _ownerLocationFlow.asSharedFlow()

    // Alerta de zona — buffer mayor para no perder alertas críticas
    private val _zoneExitFlow = MutableSharedFlow<ZoneAlertEvent>(extraBufferCapacity = 8)
    val zoneExitFlow = _zoneExitFlow.asSharedFlow()

    private val _petStatusFlow = MutableSharedFlow<PetStatusUpdate>(extraBufferCapacity = 4)
    val petStatusFlow = _petStatusFlow.asSharedFlow()

    private val _ownerProfileFlow = MutableSharedFlow<OwnerProfileUpdate>(extraBufferCapacity = 1)
    val ownerProfileFlow = _ownerProfileFlow.asSharedFlow()

    private val _petProfileFlow = MutableSharedFlow<PetProfileUpdate>(extraBufferCapacity = 4)
    val petProfileFlow = _petProfileFlow.asSharedFlow()

    private val _sightingNewFlow = MutableSharedFlow<SightingNewEvent>(extraBufferCapacity = 8)
    val sightingNewFlow = _sightingNewFlow.asSharedFlow()

    // Alerta comunitaria activada — broadcast a todos, buffer mayor para no perderla
    private val _communityAlertFlow = MutableSharedFlow<CommunityAlertActivatedEvent>(extraBufferCapacity = 8)
    val communityAlertFlow = _communityAlertFlow.asSharedFlow()

    // Pins del mapa público — broadcast: agregar/quitar mascotas perdidas en vivo
    private val _lostPetAddedFlow = MutableSharedFlow<MapLostPetAddedEvent>(extraBufferCapacity = 16)
    val lostPetAddedFlow = _lostPetAddedFlow.asSharedFlow()

    private val _lostPetRemovedFlow = MutableSharedFlow<MapLostPetRemovedEvent>(extraBufferCapacity = 16)
    val lostPetRemovedFlow = _lostPetRemovedFlow.asSharedFlow()

    // ── Chat privado dueño ↔ rescatista ────────────────────────────────────────
    private val _chatInviteFlow = MutableSharedFlow<ChatInviteEvent>(extraBufferCapacity = 8)
    val chatInviteFlow = _chatInviteFlow.asSharedFlow()

    private val _chatAcceptedFlow = MutableSharedFlow<ChatAcceptedEvent>(extraBufferCapacity = 8)
    val chatAcceptedFlow = _chatAcceptedFlow.asSharedFlow()

    private val _chatDeclinedFlow = MutableSharedFlow<ChatDeclinedEvent>(extraBufferCapacity = 8)
    val chatDeclinedFlow = _chatDeclinedFlow.asSharedFlow()

    private val _chatMessageFlow = MutableSharedFlow<ChatMessageEvent>(extraBufferCapacity = 16)
    val chatMessageFlow = _chatMessageFlow.asSharedFlow()

    private val _chatUnreadCountFlow = MutableSharedFlow<ChatUnreadCountEvent>(extraBufferCapacity = 16)
    val chatUnreadCountFlow = _chatUnreadCountFlow.asSharedFlow()

    fun connect(jwtToken: String, context: Context) {
        disconnectClean()
        connectedWithToken = jwtToken
        appContext = context.applicationContext

        try {
            val options = IO.Options.builder()
                .setTransports(arrayOf(io.socket.engineio.client.transports.WebSocket.NAME))
                .setAuth(mapOf("token" to "Bearer $jwtToken"))
                // Reconexión automática robusta (el cliente Java reintenta solo ante caídas).
                .setReconnection(true)
                .setReconnectionAttempts(Integer.MAX_VALUE)
                .setReconnectionDelay(500)
                .setReconnectionDelayMax(5000)
                .build()

            socket = IO.socket(AppConfig.WS_URL, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketManager", "Conectado al ecosistema en tiempo real")
                // Cada (re)conexión = sesión nueva. Avisa para re-sincronizar estado por REST.
                _connectionFlow.tryEmit(Unit)
            }

            // Evento de bienvenida del backend: confirma autenticación + rooms unidos.
            socket?.on("connected") { args ->
                runCatching { (args[0] as JSONObject).getString("usuarioId") }
                    .onSuccess { Log.d("SocketManager", "[WS] connected — usuario en línea: $it") }
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("SocketManager", "Error de conexión: ${args.joinToString { it?.toString() ?: "null" }}")
                // Si el JWT rotó (el TokenAuthenticator HTTP ya refrescó cachedAccessToken),
                // el socket sigue reintentando con el token viejo → el backend lo rechaza en
                // bucle. Recreamos el socket con el token fresco SOLO si cambió, para no
                // romper el backoff de socket.io ante errores de red normales (mismo token).
                val fresh = com.frontend.petfinder.PetFinderApp.sessionManager.cachedAccessToken
                val ctx = appContext
                if (fresh != null && ctx != null && fresh != connectedWithToken) {
                    Log.w("SocketManager", "Token rotó → reconectando con token fresco")
                    connect(fresh, ctx)
                }
            }

            socket?.on(Socket.EVENT_DISCONNECT) { args ->
                Log.w("SocketManager", "Socket desconectado — motivo: ${args.joinToString { it?.toString() ?: "null" }}")
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

            socket?.on("pet:exited-zone") { args ->
                val data = args[0] as JSONObject
                val alert = gson.fromJson(data.toString(), ZoneAlertEvent::class.java)
                _zoneExitFlow.tryEmit(alert)
                dispararNotificacionPeligro(context, alert)
            }

            socket?.on("pet:status-changed") { args ->
                runCatching {
                    val data = args[0] as JSONObject
                    val update = gson.fromJson(data.toString(), PetStatusUpdate::class.java)
                    _petStatusFlow.tryEmit(update)
                }.onFailure { e ->
                    Log.e("SocketManager", "pet:status-changed parse error: ${e.message}")
                }
            }

            socket?.on("owner:profile-updated") { args ->
                val data = args[0] as JSONObject
                val update = gson.fromJson(data.toString(), OwnerProfileUpdate::class.java)
                _ownerProfileFlow.tryEmit(update)
            }

            socket?.on("pet:profile-updated") { args ->
                runCatching {
                    val data = args[0] as JSONObject
                    val update = gson.fromJson(data.toString(), PetProfileUpdate::class.java)
                    _petProfileFlow.tryEmit(update)
                }.onFailure { e ->
                    Log.e("SocketManager", "pet:profile-updated parse error: ${e.message}")
                }
            }

            socket?.on("sighting:new") { args ->
                runCatching {
                    val data = args[0] as JSONObject
                    val event = gson.fromJson(data.toString(), SightingNewEvent::class.java)
                    _sightingNewFlow.tryEmit(event)
                }.onFailure { e ->
                    Log.e("SocketManager", "sighting:new parse error: ${e.message}")
                }
            }

            socket?.on("community:alert-activated") { args ->
                runCatching {
                    val data = args[0] as JSONObject
                    val event = gson.fromJson(data.toString(), CommunityAlertActivatedEvent::class.java)
                    _communityAlertFlow.tryEmit(event)
                }.onFailure { e ->
                    Log.e("SocketManager", "community:alert-activated parse error: ${e.message}")
                }
            }

            socket?.on("map:lost-pet-added") { args ->
                runCatching {
                    val data = args[0] as JSONObject
                    val event = gson.fromJson(data.toString(), MapLostPetAddedEvent::class.java)
                    _lostPetAddedFlow.tryEmit(event)
                }.onFailure { e ->
                    Log.e("SocketManager", "map:lost-pet-added parse error: ${e.message}")
                }
            }

            socket?.on("map:lost-pet-removed") { args ->
                runCatching {
                    val data = args[0] as JSONObject
                    val event = gson.fromJson(data.toString(), MapLostPetRemovedEvent::class.java)
                    _lostPetRemovedFlow.tryEmit(event)
                }.onFailure { e ->
                    Log.e("SocketManager", "map:lost-pet-removed parse error: ${e.message}")
                }
            }

            socket?.on("chat:invite") { args ->
                runCatching {
                    val data = args[0] as JSONObject
                    _chatInviteFlow.tryEmit(gson.fromJson(data.toString(), ChatInviteEvent::class.java))
                }.onFailure { e -> Log.e("SocketManager", "chat:invite parse error: ${e.message}") }
            }

            socket?.on("chat:accepted") { args ->
                runCatching {
                    val data = args[0] as JSONObject
                    _chatAcceptedFlow.tryEmit(gson.fromJson(data.toString(), ChatAcceptedEvent::class.java))
                }.onFailure { e -> Log.e("SocketManager", "chat:accepted parse error: ${e.message}") }
            }

            socket?.on("chat:declined") { args ->
                runCatching {
                    val data = args[0] as JSONObject
                    _chatDeclinedFlow.tryEmit(gson.fromJson(data.toString(), ChatDeclinedEvent::class.java))
                }.onFailure { e -> Log.e("SocketManager", "chat:declined parse error: ${e.message}") }
            }

            socket?.on("chat:message") { args ->
                runCatching {
                    val data = args[0] as JSONObject
                    _chatMessageFlow.tryEmit(gson.fromJson(data.toString(), ChatMessageEvent::class.java))
                }.onFailure { e -> Log.e("SocketManager", "chat:message parse error: ${e.message}") }
            }

            socket?.on("chat:unread-count") { args ->
                runCatching {
                    val data = args[0] as JSONObject
                    _chatUnreadCountFlow.tryEmit(gson.fromJson(data.toString(), ChatUnreadCountEvent::class.java))
                }.onFailure { e -> Log.e("SocketManager", "chat:unread-count parse error: ${e.message}") }
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

    /**
     * Garantiza que el socket esté conectado. Si ya está conectado, no hace nada;
     * si está caído o nulo, (re)conecta con el token provisto (el más reciente).
     * Pensado para llamarse al volver la app a primer plano.
     */
    fun ensureConnected(jwtToken: String, context: Context) {
        val s = socket
        if (s != null && s.connected()) return
        connect(jwtToken, context)
    }

    private fun disconnectClean() {
        socket?.let { s ->
            s.off(Socket.EVENT_CONNECT)
            s.off("connected")
            s.off(Socket.EVENT_CONNECT_ERROR)
            s.off(Socket.EVENT_DISCONNECT)
            s.off("pet:location-updated")
            s.off("pet:status-changed")
            s.off("owner:location-updated")
            s.off("pet:exited-zone")
            s.off("owner:profile-updated")
            s.off("pet:profile-updated")
            s.off("sighting:new")
            s.off("community:alert-activated")
            s.off("map:lost-pet-added")
            s.off("map:lost-pet-removed")
            s.off("chat:invite")
            s.off("chat:accepted")
            s.off("chat:declined")
            s.off("chat:message")
            s.off("chat:unread-count")
            s.disconnect()
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
