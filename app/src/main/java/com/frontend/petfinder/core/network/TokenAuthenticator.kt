package com.frontend.petfinder.core.network

import android.util.Log
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.auth.data.AuthApi
import com.frontend.petfinder.auth.data.RefreshTokenRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "TokenAuthenticator"

class TokenAuthenticator : Authenticator {

    // Retrofit limpio sin AuthInterceptor para evitar bucles infinitos en el refresh
    private val refreshApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://backend-petfinder.onrender.com/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Si la request ya no tenía header de auth, no hay token que renovar
        if (response.request.header("Authorization") == null) return null

        // OkHttp llama a authenticate() hasta 3 veces por defecto. Limitamos a 1 intento.
        if (response.responseCount() >= 2) {
            Log.w(TAG, "Token refresh fallido 2 veces — cerrando sesión")
            runBlocking { PetFinderApp.sessionManager.clearSession() }
            return null
        }

        val refreshToken = runBlocking {
            PetFinderApp.sessionManager.getRefreshToken().first()
        } ?: run {
            Log.w(TAG, "No hay refreshToken guardado — cerrando sesión")
            return null
        }

        return try {
            val refreshResponse = runBlocking {
                refreshApi.refreshToken(RefreshTokenRequest(refreshToken))
            }

            if (refreshResponse.isSuccessful) {
                val body = refreshResponse.body()!!
                runBlocking {
                    PetFinderApp.sessionManager.updateTokens(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken
                    )
                }
                Log.d(TAG, "Token renovado exitosamente")
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${body.accessToken}")
                    .build()
            } else {
                Log.w(TAG, "Refresh rechazado por el servidor (${refreshResponse.code()}) — cerrando sesión")
                runBlocking { PetFinderApp.sessionManager.clearSession() }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error de red durante el refresh: ${e.message}", e)
            null
        }
    }

    private fun Response.responseCount(): Int {
        var count = 1
        var prior = this.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
