package com.frontend.petfinder.core.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    // Nota: Más adelante conectaremos esto para leer el token guardado en el dispositivo
    private var token: String? = null

    fun setToken(newToken: String?) {
        this.token = newToken
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // Si tenemos un token JWT, lo inyectamos en la cabecera Authorization
        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        return chain.proceed(requestBuilder.build())
    }
}