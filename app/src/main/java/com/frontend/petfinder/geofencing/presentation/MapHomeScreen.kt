package com.frontend.petfinder.geofencing.presentation

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch // Importación necesaria para el scope.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapHomeScreen(
    mapViewModel: MapViewModel
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // --- NUEVO: Alcance de corrutina para animar la cámara ---
    val scope = rememberCoroutineScope()

    val snapshot by mapViewModel.snapshot.collectAsState()
    val isDrawingMode by mapViewModel.isDrawingMode.collectAsState()
    val drawingType by mapViewModel.drawingType.collectAsState()
    val tempCircleCenter by mapViewModel.tempCircleCenter.collectAsState()
    val tempPolygonPoints by mapViewModel.tempPolygonPoints.collectAsState()
    val pets by mapViewModel.pets.collectAsState()

    var hasLocationPermission by remember { mutableStateOf(false) }
    var showAssignPetsDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-17.3895, -66.1568), 13f)
    }

    // --- CORRECCIÓN: Animación envuelta en scope.launch ---
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    scope.launch { // Esto soluciona el error de compilación
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(it.latitude, it.longitude),
                                15f
                            )
                        )
                    }
                }
            }
        }
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocationPermission, zoomControlsEnabled = false),
                onMapClick = { latLng -> mapViewModel.handleMapClick(latLng) }
            ) {
                snapshot?.let { data ->
                    data.marcadores.usuariosCompartidos.forEach { user ->
                        Marker(state = MarkerState(position = LatLng(user.lat, user.lng)), title = user.nombre, snippet = "Usuario conectado")
                    }

                    data.marcadores.desaparecidas.forEach { pet ->
                        Marker(
                            state = MarkerState(position = LatLng(pet.lat, pet.lng)),
                            title = "¡${pet.nombre} desaparecida!",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        )
                    }

                    data.zonas.forEach { zona ->
                        if (zona.tipo == "circulo" && zona.centro != null) {
                            Circle(
                                center = LatLng(zona.centro.lat, zona.centro.lng),
                                radius = zona.radioMetros ?: 80.0,
                                fillColor = Color(0x221976D2), strokeColor = Color(0xFF1976D2), strokeWidth = 2f
                            )
                        } else if (zona.tipo == "poligono" && zona.geometria != null) {
                            val puntos = zona.geometria.coordinates[0].map { LatLng(it[1], it[0]) }
                            Polygon(points = puntos, fillColor = Color(0x22FF9800), strokeColor = Color(0xFFFF9800), strokeWidth = 2f)
                        }
                    }
                }

                if (isDrawingMode) {
                    if (drawingType == "circulo" && tempCircleCenter != null) {
                        Circle(center = tempCircleCenter!!, radius = 80.0, fillColor = Color(0x554CAF50), strokeColor = Color(0xFF4CAF50), strokeWidth = 4f)
                    } else if (drawingType == "poligono" && tempPolygonPoints.isNotEmpty()) {
                        Polygon(points = tempPolygonPoints, fillColor = Color(0x554CAF50), strokeColor = Color(0xFF4CAF50), strokeWidth = 4f)
                        tempPolygonPoints.forEach { point -> Marker(state = MarkerState(position = point)) }
                    }
                }
            }

            if (isDrawingMode) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = if (drawingType == "circulo") "Toca el mapa para fijar el centro" else "Toca para agregar vértices (Mín. 3)",
                        modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FloatingActionButton(onClick = { mapViewModel.cancelDrawing() }, containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    }

                    if (drawingType == "poligono" && tempPolygonPoints.isNotEmpty()) {
                        FloatingActionButton(onClick = { mapViewModel.undoLastPolygonPoint() }) {
                            Icon(Icons.Default.Undo, contentDescription = "Deshacer")
                        }
                    }

                    val canSave = (drawingType == "circulo" && tempCircleCenter != null) || (drawingType == "poligono" && tempPolygonPoints.size >= 3)
                    if (canSave) {
                        FloatingActionButton(
                            onClick = { showAssignPetsDialog = true },
                            containerColor = Color(0xFF4CAF50), contentColor = Color.White
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Guardar Zona")
                        }
                    }
                }
            }
        }

        if (showAssignPetsDialog) {
            var nombreZona by remember { mutableStateOf("") }
            val selectedPetIds = remember { mutableStateListOf<String>() }

            AlertDialog(
                onDismissRequest = { showAssignPetsDialog = false },
                title = { Text("Guardar y Asignar Mascotas") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = nombreZona, onValueChange = { nombreZona = it },
                            label = { Text("Nombre de la zona (ej. Casa)") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("¿Quién estará seguro aquí?", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(modifier = Modifier.fillMaxHeight(0.4f)) {
                            items(pets) { pet ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable {
                                            if (selectedPetIds.contains(pet.mascotaId)) selectedPetIds.remove(pet.mascotaId)
                                            else selectedPetIds.add(pet.mascotaId)
                                        }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = selectedPetIds.contains(pet.mascotaId), onCheckedChange = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(pet.nombre)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            mapViewModel.saveZoneWithMultiplePets(nombreZona, selectedPetIds)
                            showAssignPetsDialog = false
                        },
                        enabled = nombreZona.isNotBlank() && selectedPetIds.isNotEmpty()
                    ) { Text("Guardar Zona") }
                },
                dismissButton = { TextButton(onClick = { showAssignPetsDialog = false }) { Text("Cancelar") } }
            )
        }
    }
}