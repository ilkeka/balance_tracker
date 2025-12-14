package me.ilker.balance_tracker.models

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationRequest(
    val email: Email,
    val password: Password
)
