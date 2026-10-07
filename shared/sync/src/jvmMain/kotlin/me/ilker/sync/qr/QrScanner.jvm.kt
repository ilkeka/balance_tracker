package me.ilker.sync.qr

import androidx.compose.runtime.Composable

@Composable
internal actual fun QrScanner(onScanned: (String) -> Unit) {
    ManualPairingCodeEntry(
        enabled = true,
        onScanned = onScanned
    )
}
