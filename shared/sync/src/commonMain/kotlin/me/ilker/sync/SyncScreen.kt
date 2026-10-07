package me.ilker.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.ilker.sync.platform.BindSyncPlatformContext
import me.ilker.sync.platform.RequestNearbyDevicePermissions
import me.ilker.sync.qr.QrScanner
import me.ilker.sync.views.IncomingRequestDialog
import me.ilker.sync.views.PairingCodeDialog
import me.ilker.sync.views.SyncLoadedView

/**
 * Rendering only: the screen translates [SyncState] into composables and hands user actions back as
 * [SyncIntent]s through the callbacks the navigation layer supplies.
 */
@Composable
fun SyncScreen(
    state: State<SyncState>,
    onSync: () -> Unit,
    onDiscover: () -> Unit,
    onStopDiscovering: () -> Unit,
    onSelectDevice: (String) -> Unit,
    onAcceptIncoming: () -> Unit,
    onRejectIncoming: () -> Unit,
    onPair: () -> Unit,
    onPairWith: (String) -> Unit,
    onExport: () -> Unit,
    onImport: (String) -> Unit,
    onUnpair: () -> Unit,
    onDismissPairingCode: () -> Unit,
    onToggleScanner: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    BindSyncPlatformContext()
    val currentState = state.value

    // Set only while the platform is being asked, so the prompt appears next to the tap that caused it
    // rather than on every recomposition of a screen the user opened for something else.
    var isRequestingNearbyPermission by remember { mutableStateOf(false) }

    SyncLoadedView(
        state = currentState,
        onSync = onSync,
        onDiscover = { isRequestingNearbyPermission = true },
        onStopDiscovering = onStopDiscovering,
        onSelectDevice = onSelectDevice,
        onPair = onPair,
        onPairWith = onPairWith,
        onExport = onExport,
        onImport = onImport,
        onUnpair = onUnpair,
        onScanRequested = { onToggleScanner(true) },
        onClose = onClose
    )

    if (isRequestingNearbyPermission) {
        RequestNearbyDevicePermissions { granted ->
            isRequestingNearbyPermission = false
            if (granted) onDiscover()
        }
    }

    currentState.pairingCode?.let { code ->
        PairingCodeDialog(code = code, onDismiss = onDismissPairingCode)
    }

    currentState.incomingRequest?.let { request ->
        IncomingRequestDialog(
            request = request,
            onAccept = onAcceptIncoming,
            onReject = onRejectIncoming
        )
    }

    if (currentState.isScannerVisible) {
        QrScanner(onScanned = { scanned ->
            onToggleScanner(false)
            onPairWith(scanned)
        })
    }
}
