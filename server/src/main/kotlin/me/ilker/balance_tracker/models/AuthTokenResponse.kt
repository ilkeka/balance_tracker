package me.ilker.balance_tracker.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokenResponse(
    val token: String,
    val expiresAt: String,
    val message: String
)
