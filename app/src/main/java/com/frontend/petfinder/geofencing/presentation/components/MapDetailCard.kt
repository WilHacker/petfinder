package com.frontend.petfinder.geofencing.presentation.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.frontend.petfinder.core.presentation.components.DialogType
import com.frontend.petfinder.core.presentation.components.PetFinderDialog
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.geofencing.data.*
import com.frontend.petfinder.geofencing.presentation.MapViewModel

// =============================================================================
// CARD DE DETALLE DEL MAPA — estilo place card de Google Maps (móvil)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDetailCard(
    state: MapViewModel.MapCardState,
    onDismiss: () -> Unit,
    onViewPetDetail: (String) -> Unit,
    onLocate: (Double, Double) -> Unit
) {
    // Error → modal amigable
    if (state is MapViewModel.MapCardState.Error) {
        PetFinderDialog(
            type = DialogType.DANGER,
            title = "No disponible",
            message = state.message,
            confirmText = "Entendido",
            onConfirm = onDismiss,
            onDismiss = onDismiss
        )
        return
    }
    if (state is MapViewModel.MapCardState.None) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        when (state) {
            is MapViewModel.MapCardState.Loading -> CardLoading()
            is MapViewModel.MapCardState.Pet -> PetCardContent(state.card, onViewPetDetail, onLocate)
            is MapViewModel.MapCardState.Collaborator -> CollaboratorCardContent(state.card, onLocate)
            else -> {}
        }
    }
}

// ── Estados ──────────────────────────────────────────────────────────────────
@Composable
private fun CardLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PrimaryOrange)
    }
}

// ── Card de MASCOTA ──────────────────────────────────────────────────────────
@Composable
private fun PetCardContent(
    card: MapPetCardDto,
    onViewPetDetail: (String) -> Unit,
    onLocate: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val fotoUrl = card.fotos?.firstOrNull { it.esPrincipal }?.fotoUrl
        ?: card.fotos?.firstOrNull()?.fotoUrl

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {

        HeaderPhoto(fotoUrl, badge = estadoLabel(card.estado), badgeColor = estadoColor(card.estado))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(12.dp))
            Text(card.nombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(
                listOfNotNull(card.tipo, sexoLabel(card.sexo)).joinToString(" · ").ifBlank { "Mascota" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Fila de acciones (estilo Google)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (card.relacion != "comunidad") {
                    CardAction(Icons.Default.Info, "Ver detalle") { onViewPetDetail(card.mascotaId) }
                }
                card.ultimoAvistamiento?.let { av ->
                    if (av.lat != null && av.lng != null) {
                        CardAction(Icons.Default.Place, "Ubicar") { onLocate(av.lat, av.lng) }
                    }
                }
                // Contacto del dueño (solo comunidad)
                card.dueno?.contactoPrincipal?.valor?.let { numero ->
                    CardAction(Icons.Default.Chat, "WhatsApp", tint = Color(0xFF25D366)) {
                        openWhatsApp(context, numero, "Hola, vi a tu mascota ${card.nombre} en PetFinder.")
                    }
                    CardAction(Icons.Default.Call, "Llamar") { openDialer(context, numero) }
                }
                CardAction(Icons.Default.Share, "Compartir") {
                    sharePet(context, card)
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))

            // Recompensa
            if ((card.recompensa ?: 0.0) > 0.0) {
                InfoRow(Icons.Default.Payments, "Recompensa: Bs. %.0f".format(card.recompensa), tint = Color(0xFF2E7D32))
            }
            // Color / rasgos
            card.colorPrimario?.takeIf { it.isNotBlank() }?.let {
                InfoRow(Icons.Default.Palette, it)
            }
            card.rasgosParticulares?.takeIf { it.isNotBlank() }?.let {
                InfoRow(Icons.Default.Notes, it)
            }
            // Último avistamiento
            card.ultimoAvistamiento?.let { av ->
                if (av.lat != null && av.lng != null) {
                    InfoRow(
                        Icons.Default.Visibility,
                        "Último avistamiento" + (av.fecha?.let { " · ${fechaCorta(it)}" } ?: ""),
                        onClick = { onLocate(av.lat, av.lng) }
                    )
                }
            }
            // Contacto del dueño (comunidad)
            card.dueno?.let { d ->
                val nombre = listOfNotNull(d.nombre, d.apellidoPaterno).joinToString(" ")
                InfoRow(Icons.Default.Person, "Dueño: $nombre".trim())
            }
            // Propietarios (tuya / compartida)
            if (!card.propietarios.isNullOrEmpty()) {
                OwnersRow(card.propietarios)
            }
            // Ficha médica (solo tuya y si existe)
            card.fichaMedica?.let { FichaMedicaSection(it) }
        }
    }
}

// ── Card de COLABORADOR ──────────────────────────────────────────────────────
@Composable
private fun CollaboratorCardContent(
    card: MapCollaboratorCardDto,
    onLocate: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val nombreCompleto = listOfNotNull(card.nombre, card.apellidoPaterno).joinToString(" ")
    val principal = card.mediosContacto?.firstOrNull { it.esPrincipal } ?: card.mediosContacto?.firstOrNull()
    val emergencia = card.mediosContacto?.firstOrNull { it.esEmergencia }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {

        HeaderPhoto(card.fotoPerfilUrl, fallbackIcon = Icons.Default.Person)

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(12.dp))
            Text(nombreCompleto.ifBlank { "Colaborador" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            card.tipoRelacion?.let {
                Spacer(Modifier.height(2.dp))
                Text(relacionLabel(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                card.ubicacion?.let { u ->
                    CardAction(Icons.Default.Place, "Ubicar") { onLocate(u.lat, u.lng) }
                }
                principal?.valor?.let { numero ->
                    CardAction(Icons.Default.Chat, "WhatsApp", tint = Color(0xFF25D366)) {
                        openWhatsApp(context, numero, "Hola $nombreCompleto, te contacto desde PetFinder.")
                    }
                    CardAction(Icons.Default.Call, "Llamar") { openDialer(context, numero) }
                }
                emergencia?.valor?.let { numero ->
                    CardAction(Icons.Default.Emergency, "Emergencia", tint = Color(0xFFE53935)) {
                        openDialer(context, numero)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))

            // Mascotas compartidas
            if (!card.mascotasCompartidas.isNullOrEmpty()) {
                InfoRow(Icons.Default.Pets, "Comparten ${card.mascotasCompartidas.size} mascota(s)")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 38.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    card.mascotasCompartidas.forEach { m ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Avatar(m.fotoUrl, Icons.Default.Pets, 44.dp)
                            Text(
                                m.nombre ?: "—",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(48.dp)
                            )
                        }
                    }
                }
            }
            // Medios de contacto
            card.mediosContacto?.forEach { medio ->
                InfoRow(
                    icon = if (medio.esEmergencia) Icons.Default.Emergency else Icons.Default.Phone,
                    text = "${medio.tipo ?: "Contacto"}: ${medio.valor ?: "—"}" +
                        if (medio.esEmergencia) " · Emergencia" else "",
                    tint = if (medio.esEmergencia) Color(0xFFE53935) else PrimaryOrange,
                    onClick = medio.valor?.let { v -> { openDialer(context, v) } }
                )
            }
            // Ubicación
            if (card.ubicacion == null) {
                InfoRow(Icons.Default.LocationOff, "Sin ubicación compartida", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Sub-componentes ──────────────────────────────────────────────────────────
@Composable
private fun HeaderPhoto(
    url: String?,
    badge: String? = null,
    badgeColor: Color = PrimaryOrange,
    fallbackIcon: ImageVector = Icons.Default.Pets
) {
    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(PrimaryOrange.copy(alpha = 0.85f), PrimaryOrange.copy(alpha = 0.55f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(fallbackIcon, null, tint = Color.White, modifier = Modifier.size(64.dp))
            }
        }
        if (badge != null) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                shape = RoundedCornerShape(50),
                color = badgeColor
            ) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CardAction(icon: ImageVector, label: String, tint: Color = PrimaryOrange, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String,
    tint: Color = PrimaryOrange,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OwnersRow(propietarios: List<MapCardPropietarioDto>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Group, null, tint = PrimaryOrange, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            propietarios.take(4).forEach { p ->
                Avatar(p.fotoUrl, Icons.Default.Person, 36.dp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "${propietarios.size} responsable(s)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FichaMedicaSection(ficha: MapCardFichaMedicaDto) {
    val campos = listOfNotNull(
        ficha.alergias?.takeIf { it.isNotBlank() }?.let { "Alergias: $it" },
        ficha.enfermedadesCronicas?.takeIf { it.isNotBlank() }?.let { "Crónicas: $it" },
        ficha.medicacionDiaria?.takeIf { it.isNotBlank() }?.let { "Medicación: $it" },
        ficha.tipoSangre?.takeIf { it.isNotBlank() }?.let { "Tipo de sangre: $it" },
        ficha.notasVeterinarias?.takeIf { it.isNotBlank() }?.let { "Notas: $it" }
    )
    HorizontalDivider(color = Color(0xFFEEEEEE))
    InfoRow(Icons.Default.MedicalServices, "Ficha médica", tint = Color(0xFFD32F2F))
    if (campos.isEmpty()) {
        Text(
            "Sin datos médicos registrados",
            modifier = Modifier.padding(start = 38.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        campos.forEach {
            Text(
                "• $it",
                modifier = Modifier.padding(start = 38.dp, bottom = 6.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun Avatar(url: String?, fallback: ImageVector, size: androidx.compose.ui.unit.Dp) {
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(PrimaryOrange.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(fallback, null, tint = PrimaryOrange, modifier = Modifier.size(size * 0.55f))
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────
private fun estadoLabel(e: String) = when (e) {
    "en_casa" -> "En casa"
    "en_paseo" -> "En paseo"
    "extraviada" -> "Extraviada"
    "encontrada" -> "Encontrada"
    else -> e
}

private fun estadoColor(e: String) = when (e) {
    "extraviada" -> Color(0xFFE53935)
    "en_paseo" -> Color(0xFF1565C0)
    "encontrada" -> Color(0xFF2E7D32)
    else -> Color(0xFF2E7D32)
}

private fun sexoLabel(s: String?) = when (s?.uppercase()) {
    "M" -> "Macho"
    "F" -> "Hembra"
    else -> null
}

private fun relacionLabel(r: String) = when (r) {
    "Dueno_Principal" -> "Dueño principal"
    "Co_Propietario" -> "Co-propietario"
    "Cuidador" -> "Cuidador"
    else -> r
}

// Recorta una fecha ISO ("2026-06-01T20:00:00.000Z") a "2026-06-01"
private fun fechaCorta(iso: String) = iso.take(10)

private fun openWhatsApp(context: Context, numero: String, mensaje: String) {
    val digits = numero.filter { it.isDigit() }
    val full = if (digits.length <= 8) "591$digits" else digits // Bolivia +591
    val uri = Uri.parse("https://wa.me/$full?text=${Uri.encode(mensaje)}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}

private fun openDialer(context: Context, numero: String) {
    val uri = Uri.parse("tel:${numero.filter { it.isDigit() || it == '+' }}")
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, uri)) }
}

private fun sharePet(context: Context, card: MapPetCardDto) {
    val texto = buildString {
        append("${card.nombre} (${card.tipo ?: "mascota"}) — ${estadoLabel(card.estado)}")
        if ((card.recompensa ?: 0.0) > 0.0) append("\nRecompensa: Bs. %.0f".format(card.recompensa))
        append("\nCompartido desde PetFinder")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Compartir")) }
}
