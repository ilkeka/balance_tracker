package me.ilker.balance_tracker.config

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.resources.Resources
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import me.ilker.balance_tracker.route.login
import me.ilker.balance_tracker.route.registration
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
internal fun Application.configRouting() {
    install(Resources)

    routing {
        get("/") {
            call.respondText(
                contentType = ContentType.parse("text/html"),
                text = """
                    <h3>Balance Tracker</h3>
                    <ul>
                        <li>Powered by Ktor</li>
                    </ul>
                    """
                    .trimIndent(),
                status = HttpStatusCode.OK
            )
        }
        rateLimit(RateLimitName("registration")) {
            registration()
        }
        login()
    }
}
