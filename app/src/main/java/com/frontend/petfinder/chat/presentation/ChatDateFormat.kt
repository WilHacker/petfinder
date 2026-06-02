package com.frontend.petfinder.chat.presentation

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utilidades de formateo de fecha/hora para el chat.
 *
 * El backend entrega los timestamps en UTC (ISO-8601 con sufijo `Z`, ej.
 * "2026-06-02T22:30:00.000Z"). Aquí se parsean como instante absoluto y se
 * convierten a la zona horaria del dispositivo antes de mostrarlos, de modo que
 * el usuario en Bolivia (UTC-4) vea la hora local correcta y no la UTC cruda.
 */
private val deviceZone: ZoneId get() = ZoneId.systemDefault()

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

private val dayMonthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM", Locale.getDefault())

/** Hora local "HH:mm" de un mensaje a partir de su timestamp UTC. */
fun formatMessageTime(isoUtc: String): String = try {
    Instant.parse(isoUtc).atZone(deviceZone).format(timeFormatter)
} catch (_: Exception) {
    ""
}

/** Fecha local "dd/MM" para la lista de chats a partir de un timestamp UTC. */
fun formatChatDateShort(isoUtc: String): String = try {
    Instant.parse(isoUtc).atZone(deviceZone).format(dayMonthFormatter)
} catch (_: Exception) {
    ""
}
