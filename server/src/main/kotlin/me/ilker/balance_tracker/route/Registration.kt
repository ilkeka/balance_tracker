package me.ilker.balance_tracker.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveNullable
import io.ktor.server.request.receiveOrNull
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import me.ilker.balance_tracker.Registration
import me.ilker.balance_tracker.models.RegistrationRequest

internal fun Route.registration() {
    post<Registration> {
        val request = runCatching { call.receiveNullable<RegistrationRequest>() }
            .getOrNull()
            ?: run {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = "Request is ${call.request}"
                )
                return@post
            }


    }
}