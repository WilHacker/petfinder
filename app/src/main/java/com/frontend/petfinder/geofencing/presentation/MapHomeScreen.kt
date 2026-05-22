package com.frontend.petfinder.geofencing.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import coil.imageLoader
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frontend.petfinder.core.presentation.components.DialogType
import com.frontend.petfinder.core.presentation.components.PetFinderDialog
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.utils.MapStyle
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

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapHomeScreen(
    mapViewModel: MapViewModel,
    onNavigateToProfile: () -> Unit = {},
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

    // IDs de mascotas con reporte activo — se renderizan con marcador rojo
    val desaparecidasIds = remember(snapshot) {
        snapshot?.marcadores?.desaparecidas?.mapTo(mutableSetOf()) { it.mascotaId } ?: emptySet()
    }

    val petsToDraw = remember(snapshot, livePetLocations) {
        val result = mutableMapOf<String, Pair<LatLng, String?>>()
        snapshot?.let { data ->
            data.zonas.forEach { zona ->
                val fallbackPos = when (zona.tipo) {
                    "circulo" -> zona.centro?.let { LatLng(it.lat, it.lng) }
                    "poligono" -> {
                        val coordinates = zona.geometria?.coordinates
                        if (!coordinates.isNullOrEmpty() && coordinates[0].isNotEmpty()) {
                            val firstPoint = coordinates[0][0]
                            LatLng(firstPoint[1], firstPoint[0])
                        } else null
                    }
                    else -> null
                }
                zona.mascotas?.forEach { pet ->
                    val livePos = livePetLocations[pet.mascotaId]
                    val explicitPos = pet.ubicacion?.let { LatLng(it.lat, it.lng) }
                    val finalPos = livePos ?: explicitPos ?: fallbackPos
                    if (finalPos != null) {
                        result[pet.mascotaId] = Pair(finalPos, pet.fotoUrl)
                    }
                }
            }
            // Desaparecidas con reporte: posición GPS exacta del reporte, sobreescribe fallback
            data.marcadores.desaparecidas.forEach { pet ->
                val livePos = livePetLocations[pet.mascotaId]
                val finalPos = livePos ?: LatLng(pet.lat, pet.lng)
                result[pet.mascotaId] = Pair(finalPos, pet.fotoUrl)
            }
        }
        result
    }

    var hasLocationPermission by remember { mutableStateOf(false) }
    var showAssignPetsDialog by remember { mutableStateOf(false) }
    var showPaseoDialog by remember { mutableStateOf(false) }
    var mascotaPaseoActivoId by remember { mutableStateOf<String?>(null) }

    // --- MANEJO DE ERRORES ---
    LaunchedEffect(trackingError) {
        trackingError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            mapViewModel.clearTrackingError()
        }
    }

    // --- MANEJO DE PERMISOS ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-17.3895, -66.1568), 13f)
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

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
                onMapClick = { latLng -> mapViewModel.handleMapClick(latLng) }
            ) {
                snapshot?.let { data ->

                    // 1. MARCADORES DE CO-PROPIETARIOS (Humanos)
                    data.marcadores.usuariosCompartidos.forEach { user ->
                        key(user.personaId) {
                            val livePos = liveOwnerLocations[user.personaId]
                            val finalPos = livePos ?: LatLng(user.lat, user.lng)
                            val customIcon = rememberCustomMarkerIcon(context, user.fotoUrl, Color(0xFF4CAF50))

                            Marker(
                                state = MarkerState(position = finalPos),
                                title = user.nombre,
                                icon = customIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            )
                        }
                    }

                    // 2. MARCADORES DE MASCOTAS
                    petsToDraw.forEach { (mascotaId, petData) ->
                        key(mascotaId) {
                            val (pos, fotoUrl) = petData
                            val isLost = mascotaId in desaparecidasIds
                            val borderColor = if (isLost) Color(0xFFE53935) else PrimaryOrange
                            val customIcon = rememberCustomMarkerIcon(context, fotoUrl, borderColor)

                            Marker(
                                state = MarkerState(position = pos),
                                title = if (isLost) "Extraviada" else "Mascota",
                                snippet = if (isLost) "Mascota extraviada" else null,
                                icon = customIcon ?: BitmapDescriptorFactory.defaultMarker(
                                    if (isLost) BitmapDescriptorFactory.HUE_RED
                                    else BitmapDescriptorFactory.HUE_ORANGE
                                )
                            )
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

                // 4. MASCOTAS PERDIDAS PÚBLICAS — excluye las que ya están en petsToDraw
                if (showLostPets) {
                    lostPets.filter { it.mascotaId !in petsToDraw }.forEach { lost ->
                        key("lost_${lost.mascotaId}") {
                            val pos = LatLng(lost.lat, lost.lng)
                            val customIcon = rememberCustomMarkerIcon(context, lost.fotoUrl, Color(0xFFE53935))
                            Marker(
                                state = MarkerState(position = pos),
                                title = lost.nombre,
                                snippet = "Extraviada · ${lost.tipo}",
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

            // --- BARRA SUPERIOR ---
            // Fila única: [👤 Perfil] ··· [Perdidas] [Admin] ··· [📍 Mi ubicación]
            if (!isDrawingMode) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Perfil — izquierda
                    SmallFloatingActionButton(
                        onClick = onNavigateToProfile,
                        containerColor = Color.White,
                        contentColor = PrimaryOrange,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Mi perfil")
                    }

                    // Chips centrales — Perdidas + Admin
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Chip Perdidas
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

                        // Chip Admin (solo si aplica)
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

                    // Mi ubicación — derecha (reemplaza el botón nativo de Google)
                    SmallFloatingActionButton(
                        onClick = {
                            cameraPositionState.move(
                                CameraUpdateFactory.newLatLngZoom(LatLng(-17.3895, -66.1568), 14f)
                            )
                        },
                        containerColor = Color.White,
                        contentColor = PrimaryOrange,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
                    }
                }

                // FAB Paseo — debajo de la barra superior, lado derecho
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 72.dp, end = 12.dp)
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
                                if (isTracking) Icons.Default.Stop else Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null
                            )
                        },
                        text = { Text(if (isTracking) "Detener" else "Paseo", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // FABs de dibujo — esquina inferior derecha (solo cuando no está en modo dibujo ni tracking)
            if (!isDrawingMode && !isTracking) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 140.dp, start = 24.dp, end = 24.dp)
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
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 32.dp).fillMaxWidth(),
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
                    selectedId?.let {
                        mascotaPaseoActivoId = it
                        mapViewModel.togglePaseo(context, it)
                        showPaseoDialog = false
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