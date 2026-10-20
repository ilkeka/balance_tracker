package me.ilker.sync.qr

import androidx.compose.runtime.Composable

/**
 * Whether the camera can be opened right now, and how to ask for it.
 *
 * Kept out of the scanner itself so the prompt can be a sheet over the screen: asking from inside the
 * preview would draw it over a blank full-size surface, since there is no preview to put behind it.
 */
internal class CameraPermission(
    val granted: Boolean,
    val request: () -> Unit
)

@Composable
internal expect fun rememberCameraPermission(): CameraPermission
