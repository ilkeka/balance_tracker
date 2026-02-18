package me.ilker.balance_tracker

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.app_name
import me.ilker.balance_tracker.resources.ic_app
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    startKoin {
        modules(appModule)
    }
    val windowState = rememberWindowState(width = 1000.dp, height = 700.dp)
    val icon = rememberVectorPainter(image = vectorResource(Res.drawable.ic_app))

    SwingWindow(
        state = windowState,
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.app_name),
        icon = icon,
        init = {}
    ) {
        CommonApp()
    }
}
