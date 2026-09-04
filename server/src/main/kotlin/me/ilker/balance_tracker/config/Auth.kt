package me.ilker.balance_tracker.config

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer
import kotlin.time.Clock
import kotlin.time.Instant
import me.ilker.balance_tracker.database.ServerDB
import org.koin.ktor.ext.inject

internal fun Application.configAuthentication() {
    val db by inject<ServerDB>()

    install(Authentication) {
        bearer("auth-bearer") {
            authenticate { credential ->
                val session = db.getSessionToken(credential.token) ?: return@authenticate null
                if (Instant.parse(session.expiresAt) <= Clock.System.now()) return@authenticate null
                UserIdPrincipal(session.userId)
            }
        }
    }
}
