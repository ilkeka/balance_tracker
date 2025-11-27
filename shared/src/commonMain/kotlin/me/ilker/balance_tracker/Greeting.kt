package me.ilker.balance_tracker

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, from $platform."
    }
}