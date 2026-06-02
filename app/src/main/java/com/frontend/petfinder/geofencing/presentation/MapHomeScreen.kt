package com.frontend.petfinder.geofencing.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import coil.imageLoader
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frontend.petfinder.core.presentation.components.DialogType
import com.frontend.petfinder.core.presentation.components.PetFinderDialog
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.utils.MapStyle
import com.frontend.petfinder.core.utils.PermissionHandler
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*

// --- GENERADOR DE AVATARES CIRCULARES PREMIUM ---
@Composable
fun rememberCustomMarkerIcon(context: Context, url: String?, borderColor: Color): BitmapDescriptor? {
    var markerIcon by remember(url, borderColor) { mutableStateOf<BitmapDescriptor?>(null) }

    LaunchedEffect(url, borderColor) {
        if (url == null) return@LaunchedEffect
        runCatching {
            val request = coil.request.ImageRequest.Builder(context)
                .data(url)
                .size(112, 112)
                .scale(coil.size.Scale.FILL)
                .allowHardware(false)
                .build()

            val result = context.imageLoader.execute(request)
            if (result !is coil.request.SuccessResult) return@runCatching

            // Safe cast: algunos formatos (WebP animado, SVG) no son BitmapDrawable
            val raw = (result.drawable as? android.graphics.drawable.BitmapDrawable)
                ?.bitmap ?: return@runCatching

            // Escala explícita para garantizar 112×112 sin importar el aspect ratio original
            val photoSize = 112
            val src = if (raw.width == photoSize && raw.height == photoSize) raw
                      else android.graphics.Bitmap.createScaledBitmap(raw, photoSize, photoSize, true)

            val markerSize = 132
            val center = markerSize / 2f       // 66f
            val photoRadius = photoSize / 2f   // 56f

            val output = android.graphics.Bitmap.createBitmap(
                markerSize, markerSize, android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(output)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

            // 1. Borde exterior de color
            paint.color = borderColor.toArgb()
            canvas.drawCircle(center, center, center, paint)

            // 2. Anillo blanco interior
            paint.color = android.graphics.Color.WHITE
            canvas.drawCircle(center, center, 60f, paint)

            // 3. Foto recortada en círculo exacto (clipPath evita bordes irregulares)
            val clipPath = android.graphics.Path().apply {
                addCircle(center, center, photoRadius, android.graphics.Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            val offset = (markerSize - photoSize) / 2f  // 10f
            canvas.drawBitmap(src, offset, offset, paint)
            canvas.restore()

            markerIcon = BitmapDescriptorFactory.fromBitmap(output)
        }
    }
    return markerIcon
}

// Datos de una alerta comunitaria cercana, listos para la hoja inferior
private data class NearbyAlertUi(
    val mascotaId: String,
    val nombre: String?,
    val fotoUrl: String?,
    val lat: Double,
    val lng: Double,
    val distanceM: Float?
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapHomeScreen(
    mapViewModel: MapViewModel,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    unreadChat: Int = 0,
    isAdmin: Boolean = false
) {
    val context = LocalContext.current

    // --- ESTADOS DEL VIEWMODEL ---
    val snapshot by mapViewModel.snapshot.collectAsStateWithLifecycle()
    val pets by mapViewModel.pets.collectAsStateWithLifecycle()
    val isDrawingMode by mapViewModel.isDrawingMode.collectAsStateWithLifecycle()
    val drawingType by mapViewModel.drawingType.collectAsStateWithLifecycle()
    val tempCircleCenter by mapViewModel.tempCircleCenter.collectAsStateWithLifecycle()
    val tempPolygonPoints by mapViewModel.tempPolygonPoints.collectAsStateWithLifecycle()
    val circleRadius by mapViewModel.circleRadius.collectAsStateWithLifecycle()
    val isTracking by mapViewModel.isTracking.collectAsStateWithLifecycle()
    val livePetLocations by mapViewModel.livePetLocations.collectAsStateWithLifecycle()
    val liveOwnerLocations by mapViewModel.liveOwnerLocations.collectAsStateWithLifecycle()
    val trackingError by mapViewModel.trackingError.collectAsStateWithLifecycle()
    val lostPets by mapViewModel.lostPets.collectAsStateWithLifecycle()
    val showLostPets by mapViewModel.showLostPets.collectAsStateWithLifecycle()
    val zoneExitAlert by mapViewModel.zoneExitAlert.collectAsStateWithLifecycle()
    val tiposMascota by mapViewModel.tiposMascota.collectAsStateWithLifecycle()
    val speciesFilter by mapViewModel.speciesFilter.collectAsStateWithLifecycle()
    val focusMascotaId by mapViewModel.focusMascotaId.collectAsStateWithLifecycle()
    val activeAlerts by mapViewModel.activeAlerts.collectAsStateWithLifecycle()

    // IDs de mis propias mascotas extraviadas (borde rojo en el marcador)
    val misExtraviadas = remember(snapshot) {
        snapshot?.misMascotas
            ?.filter { it.estado == "extraviada" }
            ?.map { it.mascotaId }
            ?.toSet() ?: emptySet()
    }

    // Mis mascotas (siempre visibles, independiente del botón Perdidas)
    val myPetsToDraw = remember(snapshot, livePetLocations) {
        val result = mutableMapOf<String, Triple<LatLng, String?, String>>()
        snapshot?.misMascotas?.forEach { pet ->
            val finalPos = livePetLocations[pet.mascotaId]
                ?: pet.ubicacion?.let { LatLng(it.lat, it.lng) }
            if (finalPos != null) {
                result[pet.mascotaId] = Triple(finalPos, pet.fotoUrl, pet.tipo)
            }
        }
        result
    }

    // Mascotas ajenas extraviadas del snapshot (controladas por botón Perdidas)
    val communitySnapshotLost = remember(snapshot, livePetLocations) {
        val result = mutableMapOf<String, Triple<LatLng, String?, String>>()
        snapshot?.desaparecidas?.forEach { pet ->
            val finalPos = livePetLocations[pet.mascotaId]
                ?: LatLng(pet.ubicacion.lat, pet.ubicacion.lng)
            result[pet.mascotaId] = Triple(finalPos, pet.fotoUrl, pet.tipo)
        }
        result
    }

    // Nombre del tipo seleccionado para comparar con los DTOs que usan String
    val selectedTipoNombre = remember(speciesFilter, tiposMascota) {
        tiposMascota.find { it.tipoId == speciesFilter }?.nombre
    }

    // Mis mascotas filtradas por especie
    val filteredMyPets = remember(myPetsToDraw, speciesFilter, selectedTipoNombre) {
        if (speciesFilter == null) myPetsToDraw
        else myPetsToDraw.filter { (_, t) -> t.third.equals(selectedTipoNombre, ignoreCase = true) }
    }

    // Ajenas del snapshot filtradas por especie (solo cuando showLostPets = true)
    val filteredCommunitySnapshot = remember(communitySnapshotLost, speciesFilter, selectedTipoNombre) {
        if (speciesFilter == null) communitySnapshotLost
        else communitySnapshotLost.filter { (_, t) -> t.third.equals(selectedTipoNombre, ignoreCase = true) }
    }

    // Perdidas públicas del endpoint separado, filtradas por especie
    val filteredLostPets = remember(lostPets, speciesFilter, selectedTipoNombre) {
        if (speciesFilter == null) lostPets
        else lostPets.filter { it.tipo.equals(selectedTipoNombre, ignoreCase = true) }
    }

    // Ubicación del usuario para decidir qué alertas comunitarias están "cerca"
    var userPosition by remember { mutableStateOf<LatLng?>(null) }

    // Alertas comunitarias dentro del radio del usuario → disparan el estado de alerta de pantalla.
    // community:alert-activated es broadcast a toda la ciudad, así que filtramos por cercanía
    // para no dejar el banner siempre encendido. Sin ubicación aún, no mostramos banner.
    val nearbyAlerts = remember(activeAlerts, userPosition) {
        val u = userPosition ?: return@remember emptyList<MapViewModel.ActiveCommunityAlert>()
        activeAlerts.values.filter { a ->
            val res = FloatArray(1)
            android.location.Location.distanceBetween(u.latitude, u.longitude, a.lat, a.lng, res)
            res[0] <= 10_000f // 10 km
        }
    }

    // Detalles (nombre, foto, distancia) de cada alerta cercana, para la hoja inferior
    val nearbyAlertDetails = remember(nearbyAlerts, snapshot, lostPets, userPosition) {
        nearbyAlerts.map { a ->
            val nombre = snapshot?.misMascotas?.find { it.mascotaId == a.mascotaId }?.nombre
                ?: snapshot?.desaparecidas?.find { it.mascotaId == a.mascotaId }?.nombre
                ?: lostPets.find { it.mascotaId == a.mascotaId }?.nombre
            val foto = a.fotoUrl
                ?: snapshot?.misMascotas?.find { it.mascotaId == a.mascotaId }?.fotoUrl
                ?: snapshot?.desaparecidas?.find { it.mascotaId == a.mascotaId }?.fotoUrl
                ?: lostPets.find { it.mascotaId == a.mascotaId }?.fotoUrl
            val dist = userPosition?.let { u ->
                val res = FloatArray(1)
                android.location.Location.distanceBetween(u.latitude, u.longitude, a.lat, a.lng, res)
                res[0]
            }
            NearbyAlertUi(a.mascotaId, nombre, foto, a.lat, a.lng, dist)
        }.sortedBy { it.distanceM ?: Float.MAX_VALUE }
    }

    var showAlertsSheet by remember { mutableStateOf(false) }
    val alertsSheetState = rememberModalBottomSheetState()

    // Mascota elegida desde la bocina: su pin queda FIJO hasta que el usuario lo cierra.
    var selectedAlertId by remember { mutableStateOf<String?>(null) }
    // Rebote breve solo de atención al elegir; luego el pin se queda quieto pero visible.
    var bounceSelected by remember { mutableStateOf(false) }
    LaunchedEffect(selectedAlertId) {
        if (selectedAlertId != null) {
            bounceSelected = true
            kotlinx.coroutines.delay(4500)
            bounceSelected = false
        }
    }
    // Al activar Perdidas se ven todas → la vista individual deja de tener sentido.
    LaunchedEffect(showLostPets) {
        if (showLostPets) selectedAlertId = null
    }

    var hasLocationPermission by remember { mutableStateOf(false) }
    var showAssignPetsDialog by remember { mutableStateOf(false) }
    var showPaseoDialog by remember { mutableStateOf(false) }
    var mascotaPaseoActivoId by remember { mutableStateOf<String?>(null) }
    var pendingPaseoMascotaId by remember { mutableStateOf<String?>(null) }
    var feedbackDialog by remember { mutableStateOf<Triple<DialogType, String, String>?>(null) }
    var isCenteredOnUser by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Refresca el mapa cada vez que la pantalla vuelve a primer plano. Mitiga la falta de
    // eventos en vivo cross-usuario (status-changed es room-scoped en el backend): al volver
    // a la app, las mascotas extraviadas/recuperadas se reflejan sin reiniciar.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mapViewModel.cargarDatosDelMapa()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // --- MANEJO DE ERRORES ---
    LaunchedEffect(trackingError) {
        trackingError?.let {
            feedbackDialog = Triple(DialogType.DANGER, "Error de rastreo", it)
            mapViewModel.clearTrackingError()
        }
    }

    feedbackDialog?.let { (type, title, message) ->
        PetFinderDialog(
            type = type,
            title = title,
            message = message,
            confirmText = "Entendido",
            onConfirm = { feedbackDialog = null },
            onDismiss = { feedbackDialog = null }
        )
    }

    // --- MANEJO DE PERMISOS ---
    // Construye el array de permisos necesarios una sola vez
    val requiredPermissions = remember {
        buildList<String> {
            addAll(PermissionHandler.locationPermissions)
            PermissionHandler.notificationPermission?.let { add(it) }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        hasLocationPermission = granted
        // Si hay un paseo pendiente y ahora tenemos todos los permisos, iniciarlo automáticamente
        if (PermissionHandler.isReadyForTracking(context)) {
            pendingPaseoMascotaId?.let { id ->
                mascotaPaseoActivoId = id
                mapViewModel.togglePaseo(context, id)
                pendingPaseoMascotaId = null
            }
        }
    }

    // mascotaId actualmente resaltado en el mapa (persiste hasta que el usuario toca el mapa)
    var highlightedMascotaId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions)
    }

    // Restaura la última posición vista; si es el primer arranque, parte del default
    val cameraPositionState = rememberCameraPositionState {
        position = mapViewModel.savedCameraPosition
            ?: CameraPosition.fromLatLngZoom(LatLng(-17.3895, -66.1568), 13f)
    }

    // Centra en la ubicación real del usuario SOLO la primera vez (primer arranque de la app).
    // En visitas posteriores se respeta la posición donde el usuario dejó el mapa.
    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@LaunchedEffect
        if (mapViewModel.hasCenteredOnUser) return@LaunchedEffect
        runCatching {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    isCenteredOnUser = true
                    mapViewModel.markCenteredOnUser()
                }
            }
        }
    }

    // Obtiene la ubicación del usuario (independiente del centrado) para calcular cercanía de alertas
    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@LaunchedEffect
        runCatching {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) userPosition = LatLng(loc.latitude, loc.longitude)
            }
        }
    }

    // Guarda la posición de cámara al abandonar el mapa, para restaurarla al volver
    DisposableEffect(Unit) {
        onDispose { mapViewModel.saveCameraPosition(cameraPositionState.position) }
    }

    // Cuando el usuario arrastra el mapa, el botón vuelve a su estado inactivo
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) isCenteredOnUser = false
    }

    // Centrar mapa en una mascota cuando se navega desde PetDetail
    LaunchedEffect(focusMascotaId) {
        val mascotaId = focusMascotaId ?: return@LaunchedEffect
        val latLng = livePetLocations[mascotaId]
            ?: snapshot?.misMascotas
                ?.find { it.mascotaId == mascotaId }
                ?.ubicacion
                ?.let { LatLng(it.lat, it.lng) }
        highlightedMascotaId = mascotaId
        if (latLng != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(latLng, 17f)
                ),
                durationMs = 800
            )
        }
        mapViewModel.clearFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                mapStyleOptions = MapStyleOptions(MapStyle.json)
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false
            ),
            contentPadding = PaddingValues(bottom = 100.dp),
            onMapClick = { latLng ->
                highlightedMascotaId = null
                mapViewModel.handleMapClick(latLng)
            }
        ) {
                // 0. ALERTAS COMUNITARIAS ACTIVAS — "se pidió ayuda aquí".
                //    El botón Perdidas controla el mapa: estas alertas solo se dibujan con
                //    Perdidas ON. Excepción: la mascota recién elegida en la bocina se muestra
                //    temporal aunque Perdidas esté OFF. Cuando se dibuja, tiene prioridad sobre
                //    las demás capas (que saltan estas mascotas para no duplicar el pin).
                if (activeAlerts.isNotEmpty()) {
                    activeAlerts.forEach { (mascotaId, alert) ->
                        if (!showLostPets && mascotaId != selectedAlertId) return@forEach
                        key("alert_$mascotaId") {
                            val center = LatLng(alert.lat, alert.lng)

                            // Radio de búsqueda (solo si lo conocemos vía socket en vivo)
                            alert.radioMetros?.let { r ->
                                Circle(
                                    center = center,
                                    radius = r,
                                    fillColor = Color(0x1AE53935),
                                    strokeColor = Color(0xFFE53935),
                                    strokeWidth = 4f
                                )
                            }

                            // Halo de énfasis bajo el pin
                            Circle(
                                center = center,
                                radius = 45.0,
                                fillColor = Color(0x33E53935),
                                strokeColor = Color(0xFFE53935),
                                strokeWidth = 3f
                            )

                            // Foto: alerta en vivo → mis mascotas → desaparecidas → perdidas públicas
                            val foto = alert.fotoUrl
                                ?: snapshot?.misMascotas?.find { it.mascotaId == mascotaId }?.fotoUrl
                                ?: snapshot?.desaparecidas?.find { it.mascotaId == mascotaId }?.fotoUrl
                                ?: lostPets.find { it.mascotaId == mascotaId }?.fotoUrl
                            val recompensa = snapshot?.misMascotas?.find { it.mascotaId == mascotaId }?.recompensa
                                ?: snapshot?.desaparecidas?.find { it.mascotaId == mascotaId }?.recompensa
                            val alertSnippet = buildString {
                                append("La comunidad está buscando a esta mascota")
                                if (recompensa != null && recompensa > 0)
                                    append(" · Recompensa: Bs. %.0f".format(recompensa))
                            }
                            val alertIcon = rememberCustomMarkerIcon(context, foto, Color(0xFFE53935))

                            // Por defecto el pin está quieto. Solo rebota la mascota que el
                            // usuario eligió desde la bocina, durante unos segundos.
                            val isSelected = mascotaId == selectedAlertId
                            val bounce = rememberInfiniteTransition(label = "alertBounce_$mascotaId")
                            val dy by bounce.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(450, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alertDy"
                            )
                            val markerPos = if (isSelected && bounceSelected)
                                LatLng(center.latitude + dy * 0.00008, center.longitude)
                            else center

                            Marker(
                                state = MarkerState(position = markerPos),
                                title = "🆘 Ayuda solicitada",
                                snippet = alertSnippet,
                                icon = alertIcon ?: BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_RED
                                )
                            )
                        }
                    }
                }

                snapshot?.let { data ->

                    // 1. MARCADORES DE CO-PROPIETARIOS (Humanos)
                    data.colaboradores.forEach { user ->
                        key(user.personaId) {
                            val livePos = liveOwnerLocations[user.personaId]
                            val finalPos = livePos ?: LatLng(user.ubicacion.lat, user.ubicacion.lng)
                            val customIcon = rememberCustomMarkerIcon(context, user.fotoUrl, Color(0xFF4CAF50))

                            Marker(
                                state = MarkerState(position = finalPos),
                                title = user.nombre,
                                icon = customIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            )
                        }
                    }

                    // 2. MIS MASCOTAS — siempre visibles (en_casa, en_paseo, extraviada)
                    filteredMyPets.forEach { (mascotaId, petData) ->
                        // Saltamos solo si el bloque 0 está dibujando su pin de "Ayuda solicitada"
                        // (con Perdidas ON o si fue la elegida en la bocina). Si no, la dibujamos
                        // normal para que MIS mascotas nunca desaparezcan del mapa.
                        if (mascotaId in activeAlerts && (showLostPets || mascotaId == selectedAlertId)) return@forEach
                        key(mascotaId) {
                            val (pos, fotoUrl, _) = petData
                            val isLost = mascotaId in misExtraviadas
                            val isHighlighted = mascotaId == highlightedMascotaId
                            val borderColor = when {
                                isHighlighted -> Color(0xFF1565C0)   // azul eléctrico al enfocarse
                                isLost        -> Color(0xFFE53935)
                                else          -> PrimaryOrange
                            }
                            val customIcon = rememberCustomMarkerIcon(context, fotoUrl, borderColor)
                            val recompensa = snapshot?.misMascotas
                                ?.find { it.mascotaId == mascotaId }?.recompensa
                            val snippetText = if (isLost) buildString {
                                append("Mi mascota extraviada")
                                if (recompensa != null && recompensa > 0)
                                    append(" · Recompensa: Bs. %.0f".format(recompensa))
                            } else null

                            // Aro pulsante debajo del marcador cuando está resaltado
                            if (isHighlighted) {
                                Circle(
                                    center = pos,
                                    radius = 30.0,
                                    fillColor = Color(0x331565C0),
                                    strokeColor = Color(0xFF1565C0),
                                    strokeWidth = 3f
                                )
                            }

                            Marker(
                                state = MarkerState(position = pos),
                                title = if (isHighlighted) "📍 Tu mascota está aquí"
                                        else if (isLost) "Extraviada"
                                        else "Mi mascota",
                                snippet = snippetText,
                                icon = customIcon ?: BitmapDescriptorFactory.defaultMarker(
                                    if (isLost) BitmapDescriptorFactory.HUE_RED
                                    else BitmapDescriptorFactory.HUE_ORANGE
                                )
                            )
                        }
                    }

                    // 2b. AJENAS DEL SNAPSHOT — solo cuando Perdidas está activo
                    if (showLostPets) {
                        filteredCommunitySnapshot.forEach { (mascotaId, petData) ->
                            // Con alerta activa ya se dibuja como "Ayuda solicitada" (bloque 0).
                            if (mascotaId in activeAlerts) return@forEach
                            key("snap_$mascotaId") {
                                val (pos, fotoUrl, _) = petData
                                val customIcon = rememberCustomMarkerIcon(context, fotoUrl, Color(0xFFE53935))
                                val recompensa = snapshot?.desaparecidas
                                    ?.find { it.mascotaId == mascotaId }?.recompensa
                                val snippetText = buildString {
                                    append("Mascota extraviada")
                                    if (recompensa != null && recompensa > 0)
                                        append(" · Recompensa: Bs. %.0f".format(recompensa))
                                }
                                Marker(
                                    state = MarkerState(position = pos),
                                    title = "Extraviada",
                                    snippet = snippetText,
                                    icon = customIcon ?: BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_RED
                                    )
                                )
                            }
                        }
                    }

                    // 3. ZONAS SEGURAS GUARDADAS
                    data.zonas.forEach { zona ->
                        when (zona.tipo) {
                            "circulo" -> {
                                zona.centro?.let { centro ->
                                    Circle(
                                        center = LatLng(centro.lat, centro.lng),
                                        radius = zona.radioMetros ?: 80.0,
                                        fillColor = Color(0x22F18A20),
                                        strokeColor = PrimaryOrange
                                    )
                                }
                            }
                            "poligono" -> {
                                val coordenadas = zona.geometria?.coordinates
                                if (!coordenadas.isNullOrEmpty() && coordenadas[0].isNotEmpty()) {
                                    val puntosZona = coordenadas[0].map { LatLng(it[1], it[0]) }
                                    Polygon(
                                        points = puntosZona,
                                        fillColor = Color(0x22F18A20),
                                        strokeColor = PrimaryOrange
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. MASCOTAS PERDIDAS PÚBLICAS (endpoint separado) — excluye las mías y las del snapshot
                if (showLostPets) {
                    // activeAlerts.keys: las que ya se dibujan como "Ayuda solicitada" (bloque 0).
                    val yaRenderizadas = filteredMyPets.keys + filteredCommunitySnapshot.keys + activeAlerts.keys
                    filteredLostPets.filter { it.mascotaId !in yaRenderizadas }.forEach { lost ->
                        key("lost_${lost.mascotaId}") {
                            val pos = LatLng(lost.ubicacion.lat, lost.ubicacion.lng)
                            val customIcon = rememberCustomMarkerIcon(context, lost.fotoUrl, Color(0xFFE53935))
                            val snippetParts = mutableListOf("Extraviada · ${lost.tipo}")
                            lost.recompensa?.let { if (it > 0) snippetParts.add("Recompensa: Bs. %.0f".format(it)) }
                            Marker(
                                state = MarkerState(position = pos),
                                title = lost.nombre,
                                snippet = snippetParts.joinToString(" · "),
                                icon = customIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            )
                        }
                    }
                }

                // 5. DIBUJO TEMPORAL
                if (isDrawingMode) {
                    if (drawingType == "circulo" && tempCircleCenter != null) {
                        Circle(
                            center = tempCircleCenter!!,
                            radius = circleRadius,
                            fillColor = Color(0x55F18A20),
                            strokeColor = PrimaryOrange
                        )
                    } else if (drawingType == "poligono" && tempPolygonPoints.isNotEmpty()) {
                        Polygon(
                            points = tempPolygonPoints,
                            fillColor = Color(0x55F18A20),
                            strokeColor = PrimaryOrange
                        )
                    }
                }
            }

            // --- CONTROLES SUPERIORES (modo normal) ---
            if (!isDrawingMode) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 8.dp)
                ) {
                    // Fila 1: [👤 Perfil] ··· [Perdidas][Admin?] ··· [🚶 Paseo][📍 Ubicación]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Izquierda: Perfil + Mensajes
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmallFloatingActionButton(
                                onClick = onNavigateToProfile,
                                containerColor = Color.White,
                                contentColor = PrimaryOrange,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Mi perfil")
                            }

                            SmallFloatingActionButton(
                                onClick = onNavigateToChat,
                                containerColor = Color.White,
                                contentColor = PrimaryOrange,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadChat > 0) {
                                            Badge(
                                                containerColor = Color(0xFFE53935),
                                                contentColor = Color.White
                                            ) {
                                                Text(if (unreadChat > 99) "99+" else "$unreadChat")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ChatBubble, contentDescription = "Mensajes")
                                }
                            }
                        }

                        // Centro: chips de Perdidas + Admin
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .shadow(6.dp, RoundedCornerShape(50.dp))
                                    .clickable { mapViewModel.toggleLostPetsLayer() },
                                shape = RoundedCornerShape(50.dp),
                                color = if (showLostPets) Color(0xFFE53935) else Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Mascotas perdidas",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (showLostPets) Color.White else Color(0xFFE53935)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Perdidas",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (showLostPets) Color.White else Color(0xFFE53935)
                                    )
                                }
                            }

                            if (isAdmin) {
                                Surface(
                                    modifier = Modifier.shadow(6.dp, RoundedCornerShape(50.dp)),
                                    shape = RoundedCornerShape(50.dp),
                                    color = Color(0xFF6200EE)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            "Admin",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Derecha: Paseo
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    if (isTracking) {
                                        mascotaPaseoActivoId?.let { mapViewModel.togglePaseo(context, it) }
                                        mascotaPaseoActivoId = null
                                    } else {
                                        showPaseoDialog = true
                                    }
                                },
                                containerColor = if (isTracking) MaterialTheme.colorScheme.error else PrimaryOrange,
                                contentColor = Color.White,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                                icon = {
                                    Icon(
                                        if (isTracking) Icons.Default.Stop
                                        else Icons.AutoMirrored.Filled.DirectionsWalk,
                                        contentDescription = null
                                    )
                                },
                                text = {
                                    Text(
                                        if (isTracking) "Detener" else "Paseo",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }

                    // Fila 2: filtros de especie — full width sin obstáculos
                    if (tiposMascota.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = speciesFilter == null,
                                    onClick = { mapViewModel.setSpeciesFilter(null) },
                                    label = {
                                        Text(
                                            "Todos",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryOrange,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            items(tiposMascota) { tipo ->
                                FilterChip(
                                    selected = speciesFilter == tipo.tipoId,
                                    onClick = {
                                        mapViewModel.setSpeciesFilter(
                                            if (speciesFilter == tipo.tipoId) null else tipo.tipoId
                                        )
                                    },
                                    label = {
                                        Text(
                                            tipo.nombre,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryOrange,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Bocina de alertas comunitarias cercanas — debajo de los filtros (bajo "Todos").
                    // Acceso a demanda; funciona independientemente del botón Perdidas.
                    if (nearbyAlerts.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .padding(start = 12.dp, top = 8.dp)
                                .shadow(6.dp, RoundedCornerShape(50.dp))
                                .clickable { showAlertsSheet = true },
                            shape = RoundedCornerShape(50.dp),
                            color = Color(0xFFE53935)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Campaign,
                                    contentDescription = "Alertas comunitarias cerca",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (nearbyAlerts.size == 1) "Alerta comunitaria"
                                    else "${nearbyAlerts.size} alertas comunitarias",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Chip para cerrar la vista individual de una alerta (Perdidas OFF).
                    if (selectedAlertId != null && !showLostPets) {
                        Surface(
                            modifier = Modifier
                                .padding(start = 12.dp, top = 8.dp)
                                .shadow(6.dp, RoundedCornerShape(50.dp))
                                .clickable { selectedAlertId = null },
                            shape = RoundedCornerShape(50.dp),
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cerrar vista",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFFE53935)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Cerrar vista",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333)
                                )
                            }
                        }
                    }

                }
            }

            // Hoja inferior: lista de TODAS las alertas comunitarias cercanas
            if (showAlertsSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAlertsSheet = false },
                    sheetState = alertsSheetState
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, null, tint = Color(0xFFE53935), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Alertas comunitarias cerca de ti",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${nearbyAlertDetails.size} mascotas necesitan ayuda en tu zona",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(nearbyAlertDetails, key = { it.mascotaId }) { item ->
                                val distLabel = item.distanceM?.let { d ->
                                    if (d < 1000f) "${d.toInt()} m" else "%.1f km".format(d / 1000f)
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFFEBEE)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (item.fotoUrl != null) {
                                            AsyncImage(
                                                model = item.fotoUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE53935)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Pets, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                item.nombre ?: "Mascota",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                buildString {
                                                    append("Necesita ayuda")
                                                    if (distLabel != null) append(" · a $distLabel")
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                cameraPositionState.move(
                                                    CameraUpdateFactory.newLatLngZoom(LatLng(item.lat, item.lng), 16f)
                                                )
                                                selectedAlertId = item.mascotaId // solo esta rebota
                                                showAlertsSheet = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Place, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Ir")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FABs de dibujo — esquina inferior derecha (solo cuando no está en modo dibujo ni tracking)
            if (!isDrawingMode && !isTracking) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(bottom = 100.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Brújula — rota con el mapa, toca para volver al norte
                    val bearing = cameraPositionState.position.bearing
                    if (bearing != 0f) {
                        SmallFloatingActionButton(
                            onClick = {
                                cameraPositionState.move(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.builder(cameraPositionState.position)
                                            .bearing(0f)
                                            .build()
                                    )
                                )
                            },
                            containerColor = Color.White,
                            contentColor = PrimaryOrange,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Explore,
                                contentDescription = "Orientar al norte",
                                modifier = Modifier.graphicsLayer { rotationZ = -bearing }
                            )
                        }
                    }

                    // Mi ubicación — recentra el mapa en el usuario
                    SmallFloatingActionButton(
                        onClick = {
                            if (hasLocationPermission) {
                                fusedLocationClient.lastLocation
                                    .addOnSuccessListener { location ->
                                        if (location != null) {
                                            cameraPositionState.move(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(location.latitude, location.longitude), 16f
                                                )
                                            )
                                            isCenteredOnUser = true
                                        } else {
                                            feedbackDialog = Triple(
                                                DialogType.INFO,
                                                "Ubicación no disponible",
                                                "Activa el GPS y vuelve a intentarlo."
                                            )
                                        }
                                    }
                            } else {
                                permissionLauncher.launch(requiredPermissions)
                            }
                        },
                        containerColor = if (isCenteredOnUser) PrimaryOrange else Color.White,
                        contentColor = if (isCenteredOnUser) Color.White else PrimaryOrange,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
                    }

                    SmallFloatingActionButton(
                        onClick = { mapViewModel.startDrawing("circulo") },
                        containerColor = Color.White,
                        contentColor = PrimaryOrange,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.AddLocation, contentDescription = "Dibujar círculo")
                    }

                    SmallFloatingActionButton(
                        onClick = { mapViewModel.startDrawing("poligono") },
                        containerColor = Color.White,
                        contentColor = PrimaryOrange,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.Polyline, contentDescription = "Dibujar polígono")
                    }
                }
            }

            if (isDrawingMode) {
                // Scrim oscuro sobre el mapa para resaltar los controles
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.30f))
                )

                // Instrucción de dibujo — centrada en la parte superior
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (drawingType == "circulo") Icons.Default.AddLocation else Icons.Default.Polyline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = PrimaryOrange
                            )
                            Text(
                                text = if (drawingType == "circulo") "Toca el mapa para fijar el centro" else "Toca para agregar puntos al polígono",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 200.dp, start = 24.dp, end = 24.dp)
                ) {
                    if (drawingType == "circulo" && tempCircleCenter != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Radio: ${circleRadius.toInt()} metros", fontWeight = FontWeight.Bold, color = PrimaryOrange)
                                Slider(
                                    value = circleRadius.toFloat(), onValueChange = { mapViewModel.updateCircleRadius(it.toDouble()) },
                                    valueRange = 20f..1000f, colors = SliderDefaults.colors(thumbColor = PrimaryOrange, activeTrackColor = PrimaryOrange)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 100.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(onClick = { mapViewModel.cancelDrawing() }, containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White) {
                        Icon(Icons.Default.Close, null)
                    }
                    if (drawingType == "poligono" && tempPolygonPoints.isNotEmpty()) {
                        FloatingActionButton(onClick = { mapViewModel.undoLastPolygonPoint() }) { Icon(Icons.AutoMirrored.Filled.Undo, null) }
                    }
                    val canSave = (drawingType == "circulo" && tempCircleCenter != null) || (drawingType == "poligono" && tempPolygonPoints.size >= 3)
                    if (canSave) {
                        FloatingActionButton(onClick = { showAssignPetsDialog = true }, containerColor = PrimaryOrange, contentColor = Color.White) {
                            Icon(Icons.Default.Check, null)
                        }
                    }
                }
            }

        // --- DIÁLOGOS ---
        if (showAssignPetsDialog) {
            var nombreZona by remember { mutableStateOf("") }
            val selectedPetIds = remember { mutableStateListOf<String>() }

            PetFinderDialog(
                type = DialogType.DEFAULT,
                title = "Guardar Zona Segura",
                confirmText = "Guardar Zona",
                dismissText = "Cancelar",
                onConfirm = {
                    mapViewModel.saveZoneWithMultiplePets(nombreZona, selectedPetIds)
                    showAssignPetsDialog = false
                },
                onDismiss = { showAssignPetsDialog = false },
                content = {
                    Column {
                        OutlinedTextField(
                            value = nombreZona,
                            onValueChange = { nombreZona = it },
                            label = { Text("Nombre (ej. Casa)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Asignar a:", fontWeight = FontWeight.Bold, color = PrimaryOrange)
                        LazyColumn(modifier = Modifier.height(180.dp)) {
                            items(pets, key = { it.mascotaId }) { pet ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (selectedPetIds.contains(pet.mascotaId)) selectedPetIds.remove(pet.mascotaId)
                                            else selectedPetIds.add(pet.mascotaId)
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedPetIds.contains(pet.mascotaId),
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = PrimaryOrange)
                                    )
                                    Text(pet.nombre, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                }
            )
        }

        // --- MODAL DE ALERTA DE ZONA (H19) ---
        zoneExitAlert?.let { alert ->
            PetFinderDialog(
                type = DialogType.DANGER,
                title = "¡Alerta de zona!",
                message = "${alert.petName} salió de ${alert.zoneName}. Abre el mapa para ver su ubicación actual.",
                confirmText = "Ver en mapa",
                dismissText = "Cerrar",
                onConfirm = { mapViewModel.dismissZoneAlert() },
                onDismiss = { mapViewModel.dismissZoneAlert() }
            )
        }

        if (showPaseoDialog) {
            var selectedId by remember { mutableStateOf<String?>(null) }
            PetFinderDialog(
                type = DialogType.DEFAULT,
                title = "¿Quién sale a pasear?",
                confirmText = "Comenzar",
                dismissText = "Cancelar",
                onConfirm = {
                    selectedId?.let { id ->
                        showPaseoDialog = false
                        if (PermissionHandler.isReadyForTracking(context)) {
                            mascotaPaseoActivoId = id
                            mapViewModel.togglePaseo(context, id)
                        } else {
                            // Guarda el ID y pide permisos — cuando se concedan se inicia automáticamente
                            pendingPaseoMascotaId = id
                            permissionLauncher.launch(requiredPermissions)
                        }
                    }
                },
                onDismiss = { showPaseoDialog = false },
                content = {
                    LazyColumn(modifier = Modifier.height(180.dp)) {
                        items(pets, key = { it.mascotaId }) { pet ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedId = pet.mascotaId }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedId == pet.mascotaId,
                                    onClick = { selectedId = pet.mascotaId },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryOrange)
                                )
                                Text(pet.nombre, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            )
        }
    }
}