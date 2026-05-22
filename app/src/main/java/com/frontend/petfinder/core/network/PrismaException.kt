package com.frontend.petfinder.core.network

import java.io.IOException

class PrismaException(message: String, val code: String? = null) : IOException(message)

fun Throwable.toPrismaMessage(): String? = (this as? PrismaException)?.message