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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.frontend.petfinder.auth.data.AuthRepository
import com.frontend.petfinder.core.network.SocketManager
import com.frontend.petfinder.core.navigation.PetFinderNavGraph
import com.frontend.petfinder.core.theme.PetFinderTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    // Referencia al NavController para reenviar App Links cuando la app ya está abierta
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Reconectar socket si la app arranca con sesión ya guardada.
        // Usamos DataStore directamente porque preloadCache() es asíncrono y
        // cachedAccessToken puede ser null en este punto aunque haya sesión guardada.
        lifecycleScope.launch {
            val token = PetFinderApp.sessionManager.getAccessToken().first()
            if (token != null) {
                SocketManager.connect(token, this@MainActivity)
            }
        }

        setContent {
            PetFinderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val nc = rememberNavController()
                    navController = nc
                    PetFinderNavGraph(navController = nc)
                }
            }
        }
    }

    // Recibe el deep link petfinder://auth/callback?accessToken=...&refreshToken=...
    // y App Links https://pet-qr-web.vercel.app/scan/{token} cuando la app ya está abierta
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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
                    AuthRepository.registrarFcmToken()
                    Log.d(TAG, "Google Sign-In completado para userId=$userId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando callback OAuth: ${e.message}", e)
                }
                // PetFinderNavGraph detecta isSessionValid = true y navega a Main automáticamente
            }
        } else {
            // App Link (QR web) o cualquier otro deep link HTTPS — reenviar al NavController
            navController?.handleDeepLink(intent)
        }
    }

}
