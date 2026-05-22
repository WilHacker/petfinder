package com.frontend.petfinder

import android.app.Application
import com.frontend.petfinder.core.data.SessionManager
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
    }
}
