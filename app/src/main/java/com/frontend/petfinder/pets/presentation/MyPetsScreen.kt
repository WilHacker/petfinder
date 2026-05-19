package com.frontend.petfinder.pets.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.frontend.petfinder.core.presentation.components.DialogType
import com.frontend.petfinder.core.presentation.components.PetFinderButton
import com.frontend.petfinder.core.presentation.components.PetFinderDialog
import com.frontend.petfinder.core.presentation.components.PetFinderErrorBanner
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.pets.presentation.components.Base64Image

private fun estadoLabel(estado: String): String = when (estado) {
    "en_casa" -> "En casa"
    "en_paseo" -> "De paseo"
    "extraviada" -> "Extraviada"
    else -> estado.replaceFirstChar { it.uppercase() }
}

private fun estadoColor(estado: String): Color = when (estado) {
    "en_casa" -> Color(0xFF4CAF50)
    "en_paseo" -> PrimaryOrange
    "extraviada" -> Color(0xFFE53935)
    else -> Color(0xFF9E9E9E)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPetsScreen(
    viewModel: MyPetsViewModel = viewModel(),
    onNavigateToRegisterPet: () -> Unit,
    onNavigateToPetDetail: (String) -> Unit = {}
) {
    val pets by viewModel.pets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedQr by viewModel.selectedQrBase64.collectAsState()
    val qrError by viewModel.qrErrorMessage.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadMyPets()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Mis Mascotas",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (pets.isNotEmpty()) {
                            Text(
                                text = "${pets.size} ${if (pets.size == 1) "mascota" else "mascotas"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToRegisterPet,
                containerColor = PrimaryOrange,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp)
                    .shadow(8.dp, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Registrar Mascota")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            when {
                isLoading && pets.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryOrange
                    )
                }

                pets.isEmpty() -> {
                    // Empty state con CTA
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Pets,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFFDDDDDD)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Aún no tienes mascotas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Registra a tu compañero para generar su placa QR y mantenerlo protegido.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        PetFinderButton(
                            text = "Registrar mi primera mascota",
                            onClick = onNavigateToRegisterPet
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 24.dp, end = 24.dp,
                            top = 8.dp, bottom = 120.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(pets, key = { it.mascotaId }) { pet ->
                            val imageUrl = pet.fotos?.find { it.esPrincipal }?.fotoUrl
                                ?: pet.fotos?.firstOrNull()?.fotoUrl
                            val status = pet.estado ?: "desconocido"
                            val isExtraviada = status == "extraviada"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clickable { onNavigateToPetDetail(pet.mascotaId) }
                                    .shadow(
                                        elevation = if (isExtraviada) 16.dp else 8.dp,
                                        shape = RoundedCornerShape(28.dp),
                                        ambientColor = if (isExtraviada) Color(0xFFE53935).copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.08f),
                                        spotColor = if (isExtraviada) Color(0xFFE53935).copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.12f)
                                    ),
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Foto o placeholder
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
                                                modifier = Modifier.size(64.dp),
                                                tint = Color(0xFFCCCCCC)
                                            )
                                        }
                                    }

                                    // Degradado inferior
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.75f)
                                                    )
                                                )
                                            )
                                    )

                                    // Badge de estado — top-end
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(14.dp),
                                        shape = RoundedCornerShape(50.dp),
                                        color = estadoColor(status).copy(alpha = 0.92f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isExtraviada) {
                                                Icon(
                                                    Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Text(
                                                text = estadoLabel(status),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }

                                    // Info + botón QR — bottom
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomStart)
                                            .padding(horizontal = 20.dp, vertical = 18.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pet.nombre,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = pet.tipoMascota?.nombre ?: "Mascota",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.75f)
                                            )
                                        }

                                        // Botón QR
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .shadow(4.dp, CircleShape)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.loadPetQr(pet.mascotaId)
                                                    showQrDialog = true
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.QrCodeScanner,
                                                    contentDescription = "Ver QR",
                                                    tint = PrimaryOrange,
                                                    modifier = Modifier.size(26.dp)
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

        // Diálogo QR
        if (showQrDialog) {
            PetFinderDialog(
                type = DialogType.INFO,
                title = "Placa QR Activa",
                confirmText = "Cerrar",
                onConfirm = {
                    showQrDialog = false
                    viewModel.clearSelectedQr()
                },
                onDismiss = {
                    showQrDialog = false
                    viewModel.clearSelectedQr()
                },
                content = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when {
                            qrError != null -> {
                                PetFinderErrorBanner(
                                    message = "No se pudo cargar el código QR. Inténtalo de nuevo."
                                )
                            }
                            selectedQr == null -> {
                                CircularProgressIndicator(color = PrimaryOrange)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Generando placa...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            else -> {
                                Base64Image(base64String = selectedQr!!, modifier = Modifier.size(220.dp))
                            }
                        }
                    }
                }
            )
        }
    }
}
