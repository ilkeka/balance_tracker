package me.ilker.balance_tracker

import org.jetbrains.compose.resources.getString

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}