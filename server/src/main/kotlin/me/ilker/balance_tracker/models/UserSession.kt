package me.ilker.balance_tracker.models

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val name: String,
    val count: Int
)
