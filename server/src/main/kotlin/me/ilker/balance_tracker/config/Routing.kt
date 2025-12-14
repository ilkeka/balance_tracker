package me.ilker.balance_tracker.config

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import me.ilker.balance_tracker.Greeting
import me.ilker.balance_tracker.route.registration
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
internal fun Application.configRouting() {
    install(Resources)

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
        registration()
    }
}
