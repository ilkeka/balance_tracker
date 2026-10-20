package me.ilker.sync.clipboard

import androidx.compose.runtime.Composable

@Composable
expect fun rememberCopyToClipboard(): (String) -> Unit
