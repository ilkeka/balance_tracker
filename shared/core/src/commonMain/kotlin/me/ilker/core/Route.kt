package me.ilker.core

import kotlinx.serialization.Serializable

interface Route {
    @Serializable
    object Root : Route

    @Serializable
    object Add : Route
}