package me.ilker.balance_tracker.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: Email,
    val password: Password
)
