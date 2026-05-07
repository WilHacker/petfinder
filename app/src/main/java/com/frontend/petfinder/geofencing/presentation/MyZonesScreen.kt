package com.frontend.petfinder.geofencing.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
fun MyZonesScreen(
    viewModel: MyZonesViewModel = viewModel()
) {
    val zonas by viewModel.zonas.collectAsState()
    val pets by viewModel.pets.collectAsState()

    val isDrawingMode by viewModel.isDrawingMode.collectAsState()
    val drawingType by viewModel.drawingType.collectAsState()
    val tempCircleCenter by viewModel.tempCircleCenter.collectAsState()
    val tempPolygonPoints by viewModel.tempPolygonPoints.collectAsState()

    // Controladores de los diálogos emergentes
    var showTypeSelectorDialog by remember { mutableStateOf(false) }
    var showAssignPetsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    val cochabamba = LatLng(-17.3895, -66.1568)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(cochabamba, 14f)
    }

    Scaffold(
        floatingActionButton = {
            if (!isDrawingMode) {
                ExtendedFloatingActionButton(
                    onClick = { showTypeSelectorDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Nueva Zona") },
                    text = { Text("Nueva Zona") },
                    containerColor = MaterialTheme.colorScheme.primary
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
                // 1. DIBUJAR ZONAS EXISTENTES
                zonas.forEach { zona ->
                    if (zona.tipo == "circulo" && zona.centro != null) {
                        Circle(
                            center = LatLng(zona.centro.lat, zona.centro.lng),
                            radius = zona.radioMetros ?: 80.0,
                            fillColor = Color(0x334CAF50),
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

                // 2. DIBUJAR LA ZONA TEMPORAL MIENTRAS SE TOCA LA PANTALLA
                if (isDrawingMode) {
                    if (drawingType == "circulo" && tempCircleCenter != null) {
                        Circle(
                            center = tempCircleCenter!!,
                            radius = 80.0,
                            fillColor = Color(0x552196F3),
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
                        tempPolygonPoints.forEach { point ->
                            Marker(state = MarkerState(position = point))
                        }
                    }
                }
            }

            // 3. CONTROLES DURANTE EL DIBUJO
            if (isDrawingMode) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (drawingType == "circulo") "Toca el mapa para fijar el centro" else "Toca para agregar vértices (Mín. 3)",
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
                            Icon(Icons.Default.Undo, contentDescription = "Deshacer")
                        }
                    }

                    val canSave = (drawingType == "circulo" && tempCircleCenter != null) || (drawingType == "poligono" && tempPolygonPoints.size >= 3)
                    if (canSave) {
                        FloatingActionButton(
                            onClick = { showAssignPetsDialog = true }, // ¡Lanza el menú de checkboxes!
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Guardar Zona")
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS ---

    // Diálogo 1: Elegir la forma
    if (showTypeSelectorDialog) {
        var tipoInput by remember { mutableStateOf("circulo") }
        AlertDialog(
            onDismissRequest = { showTypeSelectorDialog = false },
            title = { Text("Crear Zona Segura") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = tipoInput == "circulo", onClick = { tipoInput = "circulo" })
                    Text("Círculo")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = tipoInput == "poligono", onClick = { tipoInput = "poligono" })
                    Text("Polígono")
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.startDrawing(tipoInput)
                    showTypeSelectorDialog = false
                }) { Text("Dibujar") }
            },
            dismissButton = { TextButton(onClick = { showTypeSelectorDialog = false }) { Text("Cancelar") } }
        )
    }

    // Diálogo 2: Poner Nombre y Checkboxes de Mascotas
    if (showAssignPetsDialog) {
        var nombreZona by remember { mutableStateOf("") }
        val selectedPetIds = remember { mutableStateListOf<String>() }

        AlertDialog(
            onDismissRequest = { showAssignPetsDialog = false },
            title = { Text("Asignar Zona a Mascotas") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreZona,
                        onValueChange = { nombreZona = it },
                        label = { Text("Nombre (ej. Mi Casa)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("¿A quién aplica esta zona?", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Lista de Checkboxes
                    LazyColumn(modifier = Modifier.fillMaxHeight(0.4f)) {
                        items(pets) { pet ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedPetIds.contains(pet.mascotaId)) selectedPetIds.remove(pet.mascotaId)
                                        else selectedPetIds.add(pet.mascotaId)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedPetIds.contains(pet.mascotaId),
                                    onCheckedChange = null // Lo maneja el Modifier.clickable del Row
                                )
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
                        viewModel.saveZoneWithMultiplePets(nombreZona, selectedPetIds)
                        showAssignPetsDialog = false
                    },
                    enabled = nombreZona.isNotBlank() && selectedPetIds.isNotEmpty()
                ) { Text("Guardar Zona") }
            },
            dismissButton = { TextButton(onClick = { showAssignPetsDialog = false }) { Text("Cancelar") } }
        )
    }
}