package com.frontend.petfinder.core.network

import com.frontend.petfinder.PetFinderApp
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Lee del caché en memoria — sin runBlocking, sin bloquear el thread de OkHttp.
        // El caché se pre-carga en PetFinderApp y se actualiza en cada saveSession/updateTokens.
        val token = PetFinderApp.sessionManager.cachedAccessToken
        val request = chain.request().newBuilder().apply {
            token?.let { addHeader("Authorization", "Bearer $it") }
        }.build()
        return chain.proceed(request)
    }
}
