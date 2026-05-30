package com.frontend.petfinder.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Redondeo extremo para imitar el diseño
val AppShapes = Shapes(
    small = RoundedCornerShape(16.dp),  // Para TextFields
    medium = RoundedCornerShape(24.dp), // Para Tarjetas (Cards)
    large = RoundedCornerShape(50.dp)   // Estilo Píldora para Botones
)

// ¡El asesino del Morado! Forzamos todos los contenedores
private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = SurfaceWhite,
    primaryContainer = PrimaryOrangeLight,
    onPrimaryContainer = PrimaryOrange,

    secondary = PrimaryOrange,
    onSecondary = SurfaceWhite,
    secondaryContainer = PrimaryOrangeLight,
    onSecondaryContainer = PrimaryOrange,

    background = BackgroundCream,
    onBackground = TextDark,

    surface = SurfaceWhite,
    onSurface = TextDark,

    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextGray,

    error = ErrorRed
)

// Para este diseño específico, forzamos la paleta clara incluso en modo oscuro temporalmente
private val DarkColorScheme = LightColorScheme

@Composable
fun PetFinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // Usamos el esquema claro por defecto
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Hacemos la barra de estado transparente para permitir edge-to-edge
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}