package me.ilker.balance_tracker.config

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.minutes

internal fun Application.configRateLimit() {
    install(RateLimit) {
        register(RateLimitName("registration")) {
            rateLimiter(
                limit = 5,
                refillPeriod = 15.minutes,
                initialSize = 5
            )
        }
    }
}
