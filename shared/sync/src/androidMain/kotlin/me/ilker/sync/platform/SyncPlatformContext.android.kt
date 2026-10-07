package me.ilker.sync.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import me.ilker.sync.transport.AndroidContextHolder

@Composable
internal actual fun BindSyncPlatformContext() {
    val context = LocalContext.current

    // Held only as long as the sync screen is on screen: a leaked Activity context would outlive the
    // GATT connection it was opened with.
    DisposableEffect(context) {
        AndroidContextHolder.context = context.applicationContext
        onDispose { AndroidContextHolder.context = null }
    }
}

/**
 * Requests the runtime Bluetooth permissions, skipping the prompt entirely when they are already held.
 *
 * Advertise is separate from scan and connect and was added in the same API level, but the three are
 * asked for together: the flow needs all of them, and asking separately would mean three prompts in a
 * row for one action.
 */
@Composable
internal actual fun RequestNearbyDevicePermissions(onResult: (granted: Boolean) -> Unit) {
    val context = LocalContext.current
    val currentOnResult = rememberUpdatedState(onResult)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted -> currentOnResult.value(granted.values.all { it }) }

    LaunchedEffect(context) {
        val missing = context.missingNearbyDevicePermissions()
        if (missing.isEmpty()) {
            currentOnResult.value(true)
        } else {
            launcher.launch(missing.toTypedArray())
        }
    }
}

private fun Context.missingNearbyDevicePermissions(): List<String> = buildList {
    // Below API 31 the Bluetooth permissions are install-time, so there is nothing left to request.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@buildList

    add(Manifest.permission.BLUETOOTH_CONNECT)
    add(Manifest.permission.BLUETOOTH_SCAN)
    add(Manifest.permission.BLUETOOTH_ADVERTISE)
}.filterNot(::hasPermission)

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED