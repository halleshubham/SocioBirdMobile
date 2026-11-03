package com.postiz.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "postiz_session")

/**
 * What the self-hoster typed in on the Connect screen:
 *  - serverUrl: their domain/IP (e.g. "postiz.mycompany.com" or "192.168.1.20:5000")
 *  - apiToken:  raw API key from Postiz Settings -> Developer, OR an OAuth2
 *               token (which the Postiz API expects prefixed with "pos_").
 *               Either one is sent verbatim in the Authorization header.
 *  - isCloud:   true if pointed at api.postiz.com (path is /public/v1,
 *               no /api prefix); false (default) for self-hosted (/api/public/v1).
 */
data class Session(
    val serverUrl: String = "",
    val apiToken: String = "",
    val isCloud: Boolean = false
) {
    val isValid: Boolean get() = serverUrl.isNotBlank() && apiToken.isNotBlank()
}

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val API_TOKEN = stringPreferencesKey("api_token")
        val IS_CLOUD = booleanPreferencesKey("is_cloud")
    }

    val sessionFlow: Flow<Session> = context.dataStore.data.map { prefs ->
        Session(
            serverUrl = prefs[Keys.SERVER_URL].orEmpty(),
            apiToken = prefs[Keys.API_TOKEN].orEmpty(),
            isCloud = prefs[Keys.IS_CLOUD] ?: false
        )
    }

    suspend fun currentSession(): Session = sessionFlow.first()

    suspend fun saveSession(serverUrl: String, apiToken: String, isCloud: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = serverUrl.trim()
            prefs[Keys.API_TOKEN] = apiToken.trim()
            prefs[Keys.IS_CLOUD] = isCloud
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
