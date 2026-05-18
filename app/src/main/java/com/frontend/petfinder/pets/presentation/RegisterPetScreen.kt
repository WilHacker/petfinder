package com.frontend.petfinder.pets.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frontend.petfinder.core.presentation.components.GradientBackground
import com.frontend.petfinder.core.presentation.components.PetFinderButton
import com.frontend.petfinder.core.presentation.components.PetFinderTextField
import com.frontend.petfinder.core.theme.PrimaryOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPetScreen(
    viewModel: RegisterPetViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val nombre by viewModel.nombre.collectAsState()
    val tiposMascota by viewModel.tiposMascota.collectAsState()
    val tipoSeleccionado by viewModel.tipoSeleccionado.collectAsState()

    val sexo by viewModel.sexo.collectAsState()
    val colorPrimario by viewModel.colorPrimario.collectAsState()
    val rasgosParticulares by viewModel.rasgosParticulares.collectAsState()
    val fotosSeleccionadas by viewModel.fotosSeleccionadas.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var expandedTipo by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        viewModel.fotosSeleccionadas.value = uris
    }

    LaunchedEffect(uiState) {
        if (uiState is RegisterPetViewModel.RegisterPetState.Success) {
            onNavigateBack()
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Registrar Mascota",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Crea el perfil de tu compañero",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            PetFinderTextField(
                value = nombre,
                onValueChange = { viewModel.nombre.value = it },
                placeholder = "Nombre de la mascota *"
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Dropdown estilizado para que combine con PetFinderTextField
            ExposedDropdownMenuBox(
                expanded = expandedTipo,
                onExpandedChange = { expandedTipo = !expandedTipo }
            ) {
                OutlinedTextField(
                    value = if (tiposMascota.isEmpty()) "Cargando tipos..." else (tipoSeleccionado?.nombre ?: "Tipo de mascota *"),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryOrange)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = Color(0xFFEAEAEA),
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedTipo,
                    onDismissRequest = { expandedTipo = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    tiposMascota.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo.nombre) },
                            onClick = {
                                viewModel.tipoSeleccionado.value = tipo
                                expandedTipo = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            PetFinderTextField(
                value = sexo,
                onValueChange = { viewModel.sexo.value = it },
                placeholder = "Sexo (M o H)" // Macho o Hembra
            )
            Spacer(modifier = Modifier.height(16.dp))

            PetFinderTextField(
                value = colorPrimario,
                onValueChange = { viewModel.colorPrimario.value = it },
                placeholder = "Color Primario"
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Este lo dejamos normal por si necesita múltiples líneas, pero con los nuevos colores
            OutlinedTextField(
                value = rasgosParticulares,
                onValueChange = { viewModel.rasgosParticulares.value = it },
                placeholder = { Text("Rasgos Particulares", color = Color.Gray) },
                shape = RoundedCornerShape(16.dp),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = Color(0xFFEAEAEA)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Selector de Fotos Premium
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = PrimaryOrange)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (fotosSeleccionadas.isNotEmpty()) "${fotosSeleccionadas.size} fotos seleccionadas" else "Seleccionar Fotos (Máx. 4)",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (fotosSeleccionadas.isNotEmpty()) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState is RegisterPetViewModel.RegisterPetState.Error) {
                Text(
                    text = (uiState as RegisterPetViewModel.RegisterPetState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (uiState is RegisterPetViewModel.RegisterPetState.Loading) {
                CircularProgressIndicator(color = PrimaryOrange)
            } else {
                PetFinderButton(
                    text = "Guardar y Generar QR",
                    onClick = { viewModel.registerPet(context) }
                )

                // Botón cancelar
                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Cancelar", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}