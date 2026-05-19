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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.theme.PrimaryOrangeLight
import com.frontend.petfinder.pets.data.dto.ContactoPublicoDto
import com.frontend.petfinder.pets.data.dto.FichaMedicaPublicaDto
import com.frontend.petfinder.pets.data.dto.PropietarioPublicoDto
import com.frontend.petfinder.pets.data.dto.PublicPetCardDto
import com.frontend.petfinder.pets.data.dto.RegistroMedicoPublicoDto
import java.text.SimpleDateFormat
import java.util.Locale

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
                    modifier = Modifier.padding(top = 40.dp, start = 8.dp).align(Alignment.TopStart)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
            }

            is PetPublicCardViewModel.CardState.Success -> {
                PublicCardContent(
                    card = state.card,
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
                    }
                )
            }
        }
    }
}

@Composable
private fun PublicCardContent(
    card: PublicPetCardDto,
    onNavigateBack: () -> Unit,
    onContactClick: (ContactoPublicoDto) -> Unit
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
                        .padding(top = 56.dp),
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
                modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp, start = 8.dp)
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
                    OwnerContactCard(propietario = propietario, onContactClick = onContactClick)
                    Spacer(modifier = Modifier.height(12.dp))
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
    onContactClick: (ContactoPublicoDto) -> Unit
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
