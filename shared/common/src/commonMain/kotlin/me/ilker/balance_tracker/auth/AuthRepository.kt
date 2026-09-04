package me.ilker.balance_tracker.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.Instant
import me.ilker.balance_tracker.sdk.AuthenticatedUser

internal class AuthRepository(
    private val sessionStorage: SessionStorage
) {
    private val _authenticatedUser = MutableStateFlow<AuthenticatedUser?>(null)
    val authenticatedUser: StateFlow<AuthenticatedUser?> = _authenticatedUser.asStateFlow()

    suspend fun restore() {
        val session = sessionStorage.get() ?: return
        val expiresAt = runCatching { Instant.parse(session.expiresAt) }.getOrNull() ?: return

        if (expiresAt <= Clock.System.now()) {
            sessionStorage.clear()
            return
        }

        _authenticatedUser.value = AuthenticatedUser(
            email = session.email,
            token = session.token,
            expiresAt = expiresAt
        )
    }

    suspend fun save(email: String, token: String, expiresAt: Instant) {
        sessionStorage.save(
            email = email,
            token = token,
            expiresAt = expiresAt.toString()
        )
        _authenticatedUser.value = AuthenticatedUser(email, token, expiresAt)
    }

    suspend fun clear() {
        sessionStorage.clear()
        _authenticatedUser.value = null
    }
}
