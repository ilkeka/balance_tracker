package me.ilker.sync.platform

import androidx.compose.runtime.Composable

@Composable
internal actual fun BindSyncPlatformContext() = Unit

/** The browser has no nearby-device link in this build, so there is nothing to ask for. */
@Composable
internal actual fun RequestNearbyDevicePermissions(onResult: (granted: Boolean) -> Unit) {
    onResult(true)
}
