package me.ilker.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.camera_permission_denied
import me.ilker.balance_tracker.resources.camera_permission_grant
import me.ilker.balance_tracker.resources.sync_cancel
import me.ilker.balance_tracker.resources.sync_permission_close
import me.ilker.balance_tracker.resources.sync_permission_continue
import me.ilker.balance_tracker.resources.sync_permission_denied_message
import me.ilker.balance_tracker.resources.sync_permission_message
import me.ilker.balance_tracker.resources.sync_permission_title
import me.ilker.balance_tracker.resources.sync_scan_close
import me.ilker.sync.platform.BindSyncPlatformContext
import me.ilker.sync.platform.RequestNearbyDevicePermissions
import me.ilker.sync.qr.QrScanner
import me.ilker.sync.qr.rememberCameraPermission
import me.ilker.sync.views.IncomingRequestDialog
import me.ilker.sync.views.PairingCodeDialog
import me.ilker.sync.views.SyncLoadedView
import org.jetbrains.compose.resources.stringResource

/**
 * What a scanned code is meant for.
 *
 * The same reader serves both one-time codes, and which one it is cannot be told from the payload
 * without trying to decode it as both, so the action that opened the scanner is remembered instead.
 */
private enum class ScannerTarget { Pairing, Document }

/**
 * The action the nearby-device permissions are being asked for.
 *
 * Pairing advertises as well as sync does, so both go through the same prompt instead of each one
 * asking in its own way.
 */
private enum class NearbyPermissionTarget { Pair, Discover }

/**
 * Steps of the permission prompt.
 *
 * The explanation is a step of its own because the system dialog is drawn over the app: anything
 * shown underneath it is covered the instant it appears, which is why it has to be read and
 * acknowledged before the platform prompt is launched.
 */
private enum class NearbyPermissionStep { Explain, Denied, Ask }

/**
 * The camera preview, covering the whole screen.
 *
 * Drawn over the screen rather than next to it: the preview has to fill the display, and a full-size
 * surface in the content flow would otherwise be laid over the card the user came from. The close
 * button matters most when the camera was refused, since the preview never shows and the screen
 * would have no way back.
 */
@Composable
private fun ScannerOverlay(
    onScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            QrScanner(onScanned = onScanned)

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(Res.string.sync_scan_close)
                    )
                }
            }
        }
    }
}

/**
 * Rendering only: the screen translates [SyncState] into composables and hands user actions back as
 * [SyncIntent]s through the callbacks the navigation layer supplies.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var permissionStep by remember { mutableStateOf<NearbyPermissionStep?>(null) }
    var permissionTarget by remember { mutableStateOf(NearbyPermissionTarget.Discover) }
    var scannerTarget by remember { mutableStateOf(ScannerTarget.Pairing) }
    var awaitingCameraPermission by remember { mutableStateOf(false) }
    val cameraPermission = rememberCameraPermission()

    fun askNearbyPermissionsFor(target: NearbyPermissionTarget) {
        permissionTarget = target
        permissionStep = NearbyPermissionStep.Explain
    }

    fun openScanner(target: ScannerTarget) {
        scannerTarget = target
        if (cameraPermission.granted) onToggleScanner(true) else awaitingCameraPermission = true
    }

    // The system dialog goes away before the answer arrives, so the preview opens once the permission
    // is actually held rather than behind a sheet the user has already dealt with.
    LaunchedEffect(cameraPermission.granted) {
        if (cameraPermission.granted && awaitingCameraPermission) {
            awaitingCameraPermission = false
            onToggleScanner(true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SyncLoadedView(
            state = currentState,
            onSync = onSync,
            onDiscover = { askNearbyPermissionsFor(NearbyPermissionTarget.Discover) },
            onStopDiscovering = onStopDiscovering,
            onSelectDevice = onSelectDevice,
            onPair = { askNearbyPermissionsFor(NearbyPermissionTarget.Pair) },
            onPairWith = onPairWith,
            onExport = onExport,
            onImport = onImport,
            onUnpair = onUnpair,
            onPairScanRequested = { openScanner(ScannerTarget.Pairing) },
            onImportScanRequested = { openScanner(ScannerTarget.Document) },
            onClose = onClose
        )

        when (permissionStep) {
            NearbyPermissionStep.Explain,
            NearbyPermissionStep.Denied -> ModalBottomSheet(
                onDismissRequest = { permissionStep = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.sync_permission_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(
                            if (permissionStep == NearbyPermissionStep.Denied) {
                                Res.string.sync_permission_denied_message
                            } else {
                                Res.string.sync_permission_message
                            }
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))

                    if (permissionStep == NearbyPermissionStep.Denied) {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { permissionStep = null }
                        ) {
                            Text(stringResource(Res.string.sync_permission_close))
                        }
                    } else {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            onClick = { permissionStep = NearbyPermissionStep.Ask },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(Res.string.sync_permission_continue))
                        }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { permissionStep = null }
                        ) {
                            Text(stringResource(Res.string.sync_cancel))
                        }
                    }
                }
            }

            // Composed only once the explanation has been acknowledged: requesting on entry would put the
            // system dialog on top of the text the user is meant to read.
            NearbyPermissionStep.Ask -> RequestNearbyDevicePermissions { granted ->
                val target = permissionTarget
                permissionStep = if (granted) null else NearbyPermissionStep.Denied
                if (granted) {
                    when (target) {
                        NearbyPermissionTarget.Pair -> onPair()
                        NearbyPermissionTarget.Discover -> onDiscover()
                    }
                }
            }

            null -> Unit
        }

        if (awaitingCameraPermission && !cameraPermission.granted) {
            ModalBottomSheet(
                onDismissRequest = { awaitingCameraPermission = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VideocamOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(Res.string.camera_permission_denied),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        onClick = { cameraPermission.request() },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(Res.string.camera_permission_grant))
                    }
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { awaitingCameraPermission = false }
                    ) {
                        Text(stringResource(Res.string.sync_cancel))
                    }
                }
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
            ScannerOverlay(
                onScanned = { scanned ->
                    onToggleScanner(false)
                    when (scannerTarget) {
                        ScannerTarget.Pairing -> onPairWith(scanned)
                        ScannerTarget.Document -> onImport(scanned)
                    }
                },
                onDismiss = { onToggleScanner(false) }
            )
        }
    }
}
