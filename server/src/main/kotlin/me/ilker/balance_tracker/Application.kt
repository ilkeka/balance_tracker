package me.ilker.balance_tracker

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.ilker.balance_tracker.config.configAuth
import me.ilker.balance_tracker.config.configHttp
import me.ilker.balance_tracker.config.configRouting
import me.ilker.balance_tracker.config.configSerialization
import me.ilker.balance_tracker.config.configStatusPages

fun main() {
    embeddedServer(
        factory = Netty,
        port = SERVER_PORT,
        host = "0.0.0.0",
        module = Application::module
    )
        .start(wait = true)
}

fun Application.module() {
    configHttp()
    configAuth()
    configSerialization()
    configStatusPages()
    configRouting()
}
