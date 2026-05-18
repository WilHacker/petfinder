package com.frontend.petfinder.core.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHandler {

    // 1. Permisos base de ubicación (Primer plano)
    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // 2. Permiso de notificaciones (Solo requerido en Android 13 / API 33+)
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else null

    /**
     * Verifica si el usuario ya concedió la ubicación precisa ("Mientras la app está en uso").
     */
    fun hasBasicLocation(context: Context): Boolean {
        return locationPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Verifica si el usuario ya concedió el permiso de notificaciones.
     * Retorna true automáticamente en Android 12 o inferior.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return notificationPermission?.let {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        } ?: true
    }

    /**
     * Verifica si la cascada completa está aprobada y el motor de rastreo puede iniciar.
     * Para un Foreground Service, "Mientras la app está en uso" + "Notificaciones" es suficiente.
     */
    fun isReadyForTracking(context: Context): Boolean {
        return hasBasicLocation(context) && hasNotificationPermission(context)
    }
}