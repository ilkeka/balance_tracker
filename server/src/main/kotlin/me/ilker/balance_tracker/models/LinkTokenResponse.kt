package me.ilker.balance_tracker.models

import kotlinx.serialization.Serializable

@Serializable
data class LinkTokenResponse(
    val token: String
)
