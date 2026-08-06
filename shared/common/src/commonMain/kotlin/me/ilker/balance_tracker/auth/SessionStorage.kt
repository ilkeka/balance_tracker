package me.ilker.balance_tracker.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.time.Clock
import kotlinx.coroutines.flow.first

internal data class PersistedSession(
    val email: String,
    val token: String,
    val expiresAt: String
)

internal class SessionStorage(private val dataStore: DataStore<Preferences>) {

    suspend fun get(): PersistedSession? {
        val preferences = dataStore.data.first()
        val email = preferences[KEY_EMAIL] ?: return null
        val token = preferences[KEY_TOKEN] ?: return null
        val expiresAt = preferences[KEY_EXPIRES_AT] ?: return null
        return PersistedSession(
            email = email,
            token = token,
            expiresAt = expiresAt
        )
    }

    suspend fun save(email: String, token: String, expiresAt: String) {
        dataStore.edit { preferences ->
            preferences[KEY_EMAIL] = email
            preferences[KEY_TOKEN] = token
            preferences[KEY_EXPIRES_AT] = expiresAt
            preferences[KEY_LAST_LOGIN] = Clock.System.now().toString()
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_EMAIL)
            preferences.remove(KEY_TOKEN)
            preferences.remove(KEY_EXPIRES_AT)
            preferences.remove(KEY_LAST_LOGIN)
        }
    }

    private companion object {
        val KEY_EMAIL = stringPreferencesKey("session_email")
        val KEY_TOKEN = stringPreferencesKey("session_token")
        val KEY_EXPIRES_AT = stringPreferencesKey("session_expires_at")
        val KEY_LAST_LOGIN = stringPreferencesKey("session_last_login")
    }
}
