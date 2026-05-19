package com.frontend.petfinder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.frontend.petfinder.core.network.RetrofitClient
import com.frontend.petfinder.core.network.SocketManager
import com.frontend.petfinder.core.navigation.PetFinderNavGraph
import com.frontend.petfinder.core.theme.PetFinderTheme
import com.frontend.petfinder.profile.data.UserApi
import com.frontend.petfinder.profile.data.dto.FcmTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetFinderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    PetFinderNavGraph(navController = navController)
                }
            }
        }
    }

    // Recibe el deep link petfinder://auth/callback?accessToken=...&refreshToken=...
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = intent.data ?: return
        if (uri.scheme == "petfinder" && uri.host == "auth" && uri.path == "/callback") {
            val accessToken = uri.getQueryParameter("accessToken") ?: return
            val refreshToken = uri.getQueryParameter("refreshToken") ?: return
            val userId = uri.getQueryParameter("userId") ?: return
            val rol = uri.getQueryParameter("rol") ?: "usuario"
            val nombre = uri.getQueryParameter("nombre") ?: ""

            lifecycleScope.launch {
                try {
                    PetFinderApp.sessionManager.saveSession(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        userId = userId,
                        rol = rol,
                        nombre = nombre
                    )
                    SocketManager.connect(accessToken, this@MainActivity)
                    registrarFcmToken()
                    Log.d(TAG, "Google Sign-In completado para userId=$userId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando callback OAuth: ${e.message}", e)
                }
                // PetFinderNavGraph detecta isSessionValid = true y navega a Main automáticamente
            }
        }
    }

    private suspend fun registrarFcmToken() {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            PetFinderApp.sessionManager.saveFcmToken(fcmToken)
            RetrofitClient.instance.create(UserApi::class.java)
                .updateFcmToken(FcmTokenRequest(fcmToken))
        } catch (e: Exception) {
            Log.w(TAG, "FCM token post Google Sign-In: ${e.message}")
        }
    }
}
