package com.frontend.petfinder.profile.presentation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import com.frontend.petfinder.PetFinderApp
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.frontend.petfinder.core.presentation.components.DialogType
import com.frontend.petfinder.core.presentation.components.PetFinderDialog
import com.frontend.petfinder.core.presentation.components.PetFinderErrorBanner
import com.frontend.petfinder.core.theme.PrimaryOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val nombre by viewModel.nombre.collectAsStateWithLifecycle()
    val apellidoPaterno by viewModel.apellidoPaterno.collectAsStateWithLifecycle()
    val apellidoMaterno by viewModel.apellidoMaterno.collectAsStateWithLifecycle()

    val rol by PetFinderApp.sessionManager.getUserRole().collectAsStateWithLifecycle(initialValue = null)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.uploadPhoto(context, it) } }

    LaunchedEffect(saveState) {
        when (val s = saveState) {
            is ProfileViewModel.SaveState.Saved -> {
                Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }
            is ProfileViewModel.SaveState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                viewModel.resetSaveState()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (saveState is ProfileViewModel.SaveState.Saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = PrimaryOrange
                        )
                    } else {
                        IconButton(onClick = { viewModel.saveProfile() }) {
                            Icon(Icons.Default.Check, contentDescription = "Guardar", tint = PrimaryOrange)
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = profileState) {
            is ProfileViewModel.ProfileState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = PrimaryOrange) }
            }

            is ProfileViewModel.ProfileState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PetFinderErrorBanner(message = state.message)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProfile() }) { Text("Reintentar") }
                    }
                }
            }

            is ProfileViewModel.ProfileState.Success -> {
                val persona = state.profile.persona
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Foto de perfil con botón de cambio
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        if (persona?.fotoPerfilUrl != null) {
                            AsyncImage(
                                model = persona.fotoPerfilUrl,
                                contentDescription = "Foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        SmallFloatingActionButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier.size(36.dp),
                            containerColor = PrimaryOrange,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Cambiar foto", modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Email (solo lectura)
                    Text(
                        text = state.profile.correoElectronico,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badge de rol
                    rol?.let { userRol ->
                        val isAdmin = userRol == "admin"
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isAdmin) Color(0xFF6200EE) else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (isAdmin) "Administrador" else "Usuario",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isAdmin) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Información personal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { viewModel.onNombreChange(it) },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apellidoPaterno,
                        onValueChange = { viewModel.onApellidoPaternoChange(it) },
                        label = { Text("Apellido Paterno") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apellidoMaterno,
                        onValueChange = { viewModel.onApellidoMaternoChange(it) },
                        label = { Text("Apellido Materno (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Contactos (solo lectura por ahora)
                    if (!persona?.mediosContacto.isNullOrEmpty()) {
                        Text(
                            text = "Contacto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        persona!!.mediosContacto.forEach { contacto ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${contacto.tipo}:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.width(100.dp)
                                )
                                Text(
                                    text = contacto.valor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    var showLogoutDialog by remember { mutableStateOf(false) }

                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                        )
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar sesión")
                    }

                    if (showLogoutDialog) {
                        PetFinderDialog(
                            type = DialogType.DANGER,
                            title = "¿Cerrar sesión?",
                            message = "Se eliminará tu sesión de este dispositivo.",
                            confirmText = "Cerrar sesión",
                            dismissText = "Cancelar",
                            onConfirm = { viewModel.logout() },
                            onDismiss = { showLogoutDialog = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
