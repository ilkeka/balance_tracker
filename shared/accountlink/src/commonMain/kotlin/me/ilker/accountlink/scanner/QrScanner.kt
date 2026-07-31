package me.ilker.accountlink.scanner

import androidx.compose.runtime.Composable

@Composable
internal expect fun QrScanner(onScanned: (String) -> Unit)
