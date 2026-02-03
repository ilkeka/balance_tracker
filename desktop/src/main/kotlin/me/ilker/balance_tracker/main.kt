package me.ilker.balance_tracker

import androidx.compose.ui.window.singleWindowApplication
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModule)
    }

    singleWindowApplication(
        title = "Balance Tracker"
    ) {
        CommonApp()
    }
}
