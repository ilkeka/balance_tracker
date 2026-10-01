package me.ilker.profile.clipboard

import androidx.compose.runtime.Composable
import platform.UIKit.UIPasteboard

@Composable
internal actual fun rememberCopyToClipboard(): (String) -> Unit {
    return { text ->
        UIPasteboard.generalPasteboard.string = text
    }
}
