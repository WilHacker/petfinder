package com.frontend.petfinder.geofencing.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Polyline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyZonesScreen(
    viewModel: MapViewModel, // Usamos el cerebro centralizado
    onNavigateToMap: () -> Unit // Función para saltar al mapa principal
) {
    val snapshot by viewModel.snapshot.collectAsState()
    val zonas = snapshot?.zonas ?: emptyList()

    var showTypeSelectorDialog by remember { mutableStateOf(false) }
    var zonaToDelete by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Zonas Seguras", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showTypeSelectorDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Nueva Zona") },
                text = { Text("Nueva Zona") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        if (zonas.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No tienes zonas registradas aún.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(zonas) { zona ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (zona.tipo == "circulo") Icons.Outlined.Circle else Icons.Outlined.Polyline,
                                contentDescription = zona.tipo,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = zona.nombre ?: zona.nombreZona ?: "Zona",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                if (!zona.mascotas.isNullOrEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Pets, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = zona.mascotas.joinToString(", ") { it.nombre },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = { zonaToDelete = zona.zonaId }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGO: Elegir forma y saltar al mapa principal ---
    if (showTypeSelectorDialog) {
        var tipoInput by remember { mutableStateOf("circulo") }
        AlertDialog(
            onDismissRequest = { showTypeSelectorDialog = false },
            title = { Text("¿Qué forma tendrá la zona?") },
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
                    // 1. Le avisa al cerebro compartido que vamos a dibujar
                    viewModel.startDrawing(tipoInput)
                    showTypeSelectorDialog = false
                    // 2. Te teletransporta a la pestaña del Mapa Principal
                    onNavigateToMap()
                }) { Text("Ir al Mapa a Dibujar") }
            },
            dismissButton = {
                TextButton(onClick = { showTypeSelectorDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // --- DIÁLOGO: Eliminar Zona ---
    if (zonaToDelete != null) {
        AlertDialog(
            onDismissRequest = { zonaToDelete = null },
            title = { Text("Eliminar Zona") },
            text = { Text("¿Estás seguro de que quieres eliminar esta zona?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteZone(zonaToDelete!!)
                        zonaToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { zonaToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}