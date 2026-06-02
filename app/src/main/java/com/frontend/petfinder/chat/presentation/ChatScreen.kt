package com.frontend.petfinder.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.frontend.petfinder.chat.data.ChatEstado
import com.frontend.petfinder.chat.data.ChatSummaryDto
import com.frontend.petfinder.core.theme.BackgroundCream
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.theme.PrimaryOrangeLight
import com.frontend.petfinder.core.theme.SurfaceVariantLight
import com.frontend.petfinder.core.theme.TextDark
import com.frontend.petfinder.core.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenChat: (conversacionId: String) -> Unit,
    viewModel: ChatListViewModel = viewModel()
) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        containerColor = BackgroundCream,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chats",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                loading && chats.isEmpty() -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryOrange
                )
                chats.isEmpty() -> EmptyState(
                    message = "Sin conversaciones",
                    subtitle = "Cuando inicies un chat con un rescatista —o alguien acepte el tuyo— aparecerá aquí."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(chats, key = { it.conversacionId }) { chat ->
                        ChatRow(chat = chat, onClick = { onOpenChat(chat.conversacionId) })
                        HorizontalDivider(color = SurfaceVariantLight, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: ChatSummaryDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PetAvatar(fotoUrl = chat.mascota.fotoUrl, name = chat.mascota.nombre)

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = chat.mascota.nombre,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    fontSize = 15.sp
                )
                chat.ultimaActividad?.let {
                    Text(formatChatDateShort(it), color = TextGray, fontSize = 11.sp)
                }
            }

            // Nombre del otro participante
            chat.otroParticipante?.nombreCompleto?.let { nombre ->
                Text(
                    text = nombre,
                    color = TextGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusPreview(chat = chat, modifier = Modifier.weight(1f))
                if (chat.estado == ChatEstado.ACEPTADA && chat.noLeidos > 0) {
                    UnreadBadge(chat.noLeidos)
                } else if (chat.estado == ChatEstado.PENDIENTE) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = "Pendiente",
                        tint = TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPreview(chat: ChatSummaryDto, modifier: Modifier = Modifier) {
    val hasUnread = chat.estado == ChatEstado.ACEPTADA && chat.noLeidos > 0
    val (text, color) = when (chat.estado) {
        ChatEstado.PENDIENTE ->
            (if (chat.soyDueno) "Invitación enviada…" else "Te invitaron a chatear") to TextGray
        ChatEstado.RECHAZADA -> "Invitación rechazada" to Color(0xFFC0392B)
        else -> (chat.ultimoMensaje ?: "📷 Multimedia") to (if (hasUnread) TextDark else TextGray)
    }
    Text(
        text = text,
        color = color,
        fontSize = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
        modifier = modifier
    )
}

// ── Shared components ─────────────────────────────────────────────────────────

@Composable
private fun PetAvatar(fotoUrl: String?, name: String) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(PrimaryOrangeLight),
        contentAlignment = Alignment.Center
    ) {
        if (fotoUrl != null) {
            AsyncImage(
                model = fotoUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(CircleShape)
            .background(PrimaryOrange),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun EmptyState(message: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = Color(0xFFDDDDDD),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(message, color = TextGray, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = Color(0xFFBBBBBB),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

