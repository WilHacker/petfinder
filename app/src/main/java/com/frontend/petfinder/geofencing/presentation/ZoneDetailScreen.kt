package com.frontend.petfinder.geofencing.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frontend.petfinder.core.presentation.components.DialogType
import com.frontend.petfinder.core.presentation.components.PetFinderDialog
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.theme.PrimaryOrangeLight
import com.frontend.petfinder.geofencing.data.ZonePetDetailDto
import com.frontend.petfinder.geofencing.data.ZoneWithPetsDto
import com.frontend.petfinder.pets.data.dto.PetListItemDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneDetailScreen(
    zonaId: Int,
    viewModel: ZoneDetailViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val zoneState by viewModel.zoneState.collectAsStateWithLifecycle()
    val userPets by viewModel.userPets.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    var showAddPetsSheet by remember { mutableStateOf(false) }
    var petToRemove by remember { mutableStateOf<ZonePetDetailDto?>(null) }
    var selectedPetIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(zonaId) {
        viewModel.loadZone(zonaId)
        viewModel.loadUserPets()
    }

    LaunchedEffect(showAddPetsSheet) {
        if (showAddPetsSheet) viewModel.loadUserPets()
    }

    LaunchedEffect(actionState) {
        if (actionState is ZoneDetailViewModel.ActionState.Success) {
            showAddPetsSheet = false
            selectedPetIds = emptySet()
            viewModel.resetAction()
        }
    }

    petToRemove?.let { mascota ->
        PetFinderDialog(
            type = DialogType.DANGER,
            title = "¿Quitar mascota?",
            message = "${mascota.nombre} dejará de estar protegida por esta zona.",
            confirmText = "Quitar",
            dismissText = "Cancelar",
            onConfirm = {
                viewModel.removePet(zonaId, mascota.mascotaId)
                petToRemove = null
            },
            onDismiss = { petToRemove = null }
        )
    }

    if (showAddPetsSheet) {
        val zone = (zoneState as? ZoneDetailViewModel.ZoneState.Success)?.zone
        val alreadyInZone = zone?.mascotas?.map { it.mascotaId }?.toSet() ?: emptySet()
        val availablePets = userPets.filter { it.mascotaId !in alreadyInZone }

        ModalBottomSheet(
            onDismissRequest = {
                showAddPetsSheet = false
                selectedPetIds = emptySet()
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Agregar mascotas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Selecciona las mascotas que quieres proteger en esta zona.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))

                if (availablePets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Todas tus mascotas ya están en esta zona.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    availablePets.forEach { pet ->
                        val isSelected = pet.mascotaId in selectedPetIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedPetIds = if (checked) {
                                        selectedPetIds + pet.mascotaId
                                    } else {
                                        selectedPetIds - pet.mascotaId
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PrimaryOrange,
                                    uncheckedColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pet.nombre,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val tipoLabel = pet.tipoMascota?.nombre ?: ""
                                if (tipoLabel.isNotEmpty()) {
                                    Text(
                                        text = tipoLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val isSaving = actionState is ZoneDetailViewModel.ActionState.Loading
                    Button(
                        onClick = {
                            if (selectedPetIds.isNotEmpty()) {
                                viewModel.addPets(zonaId, selectedPetIds.toList())
                            }
                        },
                        enabled = selectedPetIds.isNotEmpty() && !isSaving,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Agregar ${if (selectedPetIds.size > 1) "${selectedPetIds.size} mascotas" else "mascota"}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    when (val s = zoneState) {
                        is ZoneDetailViewModel.ZoneState.Success ->
                            Text(
                                s.zone.nombreZona ?: "Zona segura",
                                fontWeight = FontWeight.ExtraBold
                            )
                        else -> Text("Zona segura", fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        when (val state = zoneState) {
            is ZoneDetailViewModel.ZoneState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            }

            is ZoneDetailViewModel.ZoneState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadZone(zonaId) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            is ZoneDetailViewModel.ZoneState.Success -> {
                ZoneDetailContent(
                    zone = state.zone,
                    paddingValues = paddingValues,
                    onToggleActive = { viewModel.toggleActive(zonaId, state.zone.estaActiva ?: true) },
                    onAddPets = { showAddPetsSheet = true },
                    onRemovePet = { petToRemove = it }
                )
            }
        }
    }
}

@Composable
private fun ZoneDetailContent(
    zone: ZoneWithPetsDto,
    paddingValues: PaddingValues,
    onToggleActive: () -> Unit,
    onAddPets: () -> Unit,
    onRemovePet: (ZonePetDetailDto) -> Unit
) {
    val isActive = zone.estaActiva ?: true
    val isCircle = zone.tipo == "circulo"

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Tarjeta de info de zona ──
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryOrangeLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCircle) Icons.Default.AddLocation else Icons.Default.Polyline,
                                    contentDescription = null,
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isCircle) "Zona circular" else "Zona poligonal",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isCircle && zone.radioMetros != null) {
                                    Text(
                                        text = "${zone.radioMetros.toInt()} metros de radio",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }

                        Switch(
                            checked = isActive,
                            onCheckedChange = { onToggleActive() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryOrange,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFCCCCCC),
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }

                    if (!isActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF3E0)
                        ) {
                            Text(
                                text = "Zona desactivada — no enviará alertas.",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Encabezado de mascotas ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mascotas protegidas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    val count = zone.mascotas?.size ?: 0
                    Text(
                        text = if (count == 0) "Sin mascotas aún" else "$count mascota${if (count > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onAddPets,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PrimaryOrangeLight,
                        contentColor = PrimaryOrange
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Agregar",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── Lista de mascotas ──
        val mascotas = zone.mascotas ?: emptyList()
        if (mascotas.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8F8F8)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Pets,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFFDDDDDD)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Agrega mascotas para activar la protección.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(mascotas, key = { it.mascotaId }) { mascota ->
                ZonePetRow(mascota = mascota, onRemove = { onRemovePet(mascota) })
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun ZonePetRow(
    mascota: ZonePetDetailDto,
    onRemove: () -> Unit
) {
    val estadoColor = when (mascota.estado) {
        "extraviada" -> Color(0xFFD32F2F)
        "en_paseo" -> Color(0xFF1565C0)
        else -> Color(0xFF2E7D32)
    }
    val estadoLabel = when (mascota.estado) {
        "extraviada" -> "Extraviada"
        "en_paseo" -> "En paseo"
        else -> "En casa"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryOrangeLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mascota.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(estadoColor)
                    )
                    Text(
                        text = estadoLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = estadoColor
                    )
                    if (mascota.tipoMascota.isNotEmpty()) {
                        Text(
                            text = "· ${mascota.tipoMascota}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Quitar mascota",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
