package me.ilker.sync.platform

import androidx.compose.runtime.Composable

/**
 * Gives platform code the ambient it needs while the sync screen is composed.
 *
 * The Bluetooth transport needs a `Context` to open a GATT connection, but the common sync layer must
 * not reference one, and the transport interface is deliberately context-free. Targets that need
 * nothing implement this as a no-op.
 */
@Composable
internal expect fun BindSyncPlatformContext()

/**
 * Asks for whatever the platform needs before it can talk to a nearby device, then reports whether it
 * got it.
 *
 * Requested at the moment the user asks to look for devices rather than at startup, so the prompt
 * arrives next to the action it enables instead of in front of a screen the user did not come here
 * for. Targets that need nothing report success immediately.
 */
@Composable
internal expect fun RequestNearbyDevicePermissions(onResult: (granted: Boolean) -> Unit)