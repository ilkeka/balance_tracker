package me.ilker.balance_tracker.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveNullable
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlin.time.Clock
import me.ilker.balance_tracker.LinkRoute
import me.ilker.balance_tracker.LinkTokenRoute
import me.ilker.balance_tracker.database.ServerDB
import me.ilker.balance_tracker.models.LinkTokenRequest
import me.ilker.balance_tracker.models.LinkTokenResponse
import me.ilker.balance_tracker.models.MessageResponse
import me.ilker.balance_tracker.models.UserSession
import org.koin.ktor.ext.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@ExperimentalUuidApi
internal fun Route.linkToken() {
    val db by inject<ServerDB>()

    get<LinkTokenRoute> {
        val session = call.sessions.get<UserSession>()
            ?: run {
                call.respond(
                    status = HttpStatusCode.Unauthorized,
                    message = MessageResponse("Not authenticated")
                )
                return@get
            }

        val token = Uuid.generateV4().toString()

        db.createLinkToken(
            token = token,
            ownerId = session.userId,
            createdAt = Clock.System.now().toString()
        )

        call.respond(
            status = HttpStatusCode.OK,
            message = LinkTokenResponse(token)
        )
    }
}

@ExperimentalUuidApi
internal fun Route.link() {
    val db by inject<ServerDB>()

    post<LinkRoute> {
        val session = call.sessions.get<UserSession>()
            ?: run {
                call.respond(
                    status = HttpStatusCode.Unauthorized,
                    message = MessageResponse("Not authenticated")
                )
                return@post
            }

        val request = runCatching { call.receiveNullable<LinkTokenRequest>() }
            .getOrNull()
            ?: run {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = MessageResponse("Invalid request body")
                )
                return@post
            }

        val linkToken = db.getLinkToken(request.token)
            ?: run {
                call.respond(
                    status = HttpStatusCode.NotFound,
                    message = MessageResponse("Invalid or expired link token")
                )
                return@post
            }

        if (linkToken.ownerId == session.userId) {
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = MessageResponse("You cannot link your account to yourself")
            )
            return@post
        }

        val existingLink = db.getAccountLink(
            ownerId = linkToken.ownerId,
            linkedId = session.userId
        )

        if (existingLink != null) {
            call.respond(
                status = HttpStatusCode.Conflict,
                message = MessageResponse("Account already linked")
            )
            return@post
        }

        db.createAccountLink(
            id = Uuid.generateV4().toString(),
            ownerId = linkToken.ownerId,
            linkedId = session.userId,
            createdAt = Clock.System.now().toString()
        )

        db.deleteLinkToken(request.token)

        call.respond(
            status = HttpStatusCode.OK,
            message = MessageResponse("Account linked successfully")
        )
    }
}
