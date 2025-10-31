package me.ilker.balance_tracker

import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.app_name

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        val res = Res.string.app_name
        return "Hello, ${platform.name}!"
    }
}