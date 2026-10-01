package me.ilker.profile.clipboard

import androidx.compose.runtime.Composable

@Composable
internal expect fun rememberCopyToClipboard(): (String) -> Unit
