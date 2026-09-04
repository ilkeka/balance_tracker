package me.ilker.balance_tracker.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveNullable
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import me.ilker.balance_tracker.Login
import me.ilker.balance_tracker.database.ServerDB
import me.ilker.balance_tracker.models.AuthTokenResponse
import me.ilker.balance_tracker.models.LoginRequest
import me.ilker.balance_tracker.models.MessageResponse
import me.ilker.balance_tracker.passwordVerify
import org.koin.ktor.ext.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@ExperimentalUuidApi
internal fun Route.login() {
    val db by inject<ServerDB>()

    post<Login> {
        val request = runCatching { call.receiveNullable<LoginRequest>() }
            .getOrNull()
            ?: run {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = MessageResponse("Invalid request body")
                )
                return@post
            }

        val user = db.getUserByEmail(request.email.value)

        if (user == null || !passwordVerify(request.password.value, user.password)) {
            call.respond(
                status = HttpStatusCode.Unauthorized,
                message = MessageResponse("Invalid email or password")
            )
            return@post
        }

        val token = Uuid.generateV4().toString()
        val expiresAt = Clock.System.now() + 30.days

        db.deleteSessionTokenByUserId(user.id)
        db.createSessionToken(
            token = token,
            userId = user.id,
            expiresAt = expiresAt.toString(),
            createdAt = Clock.System.now().toString()
        )

        call.respond(
            status = HttpStatusCode.OK,
            message = AuthTokenResponse(
                token = token,
                expiresAt = expiresAt.toString(),
                message = "Login successful"
            )
        )
    }
}
