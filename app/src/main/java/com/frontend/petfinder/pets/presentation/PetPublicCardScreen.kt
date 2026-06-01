package com.frontend.petfinder.pets.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.frontend.petfinder.core.presentation.components.DialogType
import com.frontend.petfinder.core.presentation.components.PetFinderDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.theme.PrimaryOrangeLight
import com.frontend.petfinder.pets.data.dto.ContactoPublicoDto
import com.frontend.petfinder.pets.data.dto.PropietarioPublicoDto
import com.frontend.petfinder.pets.data.dto.PublicPetCardDto
import com.frontend.petfinder.pets.data.dto.RegistroMedicoPublicoDto
import com.frontend.petfinder.sightings.data.SightingDto
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun PetPublicCardScreen(
    token: String,
    onNavigateBack: () -> Unit,
    viewModel: PetPublicCardViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cardState by viewModel.cardState.collectAsStateWithLifecycle()
    val sightings by viewModel.sightings.collectAsStateWithLifecycle()
    val sightingSubmitting by viewModel.sightingSubmitting.collectAsStateWithLifecycle()
    val sightingSuccess by viewModel.sightingSuccess.collectAsStateWithLifecycle()
    val ownerCard by viewModel.ownerCard.collectAsStateWithLifecycle()
    val ownerCardLoading by viewModel.ownerCardLoading.collectAsStateWithLifecycle()

    var feedbackDialog by remember { mutableStateOf<Triple<DialogType, String, String>?>(null) }

    LaunchedEffect(sightingSuccess) {
        if (sightingSuccess) {
            feedbackDialog = Triple(DialogType.SUCCESS, "¡Gracias!", "¡Avistamiento reportado! Gracias por ayudar.")
            viewModel.clearSightingSuccess()
        }
    }

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

    LaunchedEffect(token) {
        viewModel.loadCard(token)
        viewModel.registerScan(token, context)
    }

    // ── Perfil del propietario (bottom sheet) ─────────────────────────────
    if (ownerCard != null || ownerCardLoading) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearOwnerCard() },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.background
        ) {
            if (ownerCardLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            } else {
                ownerCard?.let { card ->
                    OwnerProfileSheet(card = card)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val state = cardState) {
            is PetPublicCardViewModel.CardState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryOrange)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Cargando ficha...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is PetPublicCardViewModel.CardState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = Color(0xFFDDDDDD)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.loadCard(token) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(top = 8.dp, start = 8.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
            }

            is PetPublicCardViewModel.CardState.Success -> {
                PublicCardContent(
                    card = state.card,
                    sightings = sightings,
                    sightingSubmitting = sightingSubmitting,
                    onNavigateBack = onNavigateBack,
                    onContactClick = { contacto ->
                        val intent = when (contacto.tipo.lowercase()) {
                            "whatsapp" -> Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://wa.me/${contacto.valor.filter { it.isDigit() }}")
                            )
                            else -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contacto.valor}"))
                        }
                        context.startActivity(intent)
                    },
                    onReportSighting = { desc, foto ->
                        scope.launch {
                            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                            val cts = CancellationTokenSource()
                            val loc = try {
                                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
                            } catch (_: Exception) { null }
                            viewModel.reportSighting(state.card.mascotaId, loc?.latitude ?: 0.0, loc?.longitude ?: 0.0, desc, foto)
                        }
                    },
                    onOwnerClick = { personaId -> viewModel.loadOwnerCard(personaId) }
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun PublicCardContent(
    card: PublicPetCardDto,
    sightings: List<SightingDto>,
    sightingSubmitting: Boolean,
    onNavigateBack: () -> Unit,
    onContactClick: (ContactoPublicoDto) -> Unit,
    onReportSighting: (String, MultipartBody.Part?) -> Unit,
    onOwnerClick: (String) -> Unit = {}
) {
    val primaryPhoto = card.fotos?.firstOrNull { it.esPrincipal } ?: card.fotos?.firstOrNull()
    val isLost = card.estaExtraviada

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Hero con foto ──
        Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
            if (primaryPhoto != null) {
                AsyncImage(
                    model = primaryPhoto.url,
                    contentDescription = card.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(PrimaryOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = PrimaryOrange.copy(alpha = 0.4f)
                    )
                }
            }

            // Gradiente inferior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                        )
                    )
            )

            // Badge extraviada
            if (isLost) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFD32F2F)
                ) {
                    Text(
                        text = "⚠ MASCOTA EXTRAVIADA",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Botón atrás
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(top = 8.dp, start = 8.dp)
            ) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f)) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp).size(22.dp)
                    )
                }
            }

            // Nombre y datos básicos
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = card.nombre,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                val subtitle = buildList {
                    add(card.tipo)
                    card.sexo?.let { add(sexoLabel(it)) }
                    card.colorPrimario?.let { add(it) }
                }.joinToString(" · ")
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        // ── Cuerpo ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Alerta si extraviada
            if (isLost) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFEBEE)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⚠", fontSize = 28.sp)
                        Column {
                            Text(
                                text = "Esta mascota está perdida",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB71C1C)
                            )
                            Text(
                                text = "Si la encontraste, por favor contacta al dueño.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }

            // Rasgos particulares
            card.rasgosParticulares?.takeIf { it.isNotBlank() }?.let { rasgos ->
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Señas particulares",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = rasgos,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Ficha médica básica
            val tieneFicha = card.fichaMedica != null &&
                (card.fichaMedica.vacunado != null || card.fichaMedica.esterilizado != null ||
                    !card.fichaMedica.condicionEspecial.isNullOrBlank() || !card.fichaMedica.alergia.isNullOrBlank())
            val tieneRegistros = !card.registrosMedicos.isNullOrEmpty()

            if (tieneFicha || tieneRegistros) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Ficha médica",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        card.fichaMedica?.let { ficha ->
                            if (ficha.vacunado != null || ficha.esterilizado != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ficha.vacunado?.let { v ->
                                        MedBadge(label = if (v) "Vacunado" else "Sin vacuna", ok = v)
                                    }
                                    ficha.esterilizado?.let { e ->
                                        MedBadge(label = if (e) "Esterilizado" else "Sin esterilizar", ok = e)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            ficha.condicionEspecial?.takeIf { it.isNotBlank() }?.let {
                                MedInfoRow(label = "Condición especial", value = it)
                            }
                            ficha.alergia?.takeIf { it.isNotBlank() }?.let {
                                MedInfoRow(label = "Alergias", value = it)
                            }
                        }

                        if (tieneRegistros) {
                            if (tieneFicha) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF0F0F0))
                            }
                            Text(
                                text = "Últimos registros",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            card.registrosMedicos!!.take(3).forEach { registro ->
                                PublicMedRecord(registro)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }

            // Dueños
            if (card.propietarios.isNotEmpty()) {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Contactar al dueño",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                card.propietarios.forEach { propietario ->
                    OwnerContactCard(
                        propietario = propietario,
                        onContactClick = onContactClick,
                        onProfileClick = { onOwnerClick(propietario.personaId) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Recompensa
            if (isLost) {
                card.reporteActivo?.recompensa?.let { recompensa ->
                    if (recompensa > 0) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFF8E1)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🏆", fontSize = 28.sp)
                                Column {
                                    Text(
                                        text = "Recompensa ofrecida",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                    Text(
                                        text = "Bs. %.0f".format(recompensa),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFBF360C)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Avistamientos
            if (isLost || sightings.isNotEmpty()) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Avistamientos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))

                var sightingText by remember { mutableStateOf("") }
                var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
                var gpsStatus by remember { mutableStateOf<String?>(null) }

                val photoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri -> selectedPhotoUri = uri }

                // GPS status banner
                if (gpsStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (gpsStatus!!.startsWith("✓")) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (gpsStatus!!.startsWith("✓")) Icons.Default.CheckCircle else Icons.Default.MyLocation,
                                null,
                                tint = if (gpsStatus!!.startsWith("✓")) Color(0xFF2E7D32) else Color(0xFFE65100),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                gpsStatus!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (gpsStatus!!.startsWith("✓")) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = sightingText,
                    onValueChange = { sightingText = it },
                    placeholder = { Text("¿Lo viste? Describe dónde y cuándo...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                // Selected photo preview + actions row
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo picker button
                    OutlinedButton(
                        onClick = { photoPicker.launch("image/*") },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(16.dp), tint = PrimaryOrange)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (selectedPhotoUri != null) "Foto adjunta" else "Adjuntar foto",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryOrange
                        )
                        if (selectedPhotoUri != null) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Close, null,
                                modifier = Modifier.size(14.dp).clickable { selectedPhotoUri = null },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Submit button
                    Button(
                        onClick = {
                            if (!sightingSubmitting) {
                                scope.launch {
                                    gpsStatus = "Obteniendo GPS..."
                                    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                                    val cts = CancellationTokenSource()
                                    val loc = try {
                                        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
                                    } catch (_: Exception) { null }
                                    gpsStatus = if (loc != null) "✓ GPS obtenido (${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)})" else "Sin GPS — se enviará sin coordenadas"

                                    val fotoPart = selectedPhotoUri?.let { uri ->
                                        uriToMultipart(context, uri)
                                    }
                                    onReportSighting(sightingText, fotoPart)
                                    sightingText = ""
                                    selectedPhotoUri = null
                                }
                            }
                        },
                        enabled = sightingText.isNotBlank() && !sightingSubmitting,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (sightingSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reportar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (sightings.isEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sé el primero en reportar un avistamiento",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    sightings.forEach { sighting ->
                        PublicSightingRow(sighting)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Footer branding
            Text(
                text = "Ficha generada por PetFinder",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OwnerContactCard(
    propietario: PropietarioPublicoDto,
    onContactClick: (ContactoPublicoDto) -> Unit,
    onProfileClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.clickable(onClick = onProfileClick)
            ) {
                if (propietario.fotoPerfilUrl != null) {
                    AsyncImage(
                        model = propietario.fotoPerfilUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(PrimaryOrangeLight)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(PrimaryOrangeLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = propietario.nombreCompleto.first().uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange
                        )
                    }
                }

                Column {
                    Text(
                        text = propietario.nombreCompleto,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = tipoRelacionLabel(propietario.tipoRelacion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (propietario.contactos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(modifier = Modifier.height(14.dp))

                propietario.contactos.forEach { contacto ->
                    val isWhatsapp = contacto.tipo.lowercase() == "whatsapp"
                    val bgColor = if (isWhatsapp) Color(0xFF25D366) else PrimaryOrange

                    Button(
                        onClick = { onContactClick(contacto) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = bgColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isWhatsapp) "WhatsApp · ${contacto.valor}" else "Llamar · ${contacto.valor}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    if (contacto != propietario.contactos.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MedBadge(label: String, ok: Boolean) {
    val bg = if (ok) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
    val fg = if (ok) Color(0xFF2E7D32) else Color(0xFFE65100)
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
private fun MedInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(text = value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun PublicMedRecord(registro: RegistroMedicoPublicoDto) {
    val fechaLegible = registro.fecha?.let {
        try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val output = SimpleDateFormat("d MMM yyyy", Locale("es"))
            output.format(input.parse(it)!!)
        } catch (_: Exception) { it.take(10) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(shape = RoundedCornerShape(6.dp), color = PrimaryOrangeLight) {
            Text(
                text = registro.tipo,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            registro.descripcion?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
            }
            if (fechaLegible != null || registro.veterinario != null) {
                Text(
                    text = listOfNotNull(fechaLegible, registro.veterinario?.let { "· $it" }).joinToString(" "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PublicSightingRow(sighting: SightingDto) {
    val fechaLegible = try {
        val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val output = SimpleDateFormat("d MMM · HH:mm", Locale("es"))
        output.format(input.parse(sighting.fechaAvistamiento)!!)
    } catch (_: Exception) { sighting.fechaAvistamiento.take(10) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            sighting.fotoEvidenciaUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Foto del avistamiento",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            sighting.mensajeRescatista?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Anónimo",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = fechaLegible,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OwnerProfileSheet(card: com.frontend.petfinder.profile.data.dto.UserCardDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            if (card.fotoPerfilUrl != null) {
                AsyncImage(
                    model = card.fotoPerfilUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(PrimaryOrangeLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        card.nombreCompleto.first().uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                }
            }
            Column {
                Text(card.nombreCompleto, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                if (card.contactos.isNotEmpty()) {
                    Text(
                        card.contactos.first().valor,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (card.mascotas.isNotEmpty()) {
            Text(
                "Mascotas registradas",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            card.mascotas.forEach { pet ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (pet.fotoUrl != null) {
                            AsyncImage(
                                model = pet.fotoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(PrimaryOrangeLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Pets, null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(pet.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (pet.estado == "extraviada") {
                            Surface(shape = RoundedCornerShape(50), color = Color(0xFFFFEBEE)) {
                                Text("Extraviada", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun uriToMultipart(context: Context, uri: Uri): MultipartBody.Part? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val extension = if (mimeType.contains("png")) "png" else "jpg"
        val tempFile = File.createTempFile("sighting_foto_", ".$extension", context.cacheDir)
        tempFile.outputStream().use { out -> inputStream.copyTo(out) }
        val requestBody = tempFile.asRequestBody(mimeType.toMediaType())
        MultipartBody.Part.createFormData("foto", tempFile.name, requestBody)
    } catch (_: Exception) { null }
}

private fun sexoLabel(sexo: String) = when (sexo.lowercase()) {
    "macho", "m" -> "Macho"
    "hembra", "f" -> "Hembra"
    else -> sexo
}

private fun tipoRelacionLabel(tipo: String) = when (tipo.lowercase()) {
    "propietario" -> "Dueño"
    "cuidador" -> "Cuidador"
    "tutor" -> "Tutor"
    else -> tipo
}
