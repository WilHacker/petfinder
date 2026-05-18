package com.frontend.petfinder.pets.presentation

import androidx.compose.foundation.background
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
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.pets.presentation.components.Base64Image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPetsScreen(
    viewModel: MyPetsViewModel = viewModel(),
    onNavigateToRegisterPet: () -> Unit
) {
    val pets by viewModel.pets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedQr by viewModel.selectedQrBase64.collectAsState()
    val qrError by viewModel.qrErrorMessage.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }

    // ¡EL GATILLO QUE SOLUCIONA TU PROBLEMA!
    LaunchedEffect(Unit) {
        viewModel.loadMyPets()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Mis Mascotas",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )
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
                    .padding(bottom = 80.dp)
                    .shadow(8.dp, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Registrar Mascota")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            if (isLoading && pets.isEmpty()) { // Solo mostramos cargando si la lista está vacía
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryOrange
                )
            } else if (pets.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Pets, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Aún no has registrado mascotas.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(pets, key = { it.mascotaId }) { pet ->
                        val imageUrl = pet.fotos?.find { it.esPrincipal }?.fotoUrl ?: pet.fotos?.firstOrNull()?.fotoUrl

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .shadow(12.dp, RoundedCornerShape(32.dp)),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
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
                                        Icon(Icons.Default.Pets, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                            )
                                        )
                                )

                                val statusColor = when (pet.estado) {
                                    "en_casa" -> Color(0xFF4CAF50)
                                    "en_paseo" -> PrimaryOrange
                                    "extraviada" -> Color(0xFFE53935)
                                    else -> Color.Gray
                                }

                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp),
                                    shape = RoundedCornerShape(50.dp),
                                    color = statusColor.copy(alpha = 0.9f)
                                ) {
                                    Text(
                                        text = pet.estado.uppercase().replace("_", " "),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomStart)
                                        .padding(24.dp),
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
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.loadPetQr(pet.mascotaId)
                                            showQrDialog = true
                                        },
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(Color.White, CircleShape)
                                            .shadow(4.dp, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Ver QR",
                                            tint = PrimaryOrange,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showQrDialog) {
            AlertDialog(
                onDismissRequest = {
                    showQrDialog = false
                    viewModel.clearSelectedQr()
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = { Text("Placa QR Activa", fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (qrError != null) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = qrError!!, color = Color.Red, textAlign = TextAlign.Center)
                        } else if (selectedQr == null) {
                            CircularProgressIndicator(color = PrimaryOrange)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Generando placa...")
                        } else {
                            Base64Image(base64String = selectedQr!!, modifier = Modifier.size(220.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showQrDialog = false
                        viewModel.clearSelectedQr()
                    }) {
                        Text("Cerrar", color = PrimaryOrange, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}