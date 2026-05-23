package com.frontend.petfinder.pets.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.frontend.petfinder.core.presentation.components.DialogType
import com.frontend.petfinder.core.presentation.components.PetFinderDialog
import com.frontend.petfinder.core.presentation.components.PetFinderErrorBanner
import com.frontend.petfinder.core.presentation.components.PetFinderTextField
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.theme.PrimaryOrangeLight

private val colorSugerenciasEdit = listOf(
    "Negro", "Blanco", "Café", "Gris", "Dorado", "Atigrado", "Naranja", "Crema"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditPetScreen(
    mascotaId: String,
    viewModel: EditPetViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current

    val isLoading        by viewModel.isLoading.collectAsStateWithLifecycle()
    val nombre           by viewModel.nombre.collectAsStateWithLifecycle()
    val tiposMascota     by viewModel.tiposMascota.collectAsStateWithLifecycle()
    val tipoSeleccionado by viewModel.tipoSeleccionado.collectAsStateWithLifecycle()
    val sexo             by viewModel.sexo.collectAsStateWithLifecycle()
    val colorPrimario    by viewModel.colorPrimario.collectAsStateWithLifecycle()
    val rasgos           by viewModel.rasgosParticulares.collectAsStateWithLifecycle()
    val existingPhotos   by viewModel.existingPhotos.collectAsStateWithLifecycle()
    val newPhotos        by viewModel.newPhotos.collectAsStateWithLifecycle()
    val deletingIds      by viewModel.photoDeletingIds.collectAsStateWithLifecycle()
    val saveState        by viewModel.saveState.collectAsStateWithLifecycle()
    val deleteState      by viewModel.deleteState.collectAsStateWithLifecycle()

    var expandedTipo by remember { mutableStateOf(false) }

    LaunchedEffect(mascotaId) { viewModel.load(mascotaId) }

    LaunchedEffect(saveState) {
        if (saveState is EditPetViewModel.SaveState.Success) onNavigateBack()
    }
    LaunchedEffect(deleteState) {
        if (deleteState is EditPetViewModel.DeleteState.Success) onDeleted()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris -> if (uris.isNotEmpty()) viewModel.addNewPhotos(uris) }

    // ── Diálogo confirmación eliminar mascota ──────────────────────────────
    if (deleteState is EditPetViewModel.DeleteState.Confirming) {
        PetFinderDialog(
            type = DialogType.DANGER,
            title = "¿Eliminar mascota?",
            message = "Esta acción es permanente. Se eliminarán todas las fotos, el historial médico y la placa QR asociada.",
            confirmText = "Sí, eliminar",
            dismissText = "Cancelar",
            onConfirm = { viewModel.deletePet(mascotaId) },
            onDismiss = { viewModel.cancelDelete() }
        )
    }

    // ── Diálogo error al eliminar ──────────────────────────────────────────
    if (deleteState is EditPetViewModel.DeleteState.Error) {
        PetFinderDialog(
            type = DialogType.DANGER,
            title = "Error",
            message = (deleteState as EditPetViewModel.DeleteState.Error).message,
            confirmText = "Entendido",
            onConfirm = { viewModel.cancelDelete() },
            onDismiss = { viewModel.cancelDelete() }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Editar mascota",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Actualiza el perfil de tu compañero",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = PrimaryOrange)
                    Text("Cargando información…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── SECCIÓN 1: Fotos ───────────────────────────────────────────
            EditSectionHeader("Fotos")

            val totalPhotos = existingPhotos.size + newPhotos.size
            val canAddMore  = totalPhotos < 4

            if (totalPhotos == 0) {
                // Estado vacío
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .border(1.dp, Color(0xFFEAEAEA), RoundedCornerShape(16.dp)),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(36.dp), tint = PrimaryOrange)
                        Spacer(Modifier.height(8.dp))
                        Text("Agregar fotos", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = PrimaryOrange)
                        Text("Máximo 4 fotos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Fotos existentes del servidor
                    existingPhotos.forEachIndexed { index, foto ->
                        val isDeleting = foto.fotoId in deletingIds
                        val isPrincipal = foto.esPrincipal
                        Box(modifier = Modifier.size(80.dp)) {
                            AsyncImage(
                                model = foto.fotoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isPrincipal) 2.dp else 0.dp,
                                        color = if (isPrincipal) PrimaryOrange else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            )
                            if (isPrincipal) {
                                Surface(
                                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                                    color = PrimaryOrange,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "Principal",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            // Overlay de carga al borrar
                            if (isDeleting) {
                                Box(
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                }
                            } else if (totalPhotos > 1) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                        .clickable { viewModel.deleteExistingPhoto(mascotaId, foto.fotoId) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }

                    // Fotos nuevas (aún no subidas)
                    newPhotos.forEachIndexed { index, uri ->
                        Box(modifier = Modifier.size(80.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                            )
                            // Badge "Nueva"
                            Surface(
                                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                                color = Color(0xFF1976D2),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "Nueva",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                    .clickable { viewModel.removeNewPhoto(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    // Botón agregar más
                    if (canAddMore) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF5F5F5))
                                .border(1.dp, Color(0xFFEAEAEA), RoundedCornerShape(12.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = PrimaryOrange, modifier = Modifier.size(28.dp))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${totalPhotos}/4 fotos · Las nuevas se subirán al guardar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── SECCIÓN 2: Información básica ──────────────────────────────
            EditSectionHeader("Información básica")

            PetFinderTextField(
                value = nombre,
                onValueChange = { viewModel.onNombreChange(it) },
                placeholder = "Nombre de la mascota *"
            )
            Spacer(Modifier.height(14.dp))

            // Tipo — Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedTipo,
                onExpandedChange = { expandedTipo = !expandedTipo }
            ) {
                OutlinedTextField(
                    value = tipoSeleccionado?.nombre ?: "Tipo de mascota",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, null, tint = PrimaryOrange)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = Color(0xFFEAEAEA),
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = if (tipoSeleccionado != null) MaterialTheme.colorScheme.onBackground else Color.Gray
                    ),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedTipo,
                    onDismissRequest = { expandedTipo = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    tiposMascota.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo.nombre) },
                            onClick = { viewModel.onTipoSeleccionado(tipo); expandedTipo = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Sexo — Segmented Button
            Text(
                "Sexo",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("M" to "Macho", "H" to "Hembra").forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = sexo == value,
                        onClick = { viewModel.onSexoChange(value) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = PrimaryOrangeLight,
                            activeContentColor = PrimaryOrange,
                            activeBorderColor = PrimaryOrange
                        )
                    ) {
                        Text(label, fontWeight = if (sexo == value) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── SECCIÓN 3: Apariencia ──────────────────────────────────────
            EditSectionHeader("Apariencia")

            Text(
                "Color principal",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                colorSugerenciasEdit.forEach { color ->
                    val isSelected = colorPrimario.equals(color, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .clickable { viewModel.onColorPrimarioChange(if (isSelected) "" else color) }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) PrimaryOrange else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(50.dp)
                            ),
                        color = if (isSelected) PrimaryOrangeLight else Color.White,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = color,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            PetFinderTextField(
                value = colorPrimario,
                onValueChange = { viewModel.onColorPrimarioChange(it) },
                placeholder = "O escribe el color…"
            )
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = rasgos,
                onValueChange = { viewModel.onRasgosChange(it) },
                placeholder = { Text("Rasgos particulares (mancha en la oreja, cojea, etc.)", color = Color.Gray) },
                shape = RoundedCornerShape(16.dp),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = Color(0xFFEAEAEA),
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            // ── ERROR al guardar ───────────────────────────────────────────
            if (saveState is EditPetViewModel.SaveState.Error) {
                PetFinderErrorBanner(
                    message = (saveState as EditPetViewModel.SaveState.Error).message,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // ── BOTÓN GUARDAR ──────────────────────────────────────────────
            val isSaving = saveState is EditPetViewModel.SaveState.Saving
            Button(
                onClick = { viewModel.save(context, mascotaId) },
                enabled = !isSaving && deleteState !is EditPetViewModel.DeleteState.Deleting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(4.dp, RoundedCornerShape(50.dp)),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.White)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Guardando…", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Guardar cambios", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── ZONA DE PELIGRO ────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFF0F0),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 0.dp).let {
                    androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2))
                }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text(
                            "Zona de peligro",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Eliminar esta mascota borrará permanentemente su perfil, historial médico, fotos y placa QR.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(16.dp))
                    val isDeleting = deleteState is EditPetViewModel.DeleteState.Deleting
                    OutlinedButton(
                        onClick = { viewModel.confirmDelete() },
                        enabled = !isDeleting && !isSaving,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.error, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Eliminando…", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Eliminar mascota", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun EditSectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = PrimaryOrange
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFF0F0F0), thickness = 1.5.dp)
    }
}
