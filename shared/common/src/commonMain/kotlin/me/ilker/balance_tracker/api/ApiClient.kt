package me.ilker.balance_tracker.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies

internal val apiClient: HttpClient by lazy {
    HttpClient {
        install(HttpCookies)
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 30_000
        }
    }
}
