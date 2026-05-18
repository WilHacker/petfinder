package com.frontend.petfinder

import android.app.Application
import com.frontend.petfinder.core.data.SessionManager

class PetFinderApp : Application() {

    companion object {
        lateinit var sessionManager: SessionManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
    }
}
