package com.frontend.petfinder.pets.presentation

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.frontend.petfinder.core.presentation.components.DialogType
import com.frontend.petfinder.core.presentation.components.PetFinderDialog
import com.frontend.petfinder.core.presentation.components.PetFinderErrorBanner
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.theme.PrimaryOrangeLight
import com.frontend.petfinder.pets.data.dto.PetDetailDto
import com.frontend.petfinder.pets.data.dto.PetReportDto
import com.frontend.petfinder.pets.data.dto.PetScanDto
import com.frontend.petfinder.pets.data.dto.PropietarioDetailDto
import com.frontend.petfinder.pets.presentation.components.Base64Image
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

private sealed class PetDetailModal {
    object None : PetDetailModal()
    object StatusSheet : PetDetailModal()
    object QrDialog : PetDetailModal()
    object LostConfirm : PetDetailModal()
    object LocationSheet : PetDetailModal()
}

private fun estadoLabel(estado: String) = when (estado) {
    "en_casa"    -> "En casa"
    "en_paseo"   -> "De paseo"
    "extraviada" -> "Extraviada"
    "recuperada" -> "Recuperada"
    else         -> estado.replaceFirstChar { it.uppercase() }
}

private fun estadoColor(estado: String) = when (estado) {
    "en_casa"    -> Color(0xFF4CAF50)
    "en_paseo"   -> Color(0xFFF18A20)
    "extraviada" -> Color(0xFFE53935)
    else         -> Color(0xFF9E9E9E)
}

private fun sexoLabel(sexo: String?) = when (sexo) {
    "M" -> "Macho"
    "H" -> "Hembra"
    "F" -> "Hembra"
    else -> "—"
}

private fun tipoRelacionLabel(tipo: String) = when (tipo) {
    "Dueno_Principal" -> "Dueño principal"
    "Cuidador"        -> "Cuidador"
    else              -> tipo
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    mascotaId: String,
    viewModel: PetDetailViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToMedical: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val state by viewModel.state.collectAsStateWithLifecycle()
    val qrBase64 by viewModel.qrBase64.collectAsStateWithLifecycle()
    val qrError by viewModel.qrError.collectAsStateWithLifecycle()
    val statusChanging by viewModel.statusChanging.collectAsStateWithLifecycle()
    val scans by viewModel.scans.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val locationUpdating by viewModel.locationUpdating.collectAsStateWithLifecycle()
    val locationError by viewModel.locationError.collectAsStateWithLifecycle()

    var activeModal by remember { mutableStateOf<PetDetailModal>(PetDetailModal.None) }
    var locationSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(mascotaId) { viewModel.load(mascotaId) }

    LaunchedEffect(locationError) {
        locationError?.let { viewModel.clearLocationError() }
    }

    // ── Diálogo confirmación "extraviada" ──────────────────────────────────
    if (activeModal is PetDetailModal.LostConfirm) {
        PetFinderDialog(
            type = DialogType.DANGER,
            title = "¿Reportar como extraviada?",
            message = "Se notificará a la red de usuarios cercanos y se activará el modo de búsqueda.",
            confirmText = "Sí, reportar",
            dismissText = "Cancelar",
            onConfirm = {
                viewModel.updateStatus(mascotaId, "extraviada")
                activeModal = PetDetailModal.None
            },
            onDismiss = { activeModal = PetDetailModal.None }
        )
    }

    // ── Diálogo QR ────────────────────────────────────────────────────────
    if (activeModal is PetDetailModal.QrDialog) {
        PetFinderDialog(
            type = DialogType.INFO,
            title = "Placa QR Activa",
            confirmText = "Cerrar",
            onConfirm = { activeModal = PetDetailModal.None; viewModel.clearQr() },
            onDismiss = { activeModal = PetDetailModal.None; viewModel.clearQr() },
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        qrError != null -> PetFinderErrorBanner("No se pudo cargar el código QR.")
                        qrBase64 == null -> {
                            CircularProgressIndicator(color = PrimaryOrange)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Generando placa...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> Base64Image(base64String = qrBase64!!, modifier = Modifier.size(220.dp))
                    }
                }
            }
        )
    }

    // ── Bottom sheet cambio de estado ─────────────────────────────────────
    if (activeModal is PetDetailModal.StatusSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeModal = PetDetailModal.None },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    "Cambiar estado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(8.dp))

                StatusOption(
                    icon = Icons.Default.Home,
                    label = "En casa",
                    description = "Tu mascota está en casa y segura",
                    color = Color(0xFF4CAF50),
                    onClick = {
                        viewModel.updateStatus(mascotaId, "en_casa")
                        activeModal = PetDetailModal.None
                    }
                )
                StatusOption(
                    icon = Icons.Default.DirectionsWalk,
                    label = "De paseo",
                    description = "Está contigo y comparte tu ubicación",
                    color = PrimaryOrange,
                    onClick = {
                        viewModel.updateStatus(mascotaId, "en_paseo")
                        activeModal = PetDetailModal.None
                    }
                )
                StatusOption(
                    icon = Icons.Default.Warning,
                    label = "Reportar extraviada",
                    description = "Alerta a la red de usuarios cercanos",
                    color = Color(0xFFE53935),
                    onClick = { activeModal = PetDetailModal.LostConfirm }
                )
            }
        }
    }

    // ── Sheet: actualizar ubicación ──────────────────────────────────────
    if (activeModal is PetDetailModal.LocationSheet) {
        ModalBottomSheet(
            onDismissRequest = { activeModal = PetDetailModal.None; locationSuccess = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MyLocation, null, tint = PrimaryOrange, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Actualizar ubicación",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Registra la última posición conocida de la mascota usando el GPS de tu teléfono. Se mostrará en el mapa colaborativo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (locationSuccess) {
                    Spacer(Modifier.height(20.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE8F5E9)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Text("Ubicación actualizada correctamente", color = Color(0xFF2E7D32), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                locationError?.let {
                    Spacer(Modifier.height(20.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFEBEE)) {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(12.dp))
                    }
                }

                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                            val cts = CancellationTokenSource()
                            try {
                                val loc = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
                                if (loc != null) {
                                    viewModel.updateLocation(mascotaId, loc.latitude, loc.longitude)
                                    locationSuccess = true
                                }
                            } catch (_: Exception) {}
                        }
                    },
                    enabled = !locationUpdating,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    if (locationUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Usar mi GPS actual", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ── Contenido principal ───────────────────────────────────────────────
    when (val s = state) {
        is PetDetailViewModel.DetailState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryOrange)
            }
        }

        is PetDetailViewModel.DetailState.Error -> {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFDDDDDD)
                    )
                    Spacer(Modifier.height(16.dp))
                    PetFinderErrorBanner(s.message)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { viewModel.load(mascotaId) }) {
                        Text("Reintentar", color = PrimaryOrange)
                    }
                }
            }
        }

        is PetDetailViewModel.DetailState.Success -> {
            PetDetailContent(
                pet = s.pet,
                statusChanging = statusChanging,
                scans = scans,
                reports = reports,
                onBack = onNavigateBack,
                onShowStatusSheet = { activeModal = PetDetailModal.StatusSheet },
                onShowQr = {
                    activeModal = PetDetailModal.QrDialog
                    viewModel.loadQr(mascotaId)
                },
                onNavigateToMedical = onNavigateToMedical,
                onShowLocationSheet = { activeModal = PetDetailModal.LocationSheet }
            )
        }
    }
}

@Composable
private fun PetDetailContent(
    pet: PetDetailDto,
    statusChanging: Boolean,
    scans: List<PetScanDto>,
    reports: List<PetReportDto>,
    onBack: () -> Unit,
    onShowStatusSheet: () -> Unit,
    onShowQr: () -> Unit,
    onNavigateToMedical: () -> Unit,
    onShowLocationSheet: () -> Unit
) {
    val scrollState = rememberScrollState()
    val imageUrl = pet.fotos?.find { it.esPrincipal }?.fotoUrl
        ?: pet.fotos?.firstOrNull()?.fotoUrl
    val estado = pet.estado

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // ── HERO IMAGE ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Foto de ${pet.nombre}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Pets,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                }
            }

            // Degradado inferior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            // Botón volver — top-start
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(40.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onBack, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.Black)
                }
            }

            // Badge de estado — top-end
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clickable(enabled = !statusChanging) { onShowStatusSheet() },
                shape = RoundedCornerShape(50.dp),
                color = estadoColor(estado).copy(alpha = 0.92f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (estado == "extraviada") {
                        Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                    Text(
                        estadoLabel(estado),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (!statusChanging) {
                        Icon(Icons.Default.ExpandMore, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    } else {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp
                        )
                    }
                }
            }

            // Nombre y tipo — bottom-start
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxWidth(0.72f)
            ) {
                Text(
                    pet.nombre,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                if (pet.tipoMascota != null) {
                    Text(
                        pet.tipoMascota.nombre,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            // Botón QR — bottom-end
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(52.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onShowQr, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = "Ver QR",
                        tint = PrimaryOrange,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // ── CONTENIDO ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
        ) {

            // Alerta si extraviada
            if (estado == "extraviada") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFEBEB)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                "Mascota extraviada",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                "Cambia el estado cuando la encuentres.",
                                color = Color(0xFFE53935).copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // ── Sección: Características ─────────────────────────────────
            PetSectionHeader("Características")

            PetInfoRow(icon = Icons.Default.Pets, label = "Especie", value = pet.tipoMascota?.nombre ?: "—")
            PetInfoRow(
                icon = if (pet.sexo == "M") Icons.Default.Male else Icons.Default.Female,
                label = "Sexo",
                value = sexoLabel(pet.sexo)
            )
            if (!pet.colorPrimario.isNullOrBlank()) {
                PetInfoRow(icon = Icons.Default.Palette, label = "Color", value = pet.colorPrimario)
            }
            if (!pet.rasgosParticulares.isNullOrBlank()) {
                PetInfoRow(icon = Icons.Default.Notes, label = "Rasgos", value = pet.rasgosParticulares)
            }

            Spacer(Modifier.height(24.dp))

            // ── Sección: Dueños y cuidadores ─────────────────────────────
            if (pet.propietarios.isNotEmpty()) {
                PetSectionHeader("Dueños y cuidadores")
                pet.propietarios.forEach { propietario ->
                    OwnerRow(propietario)
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Sección: Acciones rápidas ─────────────────────────────────
            PetSectionHeader("Acciones")

            ActionCard(
                icon = Icons.Default.MedicalServices,
                title = "Historial médico",
                subtitle = "Vacunas, consultas y más",
                onClick = onNavigateToMedical
            )
            Spacer(Modifier.height(10.dp))
            ActionCard(
                icon = Icons.Default.MyLocation,
                title = "Actualizar ubicación",
                subtitle = "Registra la última posición conocida",
                onClick = onShowLocationSheet
            )
            Spacer(Modifier.height(20.dp))

            // ── Sección: Escaneos QR ──────────────────────────────────────
            if (scans.isNotEmpty()) {
                var scansExpanded by remember { mutableStateOf(false) }
                PetSectionHeader("Escaneos QR recientes")
                val visibleScans = if (scansExpanded) scans.take(10) else scans.take(3)
                visibleScans.forEach { scan ->
                    ScanRow(scan)
                    Spacer(Modifier.height(8.dp))
                }
                if (scans.size > 3) {
                    TextButton(onClick = { scansExpanded = !scansExpanded }) {
                        Text(
                            if (scansExpanded) "Ver menos" else "Ver todos (${scans.size})",
                            color = PrimaryOrange,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Sección: Reportes de extravío ─────────────────────────────
            if (reports.isNotEmpty()) {
                PetSectionHeader("Reportes de extravío")
                reports.take(5).forEach { report ->
                    ReportRow(report)
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Fotos adicionales ─────────────────────────────────────────
            val otrasFormas = pet.fotos?.filter { !it.esPrincipal }?.take(3)
            if (!otrasFormas.isNullOrEmpty()) {
                PetSectionHeader("Más fotos")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    otrasFormas.forEach { foto ->
                        AsyncImage(
                            model = foto.fotoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFEAEAEA), RoundedCornerShape(12.dp))
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PetSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = PrimaryOrange
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFF0F0F0), thickness = 1.5.dp)
    }
}

@Composable
private fun PetInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PrimaryOrangeLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun OwnerRow(propietario: PropietarioDetailDto) {
    val persona = propietario.persona
    val contactoPrincipal = persona.mediosContacto.find { it.esPrincipal }
        ?: persona.mediosContacto.firstOrNull()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (persona.fotoPerfilUrl != null) {
                AsyncImage(
                    model = persona.fotoPerfilUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = PrimaryOrange, modifier = Modifier.size(24.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${persona.nombre} ${persona.apellidoPaterno}",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    tipoRelacionLabel(propietario.tipoRelacion),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (contactoPrincipal != null) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = PrimaryOrangeLight
                ) {
                    Text(
                        contactoPrincipal.tipo,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(PrimaryOrangeLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScanRow(scan: PetScanDto) {
    val context = LocalContext.current
    val fechaLegible = remember(scan.escaneadoEl) {
        try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val output = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("es"))
            output.format(input.parse(scan.escaneadoEl)!!)
        } catch (_: Exception) { scan.escaneadoEl }
    }
    val hasCoords = scan.lat != null && scan.lng != null

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.then(
            if (hasCoords) Modifier.clickable {
                val uri = Uri.parse("geo:${scan.lat},${scan.lng}?q=${scan.lat},${scan.lng}(Escaneo QR)")
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } else Modifier
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(PrimaryOrangeLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.QrCodeScanner, null, tint = PrimaryOrange, modifier = Modifier.size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(fechaLegible, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                if (hasCoords) {
                    Text(
                        "%.4f, %.4f — toca para ver en mapa".format(scan.lat, scan.lng),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryOrange
                    )
                } else {
                    Text("Sin ubicación GPS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (hasCoords) {
                Icon(Icons.Default.Map, null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ReportRow(report: PetReportDto) {
    val estadoColor = when (report.estadoReporte.lowercase()) {
        "activo", "perdido" -> Color(0xFFD32F2F)
        "resuelto", "encontrado", "recuperado" -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val estadoLabel = when (report.estadoReporte.lowercase()) {
        "activo", "perdido" -> "Activo"
        "resuelto", "encontrado", "recuperado" -> "Resuelto"
        else -> report.estadoReporte
    }
    val fechaLegible = remember(report.fechaPerdida) {
        try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val output = SimpleDateFormat("d MMM yyyy", Locale("es"))
            output.format(input.parse(report.fechaPerdida)!!)
        } catch (_: Exception) { report.fechaPerdida }
    }

    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(estadoColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, null, tint = estadoColor, modifier = Modifier.size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Reporte #${report.reporteId}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text("Perdida el $fechaLegible", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(50), color = estadoColor.copy(alpha = 0.1f)) {
                Text(
                    estadoLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = estadoColor
                )
            }
        }
    }
}

@Composable
private fun StatusOption(
    icon: ImageVector,
    label: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
