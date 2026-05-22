package com.frontend.petfinder.auth.data

import android.util.Log
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.core.network.ApiServices
import com.frontend.petfinder.profile.data.dto.FcmTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException

private const val TAG = "AuthRepository"

object AuthRepository {

    suspend fun login(request: LoginRequest): Result<AuthResponse> = runCatching {
        val r = ApiServices.auth.loginOwner(request)
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }

    suspend fun register(request: RegisterRequest): Result<AuthResponse> = runCatching {
        val r = ApiServices.auth.registerOwner(request)
        if (r.isSuccessful) r.body()!! else throw HttpException(r)
    }

    suspend fun logout(): Result<Unit> = runCatching {
        ApiServices.auth.logout()
        Unit
    }

    suspend fun registrarFcmToken() {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            PetFinderApp.sessionManager.saveFcmToken(fcmToken)
            ApiServices.user.updateFcmToken(FcmTokenRequest(fcmToken))
            Log.d(TAG, "FCM token registrado en el backend")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo registrar el FCM token: ${e.message}")
        }
    }
}
