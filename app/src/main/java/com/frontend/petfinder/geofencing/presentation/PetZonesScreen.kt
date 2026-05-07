package com.frontend.petfinder.geofencing.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetZonesScreen(
    petId: String,
    viewModel: PetZonesViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val zonas by viewModel.zonas.collectAsState()
    val isDrawingMode by viewModel.isDrawingMode.collectAsState()
    val drawingType by viewModel.drawingType.collectAsState()
    val tempCircleCenter by viewModel.tempCircleCenter.collectAsState()
    val tempCircleRadius by viewModel.tempCircleRadius.collectAsState()
    val tempPolygonPoints by viewModel.tempPolygonPoints.collectAsState()

    var showNewZoneDialog by remember { mutableStateOf(false) }

    LaunchedEffect(petId) {
        viewModel.loadZones(petId)
    }

    val cochabamba = LatLng(-17.3895, -66.1568)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(cochabamba, 14f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isDrawingMode) "Dibujando Zona" else "Zonas Seguras") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            if (!isDrawingMode) {
                ExtendedFloatingActionButton(
                    onClick = { showNewZoneDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nueva Zona") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
                onMapClick = { latLng -> viewModel.handleMapClick(latLng) }
            ) {
                // 1. DIBUJAR ZONAS EXISTENTES (Oficiales de la base de datos)
                zonas.forEach { zona ->
                    if (zona.tipo == "circulo" && zona.centro != null) {
                        Circle(
                            center = LatLng(zona.centro.lat, zona.centro.lng),
                            radius = zona.radioMetros ?: 80.0,
                            fillColor = Color(0x334CAF50), // Verde transparente
                            strokeColor = Color(0xFF4CAF50),
                            strokeWidth = 3f
                        )
                    } else if (zona.tipo == "poligono" && zona.geometria != null) {
                        val puntos = zona.geometria.coordinates[0].map { LatLng(it[1], it[0]) }
                        Polygon(
                            points = puntos,
                            fillColor = Color(0x334CAF50),
                            strokeColor = Color(0xFF4CAF50),
                            strokeWidth = 3f
                        )
                    }
                }

                // 2. DIBUJAR LA ZONA TEMPORAL (Mientras el usuario toca la pantalla)
                if (isDrawingMode) {
                    if (drawingType == "circulo" && tempCircleCenter != null) {
                        Circle(
                            center = tempCircleCenter!!,
                            radius = tempCircleRadius,
                            fillColor = Color(0x552196F3), // Azul mientras dibuja
                            strokeColor = Color(0xFF2196F3),
                            strokeWidth = 4f
                        )
                    } else if (drawingType == "poligono" && tempPolygonPoints.isNotEmpty()) {
                        Polygon(
                            points = tempPolygonPoints,
                            fillColor = Color(0x552196F3),
                            strokeColor = Color(0xFF2196F3),
                            strokeWidth = 4f
                        )
                        // Dibujar pequeños marcadores en los vértices
                        tempPolygonPoints.forEach { point ->
                            Marker(state = MarkerState(position = point), title = "Vértice")
                        }
                    }
                }
            }

            // 3. PANELES FLOTANTES DURANTE EL MODO DIBUJO
            if (isDrawingMode) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (drawingType == "circulo") "Toca el mapa para fijar el centro" else "Toca para agregar vértices (Mínimo 3)",
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FloatingActionButton(onClick = { viewModel.cancelDrawing() }, containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    }

                    if (drawingType == "poligono" && tempPolygonPoints.isNotEmpty()) {
                        FloatingActionButton(onClick = { viewModel.undoLastPolygonPoint() }) {
                            Icon(Icons.Default.Undo, contentDescription = "Deshacer vértice")
                        }
                    }

                    val canSave = (drawingType == "circulo" && tempCircleCenter != null) || (drawingType == "poligono" && tempPolygonPoints.size >= 3)
                    if (canSave) {
                        FloatingActionButton(onClick = { viewModel.saveZone() }, containerColor = Color(0xFF4CAF50), contentColor = Color.White) {
                            Icon(Icons.Default.Check, contentDescription = "Guardar Zona")
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO PARA CONFIGURAR LA NUEVA ZONA
    if (showNewZoneDialog) {
        var nombreInput by remember { mutableStateOf("") }
        var tipoInput by remember { mutableStateOf("circulo") }

        AlertDialog(
            onDismissRequest = { showNewZoneDialog = false },
            title = { Text("Nueva Zona Segura") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreInput,
                        onValueChange = { nombreInput = it },
                        label = { Text("Nombre (ej. Mi Casa)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Forma de la zona:", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = tipoInput == "circulo", onClick = { tipoInput = "circulo" })
                        Text("Círculo")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = tipoInput == "poligono", onClick = { tipoInput = "poligono" })
                        Text("Polígono")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startDrawing(nombreInput, tipoInput)
                        showNewZoneDialog = false
                    },
                    enabled = nombreInput.isNotBlank()
                ) {
                    Text("Empezar a Dibujar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewZoneDialog = false }) { Text("Cancelar") }
            }
        )
    }
}