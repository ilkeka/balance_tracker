package me.ilker.profile.scanner

import androidx.compose.runtime.Composable

@Composable
internal actual fun QrScanner(onScanned: (String) -> Unit) {
    ManualTokenEntry(
        enabled = true,
        onScanned = onScanned
    )
}
