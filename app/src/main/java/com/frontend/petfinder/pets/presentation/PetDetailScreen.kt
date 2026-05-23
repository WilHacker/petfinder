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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
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
    object AddOwnerSheet : PetDetailModal()
    object CommunityAlertSheet : PetDetailModal()
    object RewardSheet : PetDetailModal()
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
    onNavigateToMedical: () -> Unit = {},
    onNavigateToEdit: () -> Unit = {},
    onViewOnMap: (mascotaId: String) -> Unit = {}
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
    val qrDownloading by viewModel.qrDownloading.collectAsStateWithLifecycle()
    val qrDownloadResult by viewModel.qrDownloadResult.collectAsStateWithLifecycle()
    val ownerError by viewModel.ownerError.collectAsStateWithLifecycle()
    val ownerLoading by viewModel.ownerLoading.collectAsStateWithLifecycle()
    val sightings by viewModel.sightings.collectAsStateWithLifecycle()
    val sightingError by viewModel.sightingError.collectAsStateWithLifecycle()
    val sightingSubmitting by viewModel.sightingSubmitting.collectAsStateWithLifecycle()
    val communityAlertSending by viewModel.communityAlertSending.collectAsStateWithLifecycle()
    val communityAlertResult by viewModel.communityAlertResult.collectAsStateWithLifecycle()
    val communityAlertDialog by viewModel.communityAlertDialog.collectAsStateWithLifecycle()
    val rewardUpdating by viewModel.rewardUpdating.collectAsStateWithLifecycle()
    val rewardResult by viewModel.rewardResult.collectAsStateWithLifecycle()

    var activeModal by remember { mutableStateOf<PetDetailModal>(PetDetailModal.None) }
    var locationSuccess by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var feedbackDialog by remember { mutableStateOf<Triple<DialogType, String, String>?>(null) }

    LaunchedEffect(mascotaId) { viewModel.load(mascotaId) }

    LaunchedEffect(locationError) {
        locationError?.let {
            feedbackDialog = Triple(DialogType.DANGER, "Error de ubicación", it)
            viewModel.clearLocationError()
        }
    }

    LaunchedEffect(ownerError) {
        ownerError?.let {
            feedbackDialog = Triple(DialogType.DANGER, "Error", it)
            viewModel.clearOwnerError()
        }
    }

    LaunchedEffect(sightingError) {
        sightingError?.let {
            feedbackDialog = Triple(DialogType.DANGER, "Error", it)
            viewModel.clearSightingError()
        }
    }

    LaunchedEffect(qrDownloadResult) {
        qrDownloadResult?.let {
            val isError = it.startsWith("No se pudo")
            feedbackDialog = Triple(
                if (isError) DialogType.DANGER else DialogType.SUCCESS,
                if (isError) "Error" else "Listo",
                it
            )
            viewModel.clearQrDownloadResult()
        }
    }

    LaunchedEffect(communityAlertResult) {
        communityAlertResult?.let {
            feedbackDialog = Triple(DialogType.SUCCESS, "Alerta enviada", it)
            viewModel.clearCommunityAlertResult()
        }
    }

    LaunchedEffect(rewardResult) {
        rewardResult?.let {
            val isError = it.startsWith("No se pudo") || it.startsWith("Sin permiso") || it.startsWith("La mascota")
            feedbackDialog = Triple(
                if (isError) DialogType.DANGER else DialogType.SUCCESS,
                if (isError) "Error" else "Recompensa actualizada",
                it
            )
            viewModel.clearRewardResult()
        }
    }

    // ── Feedback genérico (errores, éxitos) ───────────────────────────────
    feedbackDialog?.let { (type, title, message) ->
        PetFinderDialog(
            type = type,
            title = title,
            message = message,
            confirmText = "Entendido",
            onConfirm = { feedbackDialog = null },
            onDismiss = { feedbackDialog = null }
        )
    }

    // ── Dialog alerta comunitaria (razon / sin usuarios) ──────────────────
    communityAlertDialog?.let { dialog ->
        PetFinderDialog(
            type = DialogType.INFO,
            title = dialog.title,
            message = dialog.message,
            confirmText = "Entendido",
            onConfirm = { viewModel.clearCommunityAlertDialog() },
            onDismiss = { viewModel.clearCommunityAlertDialog() }
        )
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
        val petNameForQr = (state as? PetDetailViewModel.DetailState.Success)?.pet?.nombre ?: "mascota"
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
                        else -> {
                            Base64Image(base64String = qrBase64!!, modifier = Modifier.size(220.dp))
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { viewModel.downloadQr(context, mascotaId, petNameForQr) },
                                enabled = !qrDownloading,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (qrDownloading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryOrange)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Icon(Icons.Default.SaveAlt, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Descargar para impresión (800px)")
                            }
                        }
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

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        activeModal = PetDetailModal.None
                        locationSuccess = false
                        showMapPicker = true
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryOrange)
                ) {
                    Icon(Icons.Default.Place, null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Marcar en el mapa", fontWeight = FontWeight.Bold, color = PrimaryOrange)
                }
            }
        }
    }

    // ── Bottom sheet: agregar co-propietario ──────────────────────────────
    if (activeModal is PetDetailModal.AddOwnerSheet) {
        val tiposRelacion = listOf("Cuidador", "Tutor", "Familiar")
        var emailInput by remember { mutableStateOf("") }
        var selectedTipo by remember { mutableStateOf(tiposRelacion[0]) }
        var tipoExpanded by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { activeModal = PetDetailModal.None; emailInput = "" },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text("Agregar cuidador", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ingresa el correo electrónico del usuario que quieres agregar como cuidador.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Correo electrónico") },
                    placeholder = { Text("usuario@ejemplo.com") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(expanded = tipoExpanded, onExpandedChange = { tipoExpanded = !tipoExpanded }) {
                    OutlinedTextField(
                        value = selectedTipo,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de relación") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = tipoExpanded, onDismissRequest = { tipoExpanded = false }) {
                        tiposRelacion.forEach { tipo ->
                            DropdownMenuItem(text = { Text(tipo) }, onClick = { selectedTipo = tipo; tipoExpanded = false })
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.addOwner(mascotaId, emailInput.trim(), selectedTipo)
                        activeModal = PetDetailModal.None
                        emailInput = ""
                    },
                    enabled = emailInput.isNotBlank() && !ownerLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    if (ownerLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Agregar cuidador", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ── Sheet: recompensa del reporte activo ──────────────────────────────
    if (activeModal is PetDetailModal.RewardSheet) {
        val recompensaActual: Double? = remember(reports) {
            reports.firstOrNull()?.recompensa
        }
        var rewardInput by remember { mutableStateOf(recompensaActual?.let { if (it > 0) "%.0f".format(it) else "" } ?: "") }
        var sinRecompensa by remember { mutableStateOf(recompensaActual == 0.0) }

        ModalBottomSheet(
            onDismissRequest = { activeModal = PetDetailModal.None },
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
                        .background(Color(0xFFFFF8E1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFF57F17), modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Recompensa por encontrar a mi mascota",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "La recompensa se muestra en el mapa y en la ficha QR para motivar a la comunidad.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = sinRecompensa,
                        onCheckedChange = {
                            sinRecompensa = it
                            if (it) rewardInput = "0"
                        },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryOrange)
                    )
                    Text(
                        "No ofrezco recompensa económica (Bs. 0)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = rewardInput,
                    onValueChange = {
                        rewardInput = it
                        sinRecompensa = it.trim() == "0"
                    },
                    label = { Text("Monto en bolivianos (Bs.)") },
                    placeholder = { Text("Ej. 200") },
                    prefix = { Text("Bs. ") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    enabled = !sinRecompensa,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(24.dp))

                val monto = if (sinRecompensa) 0.0 else rewardInput.toDoubleOrNull()
                val canSave = sinRecompensa || monto != null && monto >= 0

                Button(
                    onClick = {
                        val amount = if (sinRecompensa) 0.0 else rewardInput.toDoubleOrNull() ?: 0.0
                        viewModel.updateReward(mascotaId, amount)
                        activeModal = PetDetailModal.None
                    },
                    enabled = canSave && !rewardUpdating,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17))
                ) {
                    if (rewardUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (sinRecompensa) "Guardar (sin recompensa)" else "Guardar recompensa",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // ── Sheet: pedir ayuda comunitaria ────────────────────────────────────
    if (activeModal is PetDetailModal.CommunityAlertSheet) {
        var radioSlider by remember { mutableStateOf(5f) }
        ModalBottomSheet(
            onDismissRequest = { activeModal = PetDetailModal.None },
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
                        .background(Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Campaign, null, tint = Color(0xFFE53935), modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Pedir ayuda a la comunidad",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Envía una alerta a usuarios de PetFinder cercanos para que ayuden a buscar a tu mascota.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Radio de búsqueda",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(shape = RoundedCornerShape(8.dp), color = PrimaryOrangeLight) {
                        Text(
                            "${radioSlider.toInt()} km",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange
                        )
                    }
                }
                Slider(
                    value = radioSlider,
                    onValueChange = { radioSlider = it },
                    valueRange = 1f..20f,
                    steps = 18,
                    colors = SliderDefaults.colors(thumbColor = PrimaryOrange, activeTrackColor = PrimaryOrange)
                )

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val petId = (state as? PetDetailViewModel.DetailState.Success)?.pet?.mascotaId ?: mascotaId
                        viewModel.sendCommunityAlert(petId, radioSlider.toInt() * 1000)
                        activeModal = PetDetailModal.None
                    },
                    enabled = !communityAlertSending,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    if (communityAlertSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Campaign, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar alerta", fontWeight = FontWeight.Bold)
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
                sightings = sightings,
                sightingSubmitting = sightingSubmitting,
                onBack = onNavigateBack,
                onShowStatusSheet = { activeModal = PetDetailModal.StatusSheet },
                onShowQr = {
                    activeModal = PetDetailModal.QrDialog
                    viewModel.loadQr(mascotaId)
                },
                onNavigateToMedical = onNavigateToMedical,
                onShowLocationSheet = { activeModal = PetDetailModal.LocationSheet },
                onViewOnMap = { onViewOnMap(mascotaId) },
                onAddOwner = { activeModal = PetDetailModal.AddOwnerSheet },
                onRemoveOwner = { personaId -> viewModel.removeOwner(mascotaId, personaId) },
                onReportSighting = { desc ->
                    scope.launch {
                        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                        val cts = CancellationTokenSource()
                        val loc = try {
                            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
                        } catch (_: Exception) { null }
                        viewModel.reportSighting(mascotaId, loc?.latitude ?: 0.0, loc?.longitude ?: 0.0, desc)
                    }
                },
                onSendThanks = { id, msg -> viewModel.sendThanks(id, msg) },
                onCommunityAlert = { activeModal = PetDetailModal.CommunityAlertSheet },
                onUpdateReward = { activeModal = PetDetailModal.RewardSheet },
                onNavigateToEdit = onNavigateToEdit
            )
        }
    }

    // ── Pantalla completa: selector de ubicación en mapa ─────────────────
    // Renderizado ÚLTIMO para quedar encima de todo el contenido
    if (showMapPicker) {
        val cochabamba = LatLng(-17.3935, -66.1570)
        val mapPickerCameraState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(cochabamba, 14f)
        }
        var mapPickerSaving by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = mapPickerCameraState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = false
                )
            )

            // Pin fijo en el centro
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = PrimaryOrange,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .offset(y = (-24).dp)
            )

            // Botón volver
            SmallFloatingActionButton(
                onClick = { showMapPicker = false },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }

            // Panel inferior con coordenadas + confirmar
            val target = mapPickerCameraState.position.target
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Mueve el mapa para posicionar el pin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "%.5f, %.5f".format(target.latitude, target.longitude),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            mapPickerSaving = true
                            scope.launch {
                                viewModel.updateLocation(mascotaId, target.latitude, target.longitude)
                                mapPickerSaving = false
                                showMapPicker = false
                                locationSuccess = true
                                activeModal = PetDetailModal.LocationSheet
                            }
                        },
                        enabled = !mapPickerSaving,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        if (mapPickerSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Confirmar ubicación", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PetDetailContent(
    pet: PetDetailDto,
    statusChanging: Boolean,
    scans: List<PetScanDto>,
    reports: List<PetReportDto>,
    sightings: List<com.frontend.petfinder.sightings.data.SightingDto> = emptyList(),
    sightingSubmitting: Boolean = false,
    onBack: () -> Unit,
    onShowStatusSheet: () -> Unit,
    onShowQr: () -> Unit,
    onNavigateToMedical: () -> Unit,
    onShowLocationSheet: () -> Unit,
    onViewOnMap: () -> Unit = {},
    onAddOwner: () -> Unit = {},
    onRemoveOwner: (String) -> Unit = {},
    onReportSighting: (String) -> Unit = {},
    onSendThanks: (String, String?) -> Unit = { _, _ -> },
    onCommunityAlert: () -> Unit = {},
    onUpdateReward: () -> Unit = {},
    onNavigateToEdit: () -> Unit = {}
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

            // Badge de estado
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
                val activeReport = reports.firstOrNull()
                val recompensaActiva: Double? = activeReport?.recompensa

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
                        Column(modifier = Modifier.weight(1f)) {
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

                // Reward info row — always shown when there's an active report
                if (activeReport != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (recompensaActiva != null && recompensaActiva > 0) Color(0xFFFFF8E1) else Color(0xFFF5F5F5)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = if (recompensaActiva != null && recompensaActiva > 0) Color(0xFFF57F17) else Color(0xFF9E9E9E),
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Recompensa ofrecida",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF757575)
                                )
                                Text(
                                    if (recompensaActiva != null && recompensaActiva > 0)
                                        "Bs. %.0f".format(recompensaActiva)
                                    else
                                        "Sin recompensa económica",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (recompensaActiva != null && recompensaActiva > 0) Color(0xFFF57F17) else Color(0xFF757575)
                                )
                            }
                            TextButton(onClick = onUpdateReward) {
                                Text("Editar", color = PrimaryOrange, style = MaterialTheme.typography.labelMedium)
                            }
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Dueños y cuidadores",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOrange
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFF0F0F0), thickness = 1.5.dp)
                IconButton(onClick = onAddOwner, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Agregar cuidador", tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                }
            }
            pet.propietarios.forEach { propietario ->
                OwnerRow(
                    propietario = propietario,
                    onRemove = if (propietario.tipoRelacion != "Dueno_Principal") {
                        { onRemoveOwner(propietario.personaId) }
                    } else null
                )
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(8.dp))

            // ── Sección: Acciones rápidas ─────────────────────────────────
            PetSectionHeader("Acciones")

            ActionCard(
                icon = Icons.Default.Edit,
                title = "Editar mascota",
                subtitle = "Modifica datos, fotos y más",
                onClick = onNavigateToEdit
            )
            Spacer(Modifier.height(10.dp))
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
            Spacer(Modifier.height(10.dp))
            ActionCard(
                icon = Icons.Default.Map,
                title = "Ver en el mapa",
                subtitle = "Ir al mapa y centrar en esta mascota",
                onClick = onViewOnMap
            )
            if (estado == "extraviada") {
                Spacer(Modifier.height(10.dp))
                ActionCard(
                    icon = Icons.Default.Campaign,
                    title = "Pedir ayuda a la comunidad",
                    subtitle = "Alerta a usuarios cercanos para que ayuden a buscar",
                    onClick = onCommunityAlert
                )
                Spacer(Modifier.height(10.dp))
                ActionCard(
                    icon = Icons.Default.EmojiEvents,
                    title = "Establecer recompensa",
                    subtitle = "Motiva a la comunidad con una recompensa económica",
                    onClick = onUpdateReward
                )
            }
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

            // ── Sección: Avistamientos ───────────────────────────────────
            if (estado == "extraviada" || sightings.isNotEmpty()) {
                var sightingText by remember { mutableStateOf("") }
                PetSectionHeader("Avistamientos de la comunidad")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = sightingText,
                        onValueChange = { sightingText = it },
                        placeholder = { Text("¿Lo viste? Describe dónde y cuándo...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    IconButton(
                        onClick = { if (sightingText.isNotBlank()) { onReportSighting(sightingText); sightingText = "" } },
                        enabled = sightingText.isNotBlank() && !sightingSubmitting
                    ) {
                        if (sightingSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryOrange)
                        } else {
                            Icon(Icons.Default.Send, null, tint = PrimaryOrange)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                sightings.take(5).forEach { sighting ->
                    SightingRow(sighting = sighting, onThanks = { onSendThanks(sighting.avistamientoId, null) })
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(8.dp))
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
private fun OwnerRow(propietario: PropietarioDetailDto, onRemove: (() -> Unit)? = null) {
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

            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.PersonRemove, contentDescription = "Quitar cuidador", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
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
private fun SightingRow(
    sighting: com.frontend.petfinder.sightings.data.SightingDto,
    onThanks: () -> Unit
) {
    val fechaLegible = remember(sighting.fechaAvistamiento) {
        try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val output = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("es"))
            output.format(input.parse(sighting.fechaAvistamiento)!!)
        } catch (_: Exception) { sighting.fechaAvistamiento.take(10) }
    }

    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(PrimaryOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Visibility, null, tint = PrimaryOrange, modifier = Modifier.size(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Anónimo",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(fechaLegible, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            sighting.mensajeRescatista?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
            }

            if (sighting.fotoEvidenciaUrl != null) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = sighting.fotoEvidenciaUrl,
                    contentDescription = "Foto del avistamiento",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onThanks,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(14.dp), tint = PrimaryOrange)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Gracias",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryOrange
                    )
                }
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
