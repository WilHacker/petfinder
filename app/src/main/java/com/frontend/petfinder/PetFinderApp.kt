package com.frontend.petfinder

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.frontend.petfinder.core.data.SessionManager
import com.frontend.petfinder.core.network.SocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PetFinderApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        lateinit var sessionManager: SessionManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        // Pre-carga tokens en memoria para que AuthInterceptor no use runBlocking
        appScope.launch { sessionManager.preloadCache() }

        // Al volver la app a primer plano, garantiza que el socket esté conectado.
        // Con launchMode=singleTask, onCreate no corre al volver de background, así que
        // sin esto el socket caído nunca se recuperaba (había que matar la app).
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                val token = sessionManager.cachedAccessToken ?: return
                Log.d("PetFinderApp", "App en primer plano → ensureConnected")
                SocketManager.ensureConnected(token, this@PetFinderApp)
            }
        })
    }
}
