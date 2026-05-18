package com.frontend.petfinder.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frontend.petfinder.core.presentation.components.GradientBackground
import com.frontend.petfinder.core.presentation.components.PetFinderButton
import com.frontend.petfinder.core.presentation.components.PetFinderTextField

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val correo by viewModel.correo.collectAsState()
    val clave by viewModel.clave.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is LoginViewModel.LoginState.Success) {
            onLoginSuccess()
        }
    }

    // Usamos nuestro nuevo fondo con degradado
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp), // Márgenes laterales más amplios como en el diseño
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Título principal
            Text(
                text = "Iniciar Sesión",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "O usando tu correo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Email Minimalista
            PetFinderTextField(
                value = correo,
                onValueChange = { viewModel.correo.value = it },
                placeholder = "Tu correo electrónico"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Contraseña Minimalista
            PetFinderTextField(
                value = clave,
                onValueChange = { viewModel.clave.value = it },
                placeholder = ".........",
                visualTransformation = PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(
                        onClick = { /* Aquí iría la lógica de recuperar clave */ },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "¿Olvidaste tu clave?",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState is LoginViewModel.LoginState.Error) {
                Text(
                    text = (uiState as LoginViewModel.LoginState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (uiState is LoginViewModel.LoginState.Loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                // Nuestro nuevo Botón Naranja
                PetFinderButton(
                    text = "Ingresar",
                    onClick = { viewModel.login(context) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Texto de registro en la parte inferior
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "¿Eres nuevo aquí? ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = onNavigateToRegister,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Regístrate",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}