package me.ilker.sync.qr

import androidx.compose.runtime.Composable

@Composable
expect fun rememberCopyToClipboard(): (String) -> Unit
