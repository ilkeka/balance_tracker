package me.ilker.balance_tracker

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.koin.core.context.startKoin

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class
)
fun main() {
    startKoin {
        modules(appModule)
    }

    ComposeViewport { CommonApp() }
}
