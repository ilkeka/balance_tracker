package me.ilker.balance_tracker.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.util.logging.error

internal fun Application.configStatusPages() {
    install(StatusPages) {
//        exception<AuthenticationException> { call, _ ->
//            call.respond(HttpStatusCode.Unauthorized)
//        }
//        exception<AuthorizationException> { call, _ ->
//            call.respond(HttpStatusCode.Forbidden)
//        }
//        exception<RequestValidationException> { call, cause ->
//            call.respond(HttpStatusCode.BadRequest, cause.reasons.joinToString("\n"))
//        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest)
        }
        exception<Exception> { call, cause ->
            call.application.environment.log.error(cause)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
