package com.frontend.petfinder.core.network

import java.io.IOException

/**
 * Excepción personalizada para mapear los errores de base de datos del backend.
 */
class PrismaException(message: String, val code: String? = null) : IOException(message)