package me.ilker.sync.qr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS asks for the camera through the system prompt the first time the capture session starts, so
 * there is nothing to gate here.
 */
@Composable
internal actual fun rememberCameraPermission(): CameraPermission = remember {
    CameraPermission(granted = true, request = {})
}
