package com.frontend.petfinder.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "petfinder_session")

class SessionManager(private val context: Context) {

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_ROL = stringPreferencesKey("user_rol")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        rol: String,
        nombre: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_ROL] = rol
            prefs[KEY_USER_NAME] = nombre
        }
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun saveFcmToken(fcmToken: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FCM_TOKEN] = fcmToken
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    fun getAccessToken(): Flow<String?> =
        context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }

    fun getRefreshToken(): Flow<String?> =
        context.dataStore.data.map { it[KEY_REFRESH_TOKEN] }

    fun getUserId(): Flow<String?> =
        context.dataStore.data.map { it[KEY_USER_ID] }

    fun getUserRole(): Flow<String?> =
        context.dataStore.data.map { it[KEY_USER_ROL] }

    fun getUserName(): Flow<String?> =
        context.dataStore.data.map { it[KEY_USER_NAME] }

    fun getFcmToken(): Flow<String?> =
        context.dataStore.data.map { it[KEY_FCM_TOKEN] }

    fun isSessionValid(): Flow<Boolean> =
        context.dataStore.data.map { !it[KEY_ACCESS_TOKEN].isNullOrEmpty() }
}
