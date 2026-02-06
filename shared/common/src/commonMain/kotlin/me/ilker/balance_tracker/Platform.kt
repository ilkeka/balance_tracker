package me.ilker.balance_tracker

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform