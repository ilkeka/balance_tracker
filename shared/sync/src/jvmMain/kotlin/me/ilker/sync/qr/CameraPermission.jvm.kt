package me.ilker.sync.qr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Desktop has no camera, and scanning falls back to typing the code. */
@Composable
internal actual fun rememberCameraPermission(): CameraPermission = remember {
    CameraPermission(granted = true, request = {})
}
