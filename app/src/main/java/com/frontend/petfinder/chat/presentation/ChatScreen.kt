package com.frontend.petfinder.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
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
import com.frontend.petfinder.core.theme.BackgroundCream
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.theme.PrimaryOrangeLight
import com.frontend.petfinder.core.theme.SurfaceVariantLight
import com.frontend.petfinder.core.theme.TextDark
import com.frontend.petfinder.core.theme.TextGray
import com.frontend.petfinder.sightings.data.MyParticipationDto
import com.frontend.petfinder.sightings.data.SightingThreadDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenThread: (avistamientoId: String, rescatistaUsuarioId: String, petName: String, rescatistaName: String) -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val myPetsThreads by viewModel.myPetsThreads.collectAsStateWithLifecycle()
    val myParticipations by viewModel.myParticipations.collectAsStateWithLifecycle()
    val unreadOwner by viewModel.unreadOwner.collectAsStateWithLifecycle()
    val unreadRescuer by viewModel.unreadRescuer.collectAsStateWithLifecycle()
    val isLoadingThreads by viewModel.isLoadingThreads.collectAsStateWithLifecycle()
    val isLoadingParticipations by viewModel.isLoadingParticipations.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = BackgroundCream,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chat",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = PrimaryOrange
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Mis mascotas", fontSize = 13.sp)
                            if (unreadOwner > 0) UnreadBadge(unreadOwner)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Ayudé", fontSize = 13.sp)
                            if (unreadRescuer > 0) UnreadBadge(unreadRescuer)
                        }
                    }
                )
            }

            when (selectedTab) {
                0 -> OwnerThreadsTab(
                    threads = myPetsThreads,
                    loading = isLoadingThreads,
                    onRefresh = { viewModel.loadMyPetsThreads() },
                    onOpenThread = { thread ->
                        val avi = thread.avistamiento ?: return@OwnerThreadsTab
                        onOpenThread(
                            avi.avistamientoId,
                            "",
                            thread.mascota.nombre,
                            "Rescatista"
                        )
                        viewModel.onThreadOpened()
                    }
                )
                else -> RescuerParticipationsTab(
                    participations = myParticipations,
                    loading = isLoadingParticipations,
                    onRefresh = { viewModel.loadMyParticipations() },
                    onOpenThread = { participation ->
                        val ownerName = participation.dueno?.nombre ?: "Dueño"
                        onOpenThread(
                            participation.avistamientoId,
                            "",
                            participation.mascota.nombre,
                            ownerName
                        )
                        viewModel.onThreadOpened()
                    }
                )
            }
        }
    }
}

// ── Pestaña dueño — "Mis mascotas" ───────────────────────────────────────────

@Composable
private fun OwnerThreadsTab(
    threads: List<SightingThreadDto>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onOpenThread: (SightingThreadDto) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = PrimaryOrange
            )
            threads.none { it.avistamiento != null } -> EmptyState(
                message = "Sin conversaciones activas",
                subtitle = "Cuando alguien reporte avistamientos\nde tus mascotas, aparecerán aquí."
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = threads.filter { it.avistamiento != null },
                    key = { it.mascota.mascotaId }
                ) { thread ->
                    OwnerThreadItem(thread = thread, onClick = { onOpenThread(thread) })
                    HorizontalDivider(color = SurfaceVariantLight, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun OwnerThreadItem(
    thread: SightingThreadDto,
    onClick: () -> Unit
) {
    val avi = thread.avistamiento ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PetAvatar(fotoUrl = thread.mascota.fotoUrl, name = thread.mascota.nombre)

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = thread.mascota.nombre,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    fontSize = 15.sp
                )
                Text(
                    text = formatDateShort(avi.ultimaActividad),
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = avi.ultimoMensaje ?: "${avi.totalHilos} rescatista(s) reportaron",
                    color = if (avi.noLeidos > 0) TextDark else TextGray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (avi.noLeidos > 0) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (avi.noLeidos > 0) UnreadBadge(avi.noLeidos)
            }
        }
    }
}

// ── Pestaña rescatista — "Ayudé" ─────────────────────────────────────────────

@Composable
private fun RescuerParticipationsTab(
    participations: List<MyParticipationDto>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onOpenThread: (MyParticipationDto) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = PrimaryOrange
            )
            participations.isEmpty() -> EmptyState(
                message = "Aún no ayudaste a nadie",
                subtitle = "Cuando reportes avistamientos\nde mascotas perdidas, aparecerán aquí."
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(participations, key = { it.avistamientoId }) { participation ->
                    RescuerParticipationItem(
                        participation = participation,
                        onClick = { onOpenThread(participation) }
                    )
                    HorizontalDivider(color = SurfaceVariantLight, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun RescuerParticipationItem(
    participation: MyParticipationDto,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PetAvatar(
            fotoUrl = participation.mascota.fotoUrl,
            name = participation.mascota.nombre
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = participation.mascota.nombre,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    fontSize = 15.sp
                )
                Text(
                    text = formatDateShort(participation.ultimaActividad),
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(3.dp))

            val lastMsg = participation.ultimaRespuesta ?: participation.miUltimoMensaje ?: ""
            val hasUnread = participation.noLeidos > 0

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = lastMsg.ifBlank { "Sin respuesta aún" },
                    color = if (hasUnread) TextDark else TextGray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (hasUnread) UnreadBadge(participation.noLeidos)
            }

            participation.dueno?.nombre?.let { ownerName ->
                Text(
                    text = "Dueño: $ownerName",
                    color = TextGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
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

private fun formatDateShort(isoDateTime: String): String {
    return try {
        val datePart = isoDateTime.substringBefore("T")
        val parts = datePart.split("-")
        "${parts[2]}/${parts[1]}"
    } catch (_: Exception) {
        ""
    }
}
