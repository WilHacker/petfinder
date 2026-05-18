package com.frontend.petfinder.pets.presentation.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun Base64Image(
    base64String: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Código QR de la mascota"
) {
    // Usamos remember para decodificar solo cuando el string Base64 cambie
    val imageBitmap = remember(base64String) {
        try {
            // El backend envía el prefijo "data:image/png;base64,". Debemos limpiarlo.
            val cleanBase64 = if (base64String.contains(",")) {
                base64String.split(",")[1]
            } else {
                base64String
            }

            // Decodificamos de Base64 a ByteArray, y luego a Bitmap nativo de Android
            val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            bitmap?.asImageBitmap() // Convertimos a ImageBitmap de Compose
        } catch (e: Exception) {
            null
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        // Fallback visual en caso de que el Base64 sea inválido o esté corrupto
        Box(
            modifier = modifier
                .size(200.dp)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error al cargar QR",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}