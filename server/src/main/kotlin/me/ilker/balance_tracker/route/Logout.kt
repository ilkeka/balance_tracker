package me.ilker.balance_tracker.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import me.ilker.balance_tracker.Logout
import me.ilker.balance_tracker.database.ServerDB
import me.ilker.balance_tracker.models.MessageResponse
import org.koin.ktor.ext.inject

internal fun Route.logout() {
    val db by inject<ServerDB>()

    post<Logout> {
        val userId = call.principal<UserIdPrincipal>()?.name ?: run {
            call.respond(
                status = HttpStatusCode.NotFound,
                message = MessageResponse("No user id exists for this call.")
            )
            return@post
        }

        db.deleteSessionTokenByUserId(userId)

        call.respond(
            status = HttpStatusCode.OK,
            message = MessageResponse("Logged out successfully")
        )
    }
}
