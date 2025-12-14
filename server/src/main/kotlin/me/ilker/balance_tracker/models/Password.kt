package me.ilker.balance_tracker.models

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class Password(val value: String)