package com.frontend.petfinder.pets.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.frontend.petfinder.pets.data.dto.ContactoPublicoDto
import com.frontend.petfinder.pets.data.dto.PropietarioPublicoDto
import com.frontend.petfinder.pets.data.dto.PublicPetCardDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetPublicCardScreen(
    token: String,
    onNavigateBack: () -> Unit,
    viewModel: PetPublicCardViewModel = viewModel()
) {
    val context = LocalContext.current
    val cardState by viewModel.cardState.collectAsState()

    LaunchedEffect(token) {
        viewModel.loadCard(token)
        viewModel.registerScan(token, context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ficha de Mascota") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = cardState) {
                is PetPublicCardViewModel.CardState.Loading -> {
                    CircularProgressIndicator()
                }

                is PetPublicCardViewModel.CardState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadCard(token) }) {
                            Text("Reintentar")
                        }
                    }
                }

                is PetPublicCardViewModel.CardState.Success -> {
                    PetCardContent(
                        card = state.card,
                        onContactClick = { contacto ->
                            val intent = when (contacto.tipo.lowercase()) {
                                "whatsapp" -> Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://wa.me/${contacto.valor.filter { it.isDigit() }}")
                                )
                                else -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contacto.valor}"))
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PetCardContent(
    card: PublicPetCardDto,
    onContactClick: (ContactoPublicoDto) -> Unit
) {
    val primaryPhoto = card.fotos?.firstOrNull { it.esPrincipal } ?: card.fotos?.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Foto principal
        AsyncImage(
            model = primaryPhoto?.url,
            contentDescription = card.nombre,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Badge de extraviada
        if (card.estaExtraviada) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = "MASCOTA EXTRAVIADA",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Nombre
        Text(
            text = card.nombre,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Especie y sexo
        Text(
            text = buildString {
                append(card.tipo)
                card.sexo?.let { append(" · $it") }
                card.colorPrimario?.let { append(" · $it") }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Rasgos particulares
        card.rasgosParticulares?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Propietarios con contacto
        if (card.propietarios.isNotEmpty()) {
            Text(
                text = "Contactar al dueño",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
            card.propietarios.forEach { propietario ->
                OwnerContactCard(propietario = propietario, onContactClick = onContactClick)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun OwnerContactCard(
    propietario: PropietarioPublicoDto,
    onContactClick: (ContactoPublicoDto) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = propietario.fotoPerfilUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = propietario.nombreCompleto,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = propietario.tipoRelacion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (propietario.contactos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                propietario.contactos.forEach { contacto ->
                    Button(
                        onClick = { onContactClick(contacto) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (contacto.tipo.lowercase() == "whatsapp")
                                Color(0xFF25D366) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${contacto.tipo}: ${contacto.valor}")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
