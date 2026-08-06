package me.ilker.profile.scanner

import androidx.compose.runtime.Composable

@Composable
internal expect fun QrScanner(onScanned: (String) -> Unit)
