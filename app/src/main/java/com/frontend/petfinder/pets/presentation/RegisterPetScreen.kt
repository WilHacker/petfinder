package com.frontend.petfinder.pets.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Registrar Mascota",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = { viewModel.nombre.value = it },
            label = { Text("Nombre de la mascota *") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expandedTipo,
            onExpandedChange = { expandedTipo = !expandedTipo }
        ) {
            OutlinedTextField(
                value = if (tiposMascota.isEmpty()) "Cargando tipos..." else (tipoSeleccionado?.nombre ?: "Toca para seleccionar"),
                onValueChange = {},
                readOnly = true,
                label = { Text("Tipo de mascota *") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expandedTipo,
                onDismissRequest = { expandedTipo = false }
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
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = sexo,
            onValueChange = { viewModel.sexo.value = it },
            label = { Text("Sexo (M o F)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = colorPrimario,
            onValueChange = { viewModel.colorPrimario.value = it },
            label = { Text("Color Primario") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = rasgosParticulares,
            onValueChange = { viewModel.rasgosParticulares.value = it },
            label = { Text("Rasgos Particulares") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Seleccionar Fotos (Máx. 4)")
        }

        if (fotosSeleccionadas.isNotEmpty()) {
            Text(
                text = "${fotosSeleccionadas.size} foto(s) lista(s) para subir.",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )
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
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            Button(
                onClick = { viewModel.registerPet(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Guardar Perfil y Generar QR")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}