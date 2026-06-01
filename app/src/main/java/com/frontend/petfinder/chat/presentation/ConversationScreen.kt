package com.frontend.petfinder.chat.presentation

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.frontend.petfinder.chat.data.ChatEstado
import com.frontend.petfinder.chat.data.ChatMessageDto
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.theme.TextDark
import com.frontend.petfinder.core.theme.TextGray
import com.frontend.petfinder.core.utils.ImageUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversacionId: String,
    onNavigateBack: () -> Unit,
    viewModel: ConversationViewModel = viewModel()
) {
    LaunchedEffect(conversacionId) { viewModel.load(conversacionId) }

    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val sending by viewModel.sending.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var photoLat by remember { mutableStateOf<Double?>(null) }
    var photoLng by remember { mutableStateOf<Double?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedPhotoUri = uri
        if (uri != null) {
            scope.launch {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                val loc = runCatching {
                    fusedClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token
                    ).await()
                }.getOrNull()
                photoLat = loc?.latitude
                photoLng = loc?.longitude
            }
        } else {
            photoLat = null
            photoLng = null
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(messages.lastIndex) }
        }
    }

    val header = remember(detail, currentUserId) { viewModel.headerParticipant() }
    val estado = detail?.estado
    val isActive = estado == ChatEstado.ACEPTADA

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = header?.nombreCompleto ?: "Chat",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                        )
                        detail?.mascota?.nombre?.let {
                            Text(
                                text = "sobre $it 🐾",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextGray),
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            when {
                viewModel.esInvitacionPendienteParaMi -> InviteActionBar(
                    onAccept = { viewModel.accept() },
                    onDecline = { viewModel.decline(onDeclined = onNavigateBack) }
                )
                estado == ChatEstado.PENDIENTE -> InfoBar("Esperando que el rescatista acepte la invitación…")
                estado == ChatEstado.RECHAZADA -> InfoBar("La invitación fue rechazada. El chat no está disponible.")
                else -> ChatInputBar(
                    inputText = inputText,
                    onInputChange = { inputText = it },
                    selectedPhotoUri = selectedPhotoUri,
                    photoLat = photoLat,
                    sending = sending,
                    enabled = isActive,
                    onAttachPhoto = { photoLauncher.launch("image/*") },
                    onClearPhoto = {
                        selectedPhotoUri = null
                        photoLat = null
                        photoLng = null
                    },
                    onSend = {
                        if (inputText.isNotBlank() || selectedPhotoUri != null) {
                            val mensajeToSend = inputText.trim().takeIf { it.isNotBlank() }
                            val capturedUri = selectedPhotoUri
                            val capturedLat = photoLat
                            val capturedLng = photoLng
                            inputText = ""
                            selectedPhotoUri = null
                            photoLat = null
                            photoLng = null
                            scope.launch(Dispatchers.IO) {
                                val fotoPart = capturedUri?.let { uri ->
                                    runCatching {
                                        ImageUtils.processImagesForUpload(context, listOf(uri), "foto")
                                            .firstOrNull()
                                    }.getOrNull()
                                }
                                withContext(Dispatchers.Main) {
                                    viewModel.sendMessage(
                                        contenido = mensajeToSend,
                                        foto = fotoPart,
                                        lat = capturedLat,
                                        lng = capturedLng
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryOrange
                )
                messages.isEmpty() -> EmptyConversation(estado = estado)
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.mensajeId }) { msg ->
                        MessageBubble(message = msg, isMe = viewModel.isMe(msg))
                    }
                }
            }

            error?.let { msg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK", color = PrimaryOrange)
                        }
                    },
                    containerColor = Color(0xFF323232)
                ) {
                    Text(msg, color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Banner de invitación (rescatista) ─────────────────────────────────────────

@Composable
private fun InviteActionBar(onAccept: () -> Unit, onDecline: () -> Unit) {
    Surface(shadowElevation = 8.dp, color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Text(
                "Te invitaron a este chat privado. ¿Aceptas?",
                fontSize = 13.sp,
                color = TextDark,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Rechazar", color = TextGray) }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) { Text("Aceptar", color = Color.White) }
            }
        }
    }
}

@Composable
private fun InfoBar(text: String) {
    Surface(shadowElevation = 8.dp, color = Color.White) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, fontSize = 13.sp, color = TextGray, textAlign = TextAlign.Center)
        }
    }
}

// ── Barra de input ────────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    selectedPhotoUri: Uri?,
    photoLat: Double?,
    sending: Boolean,
    enabled: Boolean,
    onAttachPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    onSend: () -> Unit
) {
    val canSend = (inputText.isNotBlank() || selectedPhotoUri != null) && !sending && enabled

    Surface(shadowElevation = 8.dp, color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            if (selectedPhotoUri != null) {
                PhotoPreviewBar(
                    uri = selectedPhotoUri,
                    hasLocation = photoLat != null,
                    onClear = onClearPhoto
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onAttachPhoto,
                    enabled = enabled,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (selectedPhotoUri != null) Color(0xFFFDE8D4)
                            else Color(0xFFF0F0F0)
                        )
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Adjuntar foto",
                        tint = if (selectedPhotoUri != null) PrimaryOrange else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Escribe un mensaje…", color = TextGray, fontSize = 14.sp)
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedBorderColor = PrimaryOrange
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                IconButton(
                    onClick = { if (canSend) onSend() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (canSend) PrimaryOrange else Color(0xFFE0E0E0)),
                    enabled = canSend
                ) {
                    if (sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Enviar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoPreviewBar(uri: Uri, hasLocation: Boolean, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            Text("Foto adjunta", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark)
            Text(
                text = if (hasLocation) "📍 Ubicación capturada" else "Sin ubicación",
                fontSize = 11.sp,
                color = if (hasLocation) PrimaryOrange else TextGray
            )
        }
        IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Quitar foto",
                tint = TextGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
}

// ── Burbuja de mensaje ────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: ChatMessageDto, isMe: Boolean) {
    val bubbleColor = if (isMe) PrimaryOrange else Color.White
    val textColor = if (isMe) Color.White else TextDark
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val shape = if (isMe)
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    else
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isMe && message.autor != null) {
            val name = message.autor.nombreCompleto
            if (name.isNotBlank()) {
                Text(
                    text = name,
                    fontSize = 11.sp,
                    color = PrimaryOrange,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
        }

        Surface(
            shape = shape,
            color = bubbleColor,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                message.fotoUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Foto",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(6.dp))
                }

                message.contenido?.takeIf { it.isNotBlank() }?.let { texto ->
                    Text(text = texto, color = textColor, fontSize = 14.sp, lineHeight = 19.sp)
                }

                val time = remember(message.creadoEl) { formatTime(message.creadoEl) }
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.lat != null && message.lng != null) {
                        Text(
                            "📍",
                            fontSize = 9.sp,
                            color = if (isMe) Color.White.copy(alpha = 0.7f) else TextGray
                        )
                    }
                    Text(
                        text = time,
                        fontSize = 10.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else TextGray
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyConversation(estado: String?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val (title, sub) = when (estado) {
            ChatEstado.PENDIENTE -> "Invitación pendiente" to "Cuando se acepte la invitación,\npodrán intercambiar mensajes."
            ChatEstado.RECHAZADA -> "Chat no disponible" to "La invitación fue rechazada."
            else -> "Sin mensajes aún" to "Escribe el primer mensaje\npara coordinar el rescate."
        }
        Text(title, color = TextGray, fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = sub,
            color = Color(0xFFBBBBBB),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

private fun formatTime(isoDateTime: String): String {
    return try {
        val timePart = isoDateTime.substringAfter("T").substringBefore(".")
        timePart.substring(0, 5)
    } catch (_: Exception) { "" }
}
