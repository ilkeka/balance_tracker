package me.ilker.balance_tracker.sdk

import kotlin.time.Instant

data class AuthenticatedUser(
    val email: String,
    val token: String,
    val expiresAt: Instant
)
