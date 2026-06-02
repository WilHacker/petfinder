package com.frontend.petfinder.geofencing.presentation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.SocketManager
import com.frontend.petfinder.core.network.toPrismaMessage
import com.frontend.petfinder.core.service.LocationTrackingService
import com.frontend.petfinder.core.service.TrackingManager
import com.frontend.petfinder.core.utils.PermissionHandler
import com.frontend.petfinder.geofencing.data.*
import com.frontend.petfinder.geofencing.data.GeofencingRepository
import com.frontend.petfinder.pets.data.PetRepository
import com.frontend.petfinder.pets.data.dto.PetListItemDto
import com.frontend.petfinder.pets.data.dto.TipoMascotaDto
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "MapViewModel"

class MapViewModel : ViewModel() {

    /** Última posición de cámara del mapa — sobrevive al cambio de tab (no a la muerte del proceso). */
    var savedCameraPosition: CameraPosition? = null
        private set

    /** True una vez que el mapa se centró en el usuario (solo en el primer arranque). */
    var hasCenteredOnUser: Boolean = false
        private set

    fun saveCameraPosition(position: CameraPosition) { savedCameraPosition = position }
    fun markCenteredOnUser() { hasCenteredOnUser = true }

    private val _snapshot = MutableStateFlow<MapSnapshotResponse?>(null)
    val snapshot: StateFlow<MapSnapshotResponse?> = _snapshot.asStateFlow()

    private val _pets = MutableStateFlow<List<PetListItemDto>>(emptyList())
    val pets: StateFlow<List<PetListItemDto>> = _pets.asStateFlow()

    private val _livePetLocations = MutableStateFlow<Map<String, LatLng>>(emptyMap())
    val livePetLocations: StateFlow<Map<String, LatLng>> = _livePetLocations.asStateFlow()

    private val _liveOwnerLocations = MutableStateFlow<Map<String, LatLng>>(emptyMap())
    val liveOwnerLocations: StateFlow<Map<String, LatLng>> = _liveOwnerLocations.asStateFlow()

    private val _isDrawingMode = MutableStateFlow(false)
    val isDrawingMode: StateFlow<Boolean> = _isDrawingMode.asStateFlow()

    private val _drawingType = MutableStateFlow("circulo")
    val drawingType: StateFlow<String> = _drawingType.asStateFlow()

    private val _tempCircleCenter = MutableStateFlow<LatLng?>(null)
    val tempCircleCenter: StateFlow<LatLng?> = _tempCircleCenter.asStateFlow()

    private val _tempPolygonPoints = MutableStateFlow<List<LatLng>>(emptyList())
    val tempPolygonPoints: StateFlow<List<LatLng>> = _tempPolygonPoints.asStateFlow()

    private val _circleRadius = MutableStateFlow(80.0)
    val circleRadius: StateFlow<Double> = _circleRadius.asStateFlow()

    private val _userZones = MutableStateFlow<List<ZoneWithPetsDto>>(emptyList())
    val userZones: StateFlow<List<ZoneWithPetsDto>> = _userZones.asStateFlow()

    private val _isZonesLoading = MutableStateFlow(false)
    val isZonesLoading: StateFlow<Boolean> = _isZonesLoading.asStateFlow()

    private val _lostPets = MutableStateFlow<List<LostPetMarkerDto>>(emptyList())
    val lostPets: StateFlow<List<LostPetMarkerDto>> = _lostPets.asStateFlow()

    private val _showLostPets = MutableStateFlow(false)
    val showLostPets: StateFlow<Boolean> = _showLostPets.asStateFlow()

    fun toggleLostPetsLayer() {
        _showLostPets.value = !_showLostPets.value
    }

    // Alerta comunitaria "Pedir ayuda" — mascotas con búsqueda activa, visibles para todos
    data class ActiveCommunityAlert(
        val mascotaId: String,
        val lat: Double,
        val lng: Double,
        val radioMetros: Double?, // null cuando proviene del snapshot (no incluye radio)
        val expiraEl: String?,
        val fotoUrl: String? = null // se completa al sembrar desde perdidas; null si solo vino por socket
    )

    private val _activeAlerts = MutableStateFlow<Map<String, ActiveCommunityAlert>>(emptyMap())
    val activeAlerts: StateFlow<Map<String, ActiveCommunityAlert>> = _activeAlerts.asStateFlow()

    // expiraEl viene ISO-8601 (ej. "2026-06-02T02:08:51Z"). Sin fecha → la tratamos como activa.
    private fun isAlertActive(expiraEl: String?): Boolean {
        if (expiraEl == null) return true
        return runCatching {
            java.time.Instant.parse(expiraEl).isAfter(java.time.Instant.now())
        }.getOrDefault(true)
    }

    // Reconstruye la capa de alertas. IMPORTANTE: las alertas son de alta prioridad y
    // deben verse sin importar el filtro de especie de la UI. Por eso se siembran desde
    // perdidas SIN filtrar (no desde _lostPets, que viene filtrado por el backend).
    // Preserva el radio de cualquier alerta recibida en vivo por socket.
    private suspend fun rebuildActiveAlertsFromData() {
        // Cuando no hay filtro, _lostPets ya es la lista completa; si hay filtro, pedimos sin filtrar.
        val perdidasSinFiltrar = if (_speciesFilter.value == null) {
            _lostPets.value
        } else {
            GeofencingRepository.getPublicLostPets(null).getOrDefault(_lostPets.value)
        }

        val seeded = mutableMapOf<String, ActiveCommunityAlert>()
        _snapshot.value?.desaparecidas?.forEach { d ->
            val a = d.alertaComunidad
            if (a?.activa == true && isAlertActive(a.expiraEl)) {
                seeded[d.mascotaId] = ActiveCommunityAlert(
                    d.mascotaId, d.ubicacion.lat, d.ubicacion.lng, null, a.expiraEl, d.fotoUrl
                )
            }
        }
        perdidasSinFiltrar.forEach { l ->
            val a = l.alertaComunidad
            if (a?.activa == true && isAlertActive(a.expiraEl) && l.mascotaId !in seeded) {
                seeded[l.mascotaId] = ActiveCommunityAlert(
                    l.mascotaId, l.ubicacion.lat, l.ubicacion.lng, null, a.expiraEl, l.fotoUrl
                )
            }
        }

        // Mascotas que el backend reporta EXPLÍCITAMENTE con alerta inactiva (recuperada o
        // alerta apagada). Se quitan del mapa aunque sigan dentro de la ventana de 24h.
        val explicitamenteInactivas = buildSet {
            _snapshot.value?.desaparecidas?.forEach { d ->
                if (d.alertaComunidad?.activa == false) add(d.mascotaId)
            }
            perdidasSinFiltrar.forEach { l ->
                if (l.alertaComunidad?.activa == false) add(l.mascotaId)
            }
        }

        _activeAlerts.update { live ->
            val merged = seeded.mapValues { (id, seed) ->
                val radioVivo = live[id]?.radioMetros
                if (radioVivo != null) seed.copy(radioMetros = radioVivo) else seed
            }.toMutableMap()
            // Conserva alertas en vivo que aún no aparecen en perdidas (carrera socket→datos),
            // salvo que el backend las marque explícitamente como inactivas.
            live.forEach { (id, alert) ->
                if (id !in merged && id !in explicitamenteInactivas && isAlertActive(alert.expiraEl)) {
                    merged[id] = alert
                }
            }
            merged
        }
    }

    private val _tiposMascota = MutableStateFlow<List<TipoMascotaDto>>(emptyList())
    val tiposMascota: StateFlow<List<TipoMascotaDto>> = _tiposMascota.asStateFlow()

    private val _speciesFilter = MutableStateFlow<Int?>(null)
    val speciesFilter: StateFlow<Int?> = _speciesFilter.asStateFlow()

    fun setSpeciesFilter(tipoId: Int?) {
        _speciesFilter.value = tipoId
        cargarDatosDelMapa()
    }

    private val _zoneExitAlert = MutableStateFlow<ZoneExitAlertUi?>(null)
    val zoneExitAlert: StateFlow<ZoneExitAlertUi?> = _zoneExitAlert.asStateFlow()

    data class ZoneExitAlertUi(
        val mascotaId: String,
        val zonaId: Int,
        val petName: String,
        val zoneName: String
    )

    fun dismissZoneAlert() {
        _zoneExitAlert.value = null
    }

    val isTracking: StateFlow<Boolean> = TrackingManager.isTracking

    private val _trackingError = MutableStateFlow<String?>(null)
    val trackingError: StateFlow<String?> = _trackingError.asStateFlow()

    init {
        cargarDatosDelMapa()
        escucharEventosEnTiempoReal()
        loadTiposMascota()
    }

    private fun loadTiposMascota() {
        viewModelScope.launch {
            PetRepository.getTiposMascota().onSuccess { _tiposMascota.value = it }
        }
    }

    private fun escucharEventosEnTiempoReal() {
        // Resync al (re)conectar: el cliente no soporta Connection State Recovery, así que
        // cada reconexión es sesión nueva y los eventos perdidos no se reenvían. Recargamos
        // el mapa por REST. Saltamos la PRIMERA conexión (el init ya hizo la carga inicial).
        viewModelScope.launch {
            var primeraConexion = true
            SocketManager.connectionFlow.collect {
                if (primeraConexion) { primeraConexion = false; return@collect }
                Log.d(TAG, "Reconexión detectada → re-sincronizando mapa")
                cargarDatosDelMapa()
            }
        }

        viewModelScope.launch {
            SocketManager.petLocationFlow.collect { update ->
                _livePetLocations.update { it + (update.mascotaId to LatLng(update.lat, update.lng)) }
            }
        }

        viewModelScope.launch {
            SocketManager.petStatusFlow.collect { update ->
                // Solo afecta a MIS mascotas (evento room-scoped). El mapa público se actualiza
                // por separado con map:lost-pet-added / map:lost-pet-removed (broadcast).
                _snapshot.update { current ->
                    current?.copy(
                        misMascotas = current.misMascotas.map { pet ->
                            if (pet.mascotaId == update.mascotaId) pet.copy(estado = update.estado)
                            else pet
                        }
                    )
                }
                // Card abierta: actualiza el badge de estado en vivo
                patchOpenPetCard(update.mascotaId) { it.copy(estado = update.estado) }
                GeofencingRepository.getMapSnapshot(_speciesFilter.value).fold(
                    onSuccess = { _snapshot.value = it },
                    onFailure = { e -> Log.w(TAG, "petStatusFlow refresh: ${e.message}") }
                )
            }
        }

        viewModelScope.launch {
            SocketManager.ownerLocationFlow.collect { update ->
                _liveOwnerLocations.update { it + (update.personaId to LatLng(update.lat, update.lng)) }
                // Card de colaborador abierta: actualiza su ubicación en vivo
                patchOpenCollaboratorCard(update.personaId) {
                    it.copy(ubicacion = PointDto(update.lat, update.lng))
                }
            }
        }

        viewModelScope.launch {
            SocketManager.petProfileFlow.collect { update ->
                _snapshot.update { current ->
                    current?.copy(
                        misMascotas = current.misMascotas.map { pet ->
                            if (pet.mascotaId == update.mascotaId) pet.copy(
                                nombre = update.nombre ?: pet.nombre,
                                estado = update.estado ?: pet.estado,
                                fotoUrl = update.fotoUrl ?: pet.fotoUrl
                            ) else pet
                        }
                    )
                }
                // Card abierta: actualiza nombre/estado/foto en vivo
                patchOpenPetCard(update.mascotaId) { c ->
                    c.copy(
                        nombre = update.nombre ?: c.nombre,
                        estado = update.estado ?: c.estado,
                        fotos = if (update.fotoUrl != null)
                            listOf(MapCardFotoDto(fotoUrl = update.fotoUrl, esPrincipal = true))
                        else c.fotos
                    )
                }
            }
        }
        viewModelScope.launch {
            SocketManager.communityAlertFlow.collect { ev ->
                // Pinta la alerta al instante (con su radio). No recargamos: el backend ya
                // envía map:lost-pet-added por separado y la capa de alertas es always-on.
                _activeAlerts.update { current ->
                    val previa = current[ev.mascotaId]
                    current + (ev.mascotaId to ActiveCommunityAlert(
                        ev.mascotaId, ev.lat, ev.lng, ev.radioMetros, ev.expiraEl,
                        fotoUrl = previa?.fotoUrl // conserva foto si ya la teníamos
                    ))
                }
            }
        }

        // map:lost-pet-added — broadcast: agrega el pin sin llamar a ningún endpoint
        viewModelScope.launch {
            SocketManager.lostPetAddedFlow.collect { ev ->
                val marker = LostPetMarkerDto(
                    mascotaId = ev.mascotaId,
                    nombre = ev.nombre,
                    tipo = ev.tipo ?: "",
                    fotoUrl = ev.fotoUrl,
                    ubicacion = PointDto(ev.lat, ev.lng),
                    fechaPerdida = ev.fechaPerdida,
                    recompensa = ev.recompensa,
                    alertaComunidad = null
                )
                _lostPets.update { current ->
                    if (current.any { it.mascotaId == ev.mascotaId }) current
                    else current + marker
                }
            }
        }

        // map:lost-pet-removed — broadcast: quita el pin (recuperada) por mascotaId
        viewModelScope.launch {
            SocketManager.lostPetRemovedFlow.collect { ev ->
                _lostPets.update { it.filterNot { p -> p.mascotaId == ev.mascotaId } }
                _snapshot.update { s ->
                    s?.copy(desaparecidas = s.desaparecidas.filterNot { it.mascotaId == ev.mascotaId })
                }
                _activeAlerts.update { it - ev.mascotaId }
            }
        }

        viewModelScope.launch {
            SocketManager.zoneExitFlow.collect { event ->
                val petName = _pets.value.find { it.mascotaId == event.mascotaId }?.nombre
                    ?: _snapshot.value?.misMascotas
                        ?.find { it.mascotaId == event.mascotaId }?.nombre
                    ?: "Tu mascota"
                val zoneName = _snapshot.value?.zonas
                    ?.find { it.zonaId == event.zonaId }
                    ?.let { it.nombre ?: it.nombreZona }
                    ?: _userZones.value.find { it.zonaId == event.zonaId }?.nombreZona
                    ?: "la zona segura"
                _zoneExitAlert.value = ZoneExitAlertUi(
                    mascotaId = event.mascotaId,
                    zonaId = event.zonaId,
                    petName = petName,
                    zoneName = zoneName
                )
            }
        }
    }

    fun cargarDatosDelMapa() {
        viewModelScope.launch {
            val filter = _speciesFilter.value
            val snapshotJob = async { GeofencingRepository.getMapSnapshot(filter) }
            val petsJob     = async { PetRepository.getMyPets() }
            val lostPetsJob = async { GeofencingRepository.getPublicLostPets(filter) }

            snapshotJob.await().fold(
                onSuccess = { _snapshot.value = it },
                onFailure = { e ->
                    Log.w(TAG, "getMapSnapshot: ${e.message}")
                    _trackingError.value = "No se pudo cargar el mapa. Verifica tu conexión."
                }
            )
            petsJob.await().fold(
                onSuccess = { _pets.value = it },
                onFailure = { e -> Log.w(TAG, "getMyPets: ${e.message}") }
            )
            lostPetsJob.await().fold(
                onSuccess = { _lostPets.value = it },
                onFailure = { e -> Log.w(TAG, "getPublicLostPets: ${e.message}") }
            )

            rebuildActiveAlertsFromData()
        }
    }

    fun clearTrackingError() {
        _trackingError.value = null
    }

    fun togglePaseo(context: Context, mascotaId: String) {
        viewModelScope.launch {
            if (isTracking.value) {
                PetRepository.updatePetStatus(mascotaId, "en_casa").fold(
                    onSuccess = {
                        detenerServicioRastreo(context)
                        cargarDatosDelMapa()
                    },
                    onFailure = { e ->
                        Log.e(TAG, "togglePaseo stop: ${e.message}", e)
                        _trackingError.value = "No se pudo detener el paseo"
                    }
                )
            } else {
                if (!PermissionHandler.isReadyForTracking(context)) {
                    _trackingError.value = "Faltan permisos de ubicación"
                    return@launch
                }
                PetRepository.updatePetStatus(mascotaId, "en_paseo").fold(
                    onSuccess = {
                        iniciarServicioRastreo(context)
                        cargarDatosDelMapa()
                    },
                    onFailure = { e ->
                        Log.e(TAG, "togglePaseo start: ${e.message}", e)
                        _trackingError.value = "No se pudo iniciar el paseo"
                    }
                )
            }
        }
    }

    private fun iniciarServicioRastreo(context: Context) {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(intent)
        else
            context.startService(intent)
    }

    private fun detenerServicioRastreo(context: Context) {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun startDrawing(tipo: String) {
        _drawingType.value = tipo
        _isDrawingMode.value = true
        _tempCircleCenter.value = null
        _tempPolygonPoints.value = emptyList()
        _circleRadius.value = 80.0
    }

    fun cancelDrawing() {
        _isDrawingMode.value = false
    }

    fun handleMapClick(latLng: LatLng) {
        if (!_isDrawingMode.value) return
        if (_drawingType.value == "circulo") {
            _tempCircleCenter.value = latLng
        } else {
            _tempPolygonPoints.value = _tempPolygonPoints.value + latLng
        }
    }

    fun undoLastPolygonPoint() {
        if (_tempPolygonPoints.value.isNotEmpty())
            _tempPolygonPoints.value = _tempPolygonPoints.value.dropLast(1)
    }

    fun updateCircleRadius(radius: Double) {
        _circleRadius.value = radius
    }

    fun saveZoneWithMultiplePets(nombreZona: String, selectedPetIds: List<String>) {
        if (selectedPetIds.isEmpty()) return
        val primaryPetId = selectedPetIds.first()

        viewModelScope.launch {
            val request = if (_drawingType.value == "circulo") {
                val center = _tempCircleCenter.value ?: return@launch
                CreateZoneRequest(
                    nombreZona = nombreZona,
                    tipo = "circulo",
                    lat = center.latitude,
                    lng = center.longitude,
                    radioMetros = _circleRadius.value,
                    mascotaIds = selectedPetIds
                )
            } else {
                val points = _tempPolygonPoints.value
                if (points.size < 3) return@launch
                val coordenadas = (points + points.first()).map {
                    PointDto(lat = it.latitude, lng = it.longitude)
                }
                CreateZoneRequest(
                    nombreZona = nombreZona,
                    tipo = "poligono",
                    coordenadas = coordenadas,
                    mascotaIds = selectedPetIds
                )
            }

            GeofencingRepository.createZone(primaryPetId, request).fold(
                onSuccess = {
                    cancelDrawing()
                    cargarDatosDelMapa()
                    loadAllUserZones()
                },
                onFailure = { e ->
                    Log.w(TAG, "saveZoneWithMultiplePets: ${e.message}")
                    _trackingError.value = "No se pudo guardar la zona"
                }
            )
        }
    }

    fun loadAllUserZones() {
        viewModelScope.launch {
            _isZonesLoading.value = true
            GeofencingRepository.getAllUserZones().fold(
                onSuccess = { _userZones.value = it },
                onFailure = { e ->
                    Log.w(TAG, "loadAllUserZones: ${e.message}")
                    _userZones.value = emptyList()
                }
            )
            _isZonesLoading.value = false
        }
    }

    fun toggleZoneState(zonaId: Int, currentActiveState: Boolean) {
        viewModelScope.launch {
            GeofencingRepository.updateZone(zonaId, UpdateZoneRequest(estaActiva = !currentActiveState)).fold(
                onSuccess = {
                    loadAllUserZones()
                    cargarDatosDelMapa()
                },
                onFailure = { e ->
                    Log.w(TAG, "toggleZoneState: ${e.message}")
                    _trackingError.value = "No se pudo actualizar la zona"
                }
            )
        }
    }

    fun deleteZone(zonaId: Int) {
        viewModelScope.launch {
            GeofencingRepository.deleteZone(zonaId).fold(
                onSuccess = {
                    loadAllUserZones()
                    cargarDatosDelMapa()
                },
                onFailure = { e ->
                    Log.w(TAG, "deleteZone: ${e.message}")
                    _trackingError.value = "No se pudo eliminar la zona"
                }
            )
        }
    }

    private val _focusMascotaId = MutableStateFlow<String?>(null)
    val focusMascotaId: StateFlow<String?> = _focusMascotaId.asStateFlow()

    fun focusOnPet(mascotaId: String) {
        _focusMascotaId.value = mascotaId
    }

    fun clearFocus() {
        _focusMascotaId.value = null
    }

    // ── CARD DE DETALLE DEL MAPA (al tocar un pin) ──────────────────────────────
    sealed class MapCardState {
        object None : MapCardState()
        object Loading : MapCardState()
        data class Pet(val card: MapPetCardDto) : MapCardState()
        data class Collaborator(val card: MapCollaboratorCardDto) : MapCardState()
        data class Error(val message: String) : MapCardState()
    }

    private val _mapCard = MutableStateFlow<MapCardState>(MapCardState.None)
    val mapCard: StateFlow<MapCardState> = _mapCard.asStateFlow()

    fun onPetPinTapped(mascotaId: String) {
        viewModelScope.launch {
            _mapCard.value = MapCardState.Loading
            GeofencingRepository.getMapPetCard(mascotaId).fold(
                onSuccess = { _mapCard.value = MapCardState.Pet(it) },
                onFailure = { e ->
                    _mapCard.value = MapCardState.Error(
                        e.toPrismaMessage() ?: "No se pudo cargar la información de la mascota."
                    )
                }
            )
        }
    }

    fun onCollaboratorPinTapped(personaId: String) {
        viewModelScope.launch {
            _mapCard.value = MapCardState.Loading
            GeofencingRepository.getMapCollaboratorCard(personaId).fold(
                onSuccess = { _mapCard.value = MapCardState.Collaborator(it) },
                onFailure = { e ->
                    _mapCard.value = MapCardState.Error(
                        e.toPrismaMessage() ?: "No se pudo cargar la información del colaborador."
                    )
                }
            )
        }
    }

    fun dismissCard() {
        _mapCard.value = MapCardState.None
    }

    // Parchea la card de mascota abierta si coincide el id (para updates en vivo por WS).
    private fun patchOpenPetCard(mascotaId: String, transform: (MapPetCardDto) -> MapPetCardDto) {
        val current = _mapCard.value
        if (current is MapCardState.Pet && current.card.mascotaId == mascotaId) {
            _mapCard.value = MapCardState.Pet(transform(current.card))
        }
    }

    private fun patchOpenCollaboratorCard(
        personaId: String,
        transform: (MapCollaboratorCardDto) -> MapCollaboratorCardDto
    ) {
        val current = _mapCard.value
        if (current is MapCardState.Collaborator && current.card.personaId == personaId) {
            _mapCard.value = MapCardState.Collaborator(transform(current.card))
        }
    }
}
