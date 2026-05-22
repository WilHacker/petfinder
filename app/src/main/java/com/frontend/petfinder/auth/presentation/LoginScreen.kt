package com.frontend.petfinder.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.frontend.petfinder.R
import com.frontend.petfinder.core.presentation.components.GradientBackground
import com.frontend.petfinder.core.presentation.components.PetFinderButton
import com.frontend.petfinder.core.presentation.components.PetFinderErrorBanner
import com.frontend.petfinder.core.presentation.components.PetFinderTextField

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val correo by viewModel.correo.collectAsStateWithLifecycle()
    val clave by viewModel.clave.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

            // Botón Google Sign-In
            OutlinedButton(
                onClick = { viewModel.loginWithGoogle(context) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFDDDDDD)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3C4043))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continuar con Google",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  o usa tu correo  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Email Minimalista
            PetFinderTextField(
                value = correo,
                onValueChange = { viewModel.onCorreoChange(it) },
                placeholder = "Tu correo electrónico"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Contraseña Minimalista
            PetFinderTextField(
                value = clave,
                onValueChange = { viewModel.onClaveChange(it) },
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
                PetFinderErrorBanner(
                    message = (uiState as LoginViewModel.LoginState.Error).message,
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