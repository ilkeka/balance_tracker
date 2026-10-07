package me.ilker.sync.qr

import androidx.compose.runtime.Composable
import platform.UIKit.UIPasteboard

@Composable
actual fun rememberCopyToClipboard(): (String) -> Unit {
    return { text ->
        UIPasteboard.generalPasteboard.string = text
    }
}
