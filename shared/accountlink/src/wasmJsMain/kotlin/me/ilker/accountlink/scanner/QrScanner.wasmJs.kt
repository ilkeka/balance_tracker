package me.ilker.accountlink.scanner

import androidx.compose.runtime.Composable

@Composable
internal actual fun QrScanner(onScanned: (String) -> Unit) {
    ManualTokenEntry(
        enabled = true,
        onScanned = onScanned
    )
}
