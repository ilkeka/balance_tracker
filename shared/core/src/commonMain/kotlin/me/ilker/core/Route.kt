package me.ilker.core

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    object Home : Route

    @Serializable
    object Add : Route

    @Serializable
    object Transactions : Route
}