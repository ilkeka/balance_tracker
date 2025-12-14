package me.ilker.balance_tracker.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveNullable
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.util.encodeBase64
import io.ktor.util.getDigestFunction
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.ilker.balance_tracker.Registration
import me.ilker.balance_tracker.database.DB
import me.ilker.balance_tracker.models.RegistrationRequest
import org.koin.ktor.ext.inject
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@ExperimentalUuidApi
internal fun Route.registration() {
    val db by inject<DB>()
    val digest = getDigestFunction("SHA-256") { "ktor${it.length}" }

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

        db
            .getUserByMail(request.email.value)
            ?.let {
                call.respond(
                    status = HttpStatusCode.Forbidden,
                    message = "You cannot register this email!"
                )

                return@post
            }

        val id = Uuid.generateV4().toString()

        db
            .createUser(
                id = id,
                email = request.email.value,
                password = digest(request.password.value).encodeBase64(),
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
            )
            .await()

        call.respond(
            status = HttpStatusCode.Created,
            message = "User id is $id"
        )
    }
}
