package me.ilker.balance_tracker.models

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class Password(val value: String) {
    init {
        require(value.length >= 8) { "Password must be at least 8 characters" }
    }
}