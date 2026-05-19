package com.frontend.petfinder.geofencing.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frontend.petfinder.core.presentation.components.DialogType
import com.frontend.petfinder.core.presentation.components.PetFinderButton
import com.frontend.petfinder.core.presentation.components.PetFinderDialog
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.theme.PrimaryOrangeLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyZonesScreen(
    viewModel: MapViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToZoneDetail: (Int) -> Unit = {}
) {
    val zones by viewModel.userZones.collectAsState()
    val isLoading by viewModel.isZonesLoading.collectAsState()

    var zoneToDelete by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAllUserZones()
    }

    // Diálogo de confirmación para eliminar
    zoneToDelete?.let { zonaId ->
        PetFinderDialog(
            type = DialogType.DANGER,
            title = "¿Eliminar zona?",
            message = "Esta acción no se puede deshacer. Tu mascota quedará sin protección en esta zona.",
            confirmText = "Eliminar",
            dismissText = "Cancelar",
            onConfirm = {
                viewModel.deleteZone(zonaId)
                zoneToDelete = null
            },
            onDismiss = { zoneToDelete = null }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Mis Zonas Seguras",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryOrange
                    )
                }

                zones.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShareLocation,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFFDDDDDD)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Sin zonas seguras aún",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Dibuja un círculo o polígono en el mapa y recibe alertas al instante si tu mascota se escapa.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        PetFinderButton(
                            text = "Ir al Mapa",
                            onClick = onNavigateToMap
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 24.dp, end = 24.dp,
                            top = 8.dp, bottom = 120.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(zones, key = { it.zonaId }) { zone ->
                            val isActive = zone.estaActiva ?: true
                            val isCircle = zone.tipo == "circulo"
                            val radioMetros = zone.radioMetros?.toInt()

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(if (isActive) 6.dp else 2.dp, RoundedCornerShape(20.dp))
                                    .alpha(if (isActive) 1f else 0.6f)
                                    .clickable { onNavigateToZoneDetail(zone.zonaId) },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 18.dp)
                                        .fillMaxWidth()
                                ) {
                                    // ── Encabezado: ícono de forma + nombre + switch ──
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Ícono de tipo de zona
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(PrimaryOrangeLight),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isCircle) Icons.Default.AddLocation else Icons.Default.Polyline,
                                                    contentDescription = null,
                                                    tint = PrimaryOrange,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Column {
                                                Text(
                                                    text = zone.nombreZona ?: "Zona sin nombre",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                // Tipo legible + radio si aplica
                                                val tipoLabel = if (isCircle) {
                                                    if (radioMetros != null) "Círculo · ${radioMetros}m" else "Círculo"
                                                } else {
                                                    "Polígono"
                                                }
                                                Text(
                                                    text = tipoLabel,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = isActive,
                                            onCheckedChange = {
                                                viewModel.toggleZoneState(zone.zonaId, isActive)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = PrimaryOrange,
                                                uncheckedThumbColor = Color.White,
                                                uncheckedTrackColor = Color(0xFFCCCCCC),
                                                uncheckedBorderColor = Color.Transparent
                                            )
                                        )
                                    }

                                    // ── Mascotas protegidas ──
                                    if (!zone.mascotas.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        HorizontalDivider(color = Color(0xFFF0F0F0))
                                        Spacer(modifier = Modifier.height(14.dp))

                                        Text(
                                            text = "Mascotas protegidas",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            zone.mascotas.forEach { mascota ->
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50.dp))
                                                        .background(PrimaryOrangeLight)
                                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Pets,
                                                        contentDescription = null,
                                                        tint = PrimaryOrange,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = mascota.nombre,
                                                        color = PrimaryOrange,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // ── Acción eliminar ──
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { zoneToDelete = zone.zonaId },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Eliminar zona",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Eliminar zona",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
