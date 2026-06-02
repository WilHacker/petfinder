package com.frontend.petfinder.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Normalización de números de teléfono y atajos para contactar por WhatsApp /
 * llamada.
 *
 * Los números se guardan en la app sin código de país (ej. "69524395", un móvil
 * boliviano de 8 dígitos). WhatsApp y, en algunos dispositivos, el marcador,
 * exigen el formato internacional. Aquí se antepone el código de Bolivia (591)
 * cuando el número viene local, dejando intacto el que ya trae código.
 */
private const val BOLIVIA_COUNTRY_CODE = "591"

/**
 * Devuelve el número en formato internacional sin el signo "+":
 * "69524395" -> "59169524395"; "+591 6952 4395" -> "59169524395".
 *
 * Si el número ya tiene más de 8 dígitos se asume que incluye el código de país
 * y se respeta tal cual.
 */
fun normalizeBolivianPhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length <= 8) "$BOLIVIA_COUNTRY_CODE$digits" else digits
}

/** Abre WhatsApp hacia [numero] con un [mensaje] opcional pre-rellenado. */
fun openWhatsApp(context: Context, numero: String, mensaje: String? = null) {
    val full = normalizeBolivianPhone(numero)
    val base = "https://wa.me/$full"
    val uri = Uri.parse(if (mensaje.isNullOrBlank()) base else "$base?text=${Uri.encode(mensaje)}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}

/** Abre el marcador del teléfono con [numero] en formato internacional. */
fun openDialer(context: Context, numero: String) {
    val uri = Uri.parse("tel:+${normalizeBolivianPhone(numero)}")
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, uri)) }
}
