package me.ilker.balance_tracker

import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import me.ilker.balance_tracker.config.configAuth
import me.ilker.balance_tracker.config.configHttp
import me.ilker.balance_tracker.config.configRateLimit
import me.ilker.balance_tracker.config.configRouting
import me.ilker.balance_tracker.config.configSerialization
import me.ilker.balance_tracker.config.configStatusPages
import org.koin.core.context.startKoin
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
fun main(args: Array<String>) {
    startKoin {
        modules(serverModule)
    }

    EngineMain.main(args)
}

@ExperimentalUuidApi
@Suppress("Unused")
fun Application.module() {
    configHttp()
    configAuth()
    configRateLimit()
    configSerialization()
    configStatusPages()
    configRouting()
}
