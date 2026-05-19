package com.frontend.petfinder.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.frontend.petfinder.core.theme.BackgroundCream

// 1. FONDO CON DEGRADADO (Como en el diseño)
@Composable
fun GradientBackground(content: @Composable BoxScope.() -> Unit) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF0D4), // Tono durazno/amarillento muy suave arriba
            BackgroundCream    // Crema/Blanco hacia abajo
        )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
        content = content
    )
}

// 2. BOTÓN ESTILO PÍLDORA
@Composable
fun PetFinderButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(4.dp, RoundedCornerShape(50.dp)), // Sombra suave
        shape = RoundedCornerShape(50.dp), // Forma de píldora
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

// 3. MODAL UNIFICADO
enum class DialogType { DEFAULT, DANGER, INFO }

@Composable
fun PetFinderDialog(
    type: DialogType = DialogType.DEFAULT,
    title: String,
    message: String? = null,
    confirmText: String,
    dismissText: String? = null,
    icon: ImageVector? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable (() -> Unit)? = null
) {
    val containerColor = when (type) {
        DialogType.DANGER -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val onContainerColor = when (type) {
        DialogType.DANGER -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val resolvedIcon: ImageVector? = icon ?: when (type) {
        DialogType.DANGER -> Icons.Default.Warning
        DialogType.INFO -> Icons.Default.Info
        DialogType.DEFAULT -> null
    }
    val iconTint = when (type) {
        DialogType.DANGER -> MaterialTheme.colorScheme.error
        DialogType.INFO -> MaterialTheme.colorScheme.primary
        DialogType.DEFAULT -> MaterialTheme.colorScheme.primary
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        shape = RoundedCornerShape(24.dp),
        icon = resolvedIcon?.let {
            { Icon(it, contentDescription = null, tint = iconTint, modifier = Modifier.size(36.dp)) }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = onContainerColor
            )
        },
        text = {
            Column {
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onContainerColor.copy(alpha = 0.85f)
                    )
                }
                if (content != null) {
                    if (message != null) Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        },
        confirmButton = {
            val confirmColors = when (type) {
                DialogType.DANGER -> ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
                else -> ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
            Button(onClick = onConfirm, colors = confirmColors) {
                Text(confirmText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = dismissText?.let {
            {
                TextButton(onClick = onDismiss) {
                    Text(it, color = onContainerColor.copy(alpha = 0.75f))
                }
            }
        }
    )
}

// 4. BANNER DE ERROR AMIGABLE
@Composable
fun PetFinderErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

// 5. CAMPO DE TEXTO MINIMALISTA
@Composable
fun PetFinderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.Gray) },
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color(0xFFEAEAEA), // Borde gris casi invisible
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = modifier.fillMaxWidth()
    )
}