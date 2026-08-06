package me.ilker.balance_tracker.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import me.ilker.balance_tracker.auth.AuthRepository

internal fun apiClient(authRepository: AuthRepository): HttpClient =
    HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 30_000
        }
        install(Auth) {
            bearer {
                loadTokens {
                    authRepository.authenticatedUser.value?.let { user ->
                        BearerTokens(accessToken = user.token, refreshToken = "")
                    }
                }
            }
        }
    }
