package com.frontend.petfinder.geofencing.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.utils.MapStyle
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*

// --- GENERADOR DE AVATARES CIRCULARES PREMIUM ---
@Composable
fun rememberCustomMarkerIcon(context: Context, url: String?, borderColor: Color): BitmapDescriptor? {
    var markerIcon by remember(url) { mutableStateOf<BitmapDescriptor?>(null) }

    LaunchedEffect(url) {
        if (url != null) {
            val loader = coil.ImageLoader(context)
            val request = coil.request.ImageRequest.Builder(context)
                .data(url)
                .size(112, 112)
                .transformations(coil.transform.CircleCropTransformation())
                .allowHardware(false) // Necesario para dibujar en Canvas
                .build()

            val result = loader.execute(request)
            if (result is coil.request.SuccessResult) {
                val originalBitmap = (result.drawable as android.graphics.drawable.BitmapDrawable).bitmap

                val output = android.graphics.Bitmap.createBitmap(132, 132, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(output)
                val paint = android.graphics.Paint().apply { isAntiAlias = true }

                // Borde exterior (Naranja o Verde)
                paint.color = borderColor.toArgb()
                canvas.drawCircle(66f, 66f, 66f, paint)

                // Espacio en blanco interior
                paint.color = android.graphics.Color.WHITE
                canvas.drawCircle(66f, 66f, 60f, paint)

                // Dibujamos la foto recortada
                canvas.drawBitmap(originalBitmap, 10f, 10f, null)

                markerIcon = BitmapDescriptorFactory.fromBitmap(output)
            }
        }
    }
    return markerIcon
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapHomeScreen(
    mapViewModel: MapViewModel
) {
    val context = LocalContext.current

    // --- ESTADOS DEL VIEWMODEL ---
    val snapshot by mapViewModel.snapshot.collectAsState()
    val pets by mapViewModel.pets.collectAsState()
    val isDrawingMode by mapViewModel.isDrawingMode.collectAsState()
    val drawingType by mapViewModel.drawingType.collectAsState()
    val tempCircleCenter by mapViewModel.tempCircleCenter.collectAsState()
    val tempPolygonPoints by mapViewModel.tempPolygonPoints.collectAsState()
    val circleRadius by mapViewModel.circleRadius.collectAsState()
    val isTracking by mapViewModel.isTracking.collectAsState()
    val livePetLocations by mapViewModel.livePetLocations.collectAsState()
    val liveOwnerLocations by mapViewModel.liveOwnerLocations.collectAsState()
    val trackingError by mapViewModel.trackingError.collectAsState()

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
                    myLocationButtonEnabled = hasLocationPermission,
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
                    val petsToDraw = mutableMapOf<String, Pair<LatLng, String?>>()

                    data.zonas.forEach { zona ->
                        val fallbackPos = when (zona.tipo) {
                            "circulo" -> {
                                zona.centro?.let { LatLng(it.lat, it.lng) }
                            }
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
                                petsToDraw[pet.mascotaId] = Pair(finalPos, pet.fotoUrl)
                            }
                        }
                    }

                    data.marcadores.desaparecidas.forEach { pet ->
                        val livePos = livePetLocations[pet.mascotaId]
                        val finalPos = livePos ?: LatLng(pet.lat, pet.lng)
                        petsToDraw[pet.mascotaId] = Pair(finalPos, pet.fotoUrl)
                    }

                    petsToDraw.forEach { (mascotaId, petData) ->
                        key(mascotaId) {
                            val (pos, fotoUrl) = petData
                            val customIcon = rememberCustomMarkerIcon(context, fotoUrl, PrimaryOrange)

                            Marker(
                                state = MarkerState(position = pos),
                                title = "Mascota",
                                icon = customIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
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

                // 4. DIBUJO TEMPORAL
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

            // --- COMPONENTES DE INTERFAZ ---
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(50.dp))
                        .clickable { /* Lógica de filtrado */ },
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp), tint = PrimaryOrange)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Filtrar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!isDrawingMode) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 80.dp, end = 16.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (isTracking) {
                                mascotaPaseoActivoId?.let { mapViewModel.togglePaseo(context, it) }
                                mascotaPaseoActivoId = null
                            } else { showPaseoDialog = true }
                        },
                        containerColor = if (isTracking) MaterialTheme.colorScheme.error else PrimaryOrange,
                        contentColor = Color.White,
                        icon = { Icon(if (isTracking) Icons.Default.Stop else Icons.AutoMirrored.Filled.DirectionsWalk, null) },
                        text = { Text(if (isTracking) "Detener" else "Paseo") }
                    )
                }
            }

            if (!isDrawingMode && !isTracking) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { mapViewModel.startDrawing("circulo") },
                        containerColor = Color.White,
                        contentColor = PrimaryOrange
                    ) { Icon(Icons.Default.AddLocation, contentDescription = "Círculo") }

                    SmallFloatingActionButton(
                        onClick = { mapViewModel.startDrawing("poligono") },
                        containerColor = Color.White,
                        contentColor = PrimaryOrange
                    ) { Icon(Icons.Default.Polyline, contentDescription = "Polígono") }
                }
            }

            if (isDrawingMode) {
                Column(
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 80.dp)
                ) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.9f), shadowElevation = 4.dp) {
                        Text(
                            text = if (drawingType == "circulo") "Fija el centro en el mapa" else "Agrega los puntos del polígono",
                            modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Medium
                        )
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

            AlertDialog(
                onDismissRequest = { showAssignPetsDialog = false },
                title = { Text("Guardar Zona Segura", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(value = nombreZona, onValueChange = { nombreZona = it }, label = { Text("Nombre (ej. Casa)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Asignar a:", fontWeight = FontWeight.Bold, color = PrimaryOrange)
                        LazyColumn(modifier = Modifier.height(180.dp)) {
                            items(pets) { pet ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        if (selectedPetIds.contains(pet.mascotaId)) selectedPetIds.remove(pet.mascotaId) else selectedPetIds.add(pet.mascotaId)
                                    }.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = selectedPetIds.contains(pet.mascotaId), onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = PrimaryOrange))
                                    Text(pet.nombre, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        mapViewModel.saveZoneWithMultiplePets(nombreZona, selectedPetIds)
                        showAssignPetsDialog = false
                    }, enabled = nombreZona.isNotBlank() && selectedPetIds.isNotEmpty()) { Text("Guardar Zona") }
                },
                dismissButton = { TextButton(onClick = { showAssignPetsDialog = false }) { Text("Cancelar") } }
            )
        }

        if (showPaseoDialog) {
            var selectedId by remember { mutableStateOf<String?>(null) }
            AlertDialog(
                onDismissRequest = { showPaseoDialog = false },
                title = { Text("¿Quién sale a pasear?", fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(modifier = Modifier.height(180.dp)) {
                        items(pets) { pet ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { selectedId = pet.mascotaId }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedId == pet.mascotaId, onClick = { selectedId = pet.mascotaId }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryOrange))
                                Text(pet.nombre, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        selectedId?.let {
                            mascotaPaseoActivoId = it
                            mapViewModel.togglePaseo(context, it)
                            showPaseoDialog = false
                        }
                    }, enabled = selectedId != null) { Text("Comenzar") }
                },
                dismissButton = { TextButton(onClick = { showPaseoDialog = false }) { Text("Cancelar") } }
            )
        }
    }
}