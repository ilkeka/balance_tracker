package me.ilker.sync.qr

import androidx.compose.runtime.Composable

@Composable
internal expect fun QrScanner(onScanned: (String) -> Unit)
