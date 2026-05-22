package com.frontend.petfinder.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frontend.petfinder.core.presentation.components.GradientBackground
import com.frontend.petfinder.core.presentation.components.PetFinderButton
import com.frontend.petfinder.core.presentation.components.PetFinderErrorBanner
import com.frontend.petfinder.core.presentation.components.PetFinderTextField

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = viewModel(),
    onNavigateNext: () -> Unit
) {
    val context = LocalContext.current
    val nombre by viewModel.nombre.collectAsStateWithLifecycle()
    val apellidoPaterno by viewModel.apellidoPaterno.collectAsStateWithLifecycle()
    val apellidoMaterno by viewModel.apellidoMaterno.collectAsStateWithLifecycle()
    val ci by viewModel.ci.collectAsStateWithLifecycle()
    val correo by viewModel.correo.collectAsStateWithLifecycle()
    val clave by viewModel.clave.collectAsStateWithLifecycle()
    val telefono by viewModel.telefono.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                onValueChange = { viewModel.onNombreChange(it) },
                placeholder = "Nombre"
            )
            Spacer(modifier = Modifier.height(16.dp))

            PetFinderTextField(
                value = apellidoPaterno,
                onValueChange = { viewModel.onApellidoPaternoChange(it) },
                placeholder = "Apellido Paterno"
            )
            Spacer(modifier = Modifier.height(16.dp))

            PetFinderTextField(
                value = apellidoMaterno,
                onValueChange = { viewModel.onApellidoMaternoChange(it) },
                placeholder = "Apellido Materno"
            )
            Spacer(modifier = Modifier.height(16.dp))

            PetFinderTextField(
                value = ci,
                onValueChange = { viewModel.onCiChange(it) },
                placeholder = "Cédula de Identidad (CI)"
            )
            Spacer(modifier = Modifier.height(16.dp))

            PetFinderTextField(
                value = telefono,
                onValueChange = { viewModel.onTelefonoChange(it) },
                placeholder = "Número de WhatsApp"
            )
            Spacer(modifier = Modifier.height(16.dp))

            PetFinderTextField(
                value = correo,
                onValueChange = { viewModel.onCorreoChange(it) },
                placeholder = "Correo Electrónico"
            )
            Spacer(modifier = Modifier.height(16.dp))

            PetFinderTextField(
                value = clave,
                onValueChange = { viewModel.onClaveChange(it) },
                placeholder = "Contraseña",
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (uiState is RegisterViewModel.RegisterState.Error) {
                PetFinderErrorBanner(
                    message = (uiState as RegisterViewModel.RegisterState.Error).message,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (uiState is RegisterViewModel.RegisterState.Loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                // Nuestro botón naranja redondeado
                PetFinderButton(
                    text = "Registrarme",
                    onClick = { viewModel.registerOwner(context) }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}