package com.frontend.petfinder.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frontend.petfinder.core.presentation.components.GradientBackground
import com.frontend.petfinder.core.presentation.components.PetFinderButton
import com.frontend.petfinder.core.presentation.components.PetFinderTextField

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = viewModel(),
    onNavigateNext: () -> Unit
) {
    val nombre by viewModel.nombre.collectAsState()
    val apellidoPaterno by viewModel.apellidoPaterno.collectAsState()
    val apellidoMaterno by viewModel.apellidoMaterno.collectAsState()
    val ci by viewModel.ci.collectAsState()
    val correo by viewModel.correo.collectAsState()
    val clave by viewModel.clave.collectAsState()
    val telefono by viewModel.telefono.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is RegisterViewModel.RegisterState.Success) {
            onNavigateNext()
        }
    }

    val scrollState = rememberScrollState()

    // Envolvemos todo en nuestro nuevo fondo degradado
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Crear Cuenta",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Únete a la comunidad PetFinder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Campos de texto usando nuestro componente minimalista
            PetFinderTextField(
                value = nombre,
                onValueChange = { viewModel.nombre.value = it },
                placeholder = "Nombre"
            )
            Spacer(modifier = Modifier.height(16.dp))

            PetFinderTextField(
                value = apellidoPaterno,
                onValueChange = { viewModel.apellidoPaterno.value = it },
                placeholder = "Apellido Paterno"
            )
            Spacer(modifier = Modifier.height(16.dp))

            PetFinderTextField(
                value = apellidoMaterno,
                onValueChange = { viewModel.apellidoMaterno.value = it },
                placeholder = "Apellido Materno"
            )
            Spacer(modifier = Modifier.height(16.dp))

            PetFinderTextField(
                value = ci,
                onValueChange = { viewModel.ci.value = it },
                placeholder = "Cédula de Identidad (CI)"
            )
            Spacer(modifier = Modifier.height(16.dp))

            PetFinderTextField(
                value = telefono,
                onValueChange = { viewModel.telefono.value = it },
                placeholder = "Número de WhatsApp"
            )
            Spacer(modifier = Modifier.height(16.dp))

            PetFinderTextField(
                value = correo,
                onValueChange = { viewModel.correo.value = it },
                placeholder = "Correo Electrónico"
            )
            Spacer(modifier = Modifier.height(16.dp))

            PetFinderTextField(
                value = clave,
                onValueChange = { viewModel.clave.value = it },
                placeholder = "Contraseña",
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (uiState is RegisterViewModel.RegisterState.Error) {
                Text(
                    text = (uiState as RegisterViewModel.RegisterState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (uiState is RegisterViewModel.RegisterState.Loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                // Nuestro botón naranja redondeado
                PetFinderButton(
                    text = "Registrarme",
                    onClick = { viewModel.registerOwner() }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}