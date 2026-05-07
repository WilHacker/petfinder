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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.frontend.petfinder.core.domain.EstadoMascota
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPetsScreen(
    viewModel: MyPetsViewModel = viewModel(),
    onNavigateToRegisterPet: () -> Unit,
    onNavigateToPetZones: (String) -> Unit // Manda el ID de la mascota a la pantalla del Mapa de Zonas
) {
    val pets by viewModel.pets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Cargar la lista automáticamente al entrar a la pantalla
    LaunchedEffect(Unit) {
        viewModel.loadMyPets()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mis Mascotas",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToRegisterPet,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = "Añadir") },
                text = { Text("Añadir Mascota") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            // 1. Estado de Carga
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // 2. Estado de Error (Sin internet)
            else if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadMyPets() }) {
                        Text("Reintentar")
                    }
                }
            }
            // 3. Estado Vacío (Aún no registró a nadie)
            else if (pets.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Pets, contentDescription = "Sin mascotas", tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Aún no tienes mascotas registradas.", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("¡Añade a tu mejor amigo para empezar!", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
            // 4. Lista de Mascotas (¡Éxito!)
            else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(pets) { pet ->
                        PetCard(
                            pet = pet,
                            onClick = { onNavigateToPetZones(pet.mascotaId) }
                        )
                    }
                }
            }
        }
    }
}

// Componente visual independiente para cada Tarjeta de mascota
@Composable
fun PetCard(
    pet: com.frontend.petfinder.pets.data.PetListItemDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto de la mascota usando Coil (AsyncImage)
            val fotoUrl = pet.fotos?.find { it.esPrincipal }?.fotoUrl
            AsyncImage(
                model = fotoUrl ?: "https://res.cloudinary.com/demo/image/upload/v1312461204/sample.jpg", // Imagen por defecto si no tiene foto
                contentDescription = "Foto de ${pet.nombre}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Información de texto
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pet.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = pet.tipoMascota?.nombre ?: "Mascota",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Píldora de Estado Inteligente (Colores automáticos)
                val (estadoText, estadoColor, estadoIcon) = when (pet.estado) {
                    EstadoMascota.EN_CASA.valor -> Triple("En casa", Color(0xFF4CAF50), Icons.Outlined.Home) // Verde
                    EstadoMascota.EN_PASEO.valor -> Triple("En paseo", Color(0xFF2196F3), Icons.Default.Pets) // Azul
                    EstadoMascota.EXTRAVIADA.valor -> Triple("Extraviada", Color(0xFFF44336), Icons.Default.Warning) // Rojo
                    EstadoMascota.RECUPERADA.valor -> Triple("Recuperada", Color(0xFFFF9800), Icons.Default.Pets) // Naranja
                    else -> Triple(pet.estado.replace("_", " ")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        Color.Gray, Icons.Default.Pets)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = estadoColor.copy(alpha = 0.15f), // Fondo semitransparente del mismo color
                    contentColor = estadoColor
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(estadoIcon, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = estadoText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}