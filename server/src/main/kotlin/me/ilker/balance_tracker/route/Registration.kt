package me.ilker.balance_tracker.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveNullable
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import me.ilker.balance_tracker.Registration
import me.ilker.balance_tracker.database.ServerDB
import me.ilker.balance_tracker.models.AuthTokenResponse
import me.ilker.balance_tracker.models.LoginRequest
import me.ilker.balance_tracker.models.MessageResponse
import me.ilker.balance_tracker.passwordHash
import org.koin.ktor.ext.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@ExperimentalUuidApi
internal fun Route.registration() {
    val db by inject<ServerDB>()

    post<Registration> {
        val request = runCatching { call.receiveNullable<LoginRequest>() }
            .getOrNull()
            ?: run {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = MessageResponse("Invalid request body")
                )
                return@post
            }

        val existingUser = db.getUserByEmail(request.email.value)

        if (existingUser != null) {
            call.respond(
                status = HttpStatusCode.Conflict,
                message = MessageResponse("An account with this email already exists")
            )
            return@post
        }

        val id = Uuid.generateV4().toString()

        db.createUser(
            id = id,
            email = request.email.value,
            password = passwordHash(request.password.value),
            createdAt = Clock.System.now().toString()
        )

        val token = Uuid.generateV4().toString()
        val expiresAt = Clock.System.now() + 30.days

        db.deleteSessionTokenByUserId(id)
        db.createSessionToken(
            token = token,
            userId = id,
            expiresAt = expiresAt.toString(),
            createdAt = Clock.System.now().toString()
        )

        call.respond(
            status = HttpStatusCode.Created,
            message = AuthTokenResponse(
                token = token,
                expiresAt = expiresAt.toString(),
                message = "Registration successful"
            )
        )
    }
}
