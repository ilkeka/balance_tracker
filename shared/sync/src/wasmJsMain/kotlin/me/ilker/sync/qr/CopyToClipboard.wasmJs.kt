package me.ilker.sync.qr

import androidx.compose.runtime.Composable
import kotlin.js.ExperimentalWasmJsInterop

@Composable
actual fun rememberCopyToClipboard(): (String) -> Unit {
    return { text ->
        writeToClipboard(text)
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun writeToClipboard(text: String) {
    js("window.navigator.clipboard.writeText(text).catch(function () {})")
}
