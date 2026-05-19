package com.frontend.petfinder.pets.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frontend.petfinder.core.presentation.components.DialogType
import com.frontend.petfinder.core.presentation.components.PetFinderDialog
import com.frontend.petfinder.core.presentation.components.PetFinderErrorBanner
import com.frontend.petfinder.core.presentation.components.PetFinderTextField
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.theme.PrimaryOrangeLight
import com.frontend.petfinder.pets.data.dto.CreateMedicalRecordRequest
import com.frontend.petfinder.pets.data.dto.MedicalRecordDto
import com.frontend.petfinder.pets.data.dto.UpdateMedicalRecordRequest
import java.text.SimpleDateFormat
import java.util.Locale

private val tiposMedicos = listOf(
    "vacuna"          to "Vacuna",
    "consulta"        to "Consulta",
    "cirugia"         to "Cirugía",
    "desparasitacion" to "Desparasitación",
    "otro"            to "Otro"
)

private fun tipoColor(tipo: String): Color = when (tipo) {
    "vacuna"          -> Color(0xFF4CAF50)
    "consulta"        -> Color(0xFF1E88E5)
    "cirugia"         -> Color(0xFFE53935)
    "desparasitacion" -> Color(0xFF8E24AA)
    else              -> Color(0xFF9E9E9E)
}

private fun tipoLabel(tipo: String): String =
    tiposMedicos.find { it.first == tipo }?.second ?: tipo.replaceFirstChar { it.uppercase() }

private fun formatearFecha(isoDate: String?): String {
    if (isoDate == null) return "—"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("d MMM yyyy", Locale("es"))
        outputFormat.format(inputFormat.parse(isoDate) ?: return isoDate.take(10))
    } catch (e: Exception) {
        isoDate.take(10)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalHistoryScreen(
    mascotaId: String,
    viewModel: MedicalViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val records by viewModel.records.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val formState by viewModel.formState.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<MedicalRecordDto?>(null) }
    var deleteTarget by remember { mutableStateOf<MedicalRecordDto?>(null) }

    LaunchedEffect(mascotaId) { viewModel.loadRecords(mascotaId) }

    // Cerrar sheet cuando guardado es exitoso
    LaunchedEffect(formState) {
        if (formState is MedicalViewModel.FormState.Success) {
            showForm = false
            editingRecord = null
            viewModel.resetFormState()
        }
    }

    // ── Diálogo confirmación eliminar ─────────────────────────────────────
    deleteTarget?.let { record ->
        PetFinderDialog(
            type = DialogType.DANGER,
            title = "¿Eliminar registro?",
            message = "Se eliminará permanentemente el registro de ${tipoLabel(record.tipo)}.",
            confirmText = "Eliminar",
            dismissText = "Cancelar",
            onConfirm = {
                viewModel.deleteRecord(mascotaId, record.registroId)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }

    // ── Bottom sheet formulario ───────────────────────────────────────────
    if (showForm) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showForm = false
                editingRecord = null
                viewModel.resetFormState()
            },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            MedicalRecordForm(
                initial = editingRecord,
                formState = formState,
                onSave = { tipo, descripcion, fecha, veterinario ->
                    val edit = editingRecord
                    if (edit != null) {
                        viewModel.updateRecord(
                            mascotaId, edit.registroId,
                            UpdateMedicalRecordRequest(
                                tipo = tipo,
                                descripcion = descripcion.ifBlank { null },
                                fecha = fecha.ifBlank { null },
                                veterinario = veterinario.ifBlank { null }
                            )
                        )
                    } else {
                        viewModel.createRecord(
                            mascotaId,
                            CreateMedicalRecordRequest(
                                tipo = tipo,
                                descripcion = descripcion.ifBlank { null },
                                fecha = fecha.ifBlank { null },
                                veterinario = veterinario.ifBlank { null }
                            )
                        )
                    }
                },
                onDismiss = {
                    showForm = false
                    editingRecord = null
                    viewModel.resetFormState()
                }
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Historial médico",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (records.isNotEmpty()) {
                            Text(
                                "${records.size} ${if (records.size == 1) "registro" else "registros"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showForm = true },
                containerColor = PrimaryOrange,
                contentColor = Color.White,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Agregar registro", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryOrange
                    )
                }

                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PetFinderErrorBanner(error!!)
                        TextButton(onClick = { viewModel.loadRecords(mascotaId) }) {
                            Text("Reintentar", color = PrimaryOrange)
                        }
                    }
                }

                records.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.MedicalServices,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = Color(0xFFDDDDDD)
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Sin registros médicos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Lleva un control de vacunas, consultas y tratamientos de tu mascota.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 24.dp, end = 24.dp,
                            top = 8.dp, bottom = 120.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(records, key = { it.registroId }) { record ->
                            MedicalRecordCard(
                                record = record,
                                onEdit = {
                                    editingRecord = record
                                    showForm = true
                                },
                                onDelete = { deleteTarget = record }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicalRecordCard(
    record: MedicalRecordDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val color = tipoColor(record.tipo)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tipo chip
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Text(
                        tipoLabel(record.tipo),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }

                // Fecha
                if (record.fecha != null) {
                    Text(
                        formatearFecha(record.fecha),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!record.descripcion.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    record.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (!record.veterinario.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Default.LocalHospital,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        record.veterinario,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, null, tint = PrimaryOrange, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Editar", color = PrimaryOrange, style = MaterialTheme.typography.labelMedium)
                }
                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicalRecordForm(
    initial: MedicalRecordDto?,
    formState: MedicalViewModel.FormState,
    onSave: (tipo: String, descripcion: String, fecha: String, veterinario: String) -> Unit,
    onDismiss: () -> Unit
) {
    var tipoSeleccionado by remember { mutableStateOf(initial?.tipo ?: "vacuna") }
    var descripcion by remember { mutableStateOf(initial?.descripcion ?: "") }
    var fecha by remember { mutableStateOf(initial?.fecha?.take(10) ?: "") }
    var veterinario by remember { mutableStateOf(initial?.veterinario ?: "") }

    val isSaving = formState is MedicalViewModel.FormState.Saving

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            if (initial == null) "Nuevo registro" else "Editar registro",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Tipo — chips scrollables
        Text(
            "Tipo de registro",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            tiposMedicos.forEach { (value, label) ->
                val isSelected = tipoSeleccionado == value
                val color = tipoColor(value)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .clickable { tipoSeleccionado = value },
                    shape = RoundedCornerShape(50.dp),
                    color = if (isSelected) color.copy(alpha = 0.12f) else Color(0xFFF5F5F5)
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Descripción
        PetFinderTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            placeholder = "Descripción (vacuna antirrábica, cirugía...)"
        )
        Spacer(Modifier.height(12.dp))

        // Fecha
        PetFinderTextField(
            value = fecha,
            onValueChange = { fecha = it },
            placeholder = "Fecha (AAAA-MM-DD)"
        )
        Spacer(Modifier.height(12.dp))

        // Veterinario
        PetFinderTextField(
            value = veterinario,
            onValueChange = { veterinario = it },
            placeholder = "Veterinario o clínica"
        )
        Spacer(Modifier.height(20.dp))

        if (formState is MedicalViewModel.FormState.Error) {
            PetFinderErrorBanner(
                message = formState.message,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }

        Button(
            onClick = { onSave(tipoSeleccionado, descripcion, fecha, veterinario) },
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryOrange,
                contentColor = Color.White
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    if (initial == null) "Guardar registro" else "Actualizar registro",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
