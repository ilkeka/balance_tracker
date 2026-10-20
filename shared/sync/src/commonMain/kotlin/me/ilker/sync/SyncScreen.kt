package me.ilker.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import me.ilker.balance_tracker.resources.sync_cancel
import me.ilker.balance_tracker.resources.sync_permission_close
import me.ilker.balance_tracker.resources.sync_permission_continue
import me.ilker.balance_tracker.resources.sync_permission_denied_message
import me.ilker.balance_tracker.resources.sync_permission_message
import me.ilker.balance_tracker.resources.sync_permission_title
import me.ilker.sync.platform.BindSyncPlatformContext
import me.ilker.sync.platform.RequestNearbyDevicePermissions
import me.ilker.sync.platform.hasNearbyDevicePermissions
import me.ilker.sync.views.IncomingRequestDialog
import me.ilker.sync.views.PairingCodeDialog
import me.ilker.sync.views.PairingDialog
import me.ilker.sync.views.SyncLoadedView
import org.jetbrains.compose.resources.stringResource

/**
 * Steps of the permission prompt.
 *
 * The explanation is a step of its own because the system dialog is drawn over the app: anything
 * shown underneath it is covered the instant it appears, which is why it has to be read and
 * acknowledged before the platform prompt is launched.
 */
private enum class NearbyPermissionStep { Explain, Denied, Ask }

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
    onPairDevice: (String) -> Unit,
    onAcceptPairing: () -> Unit,
    onRejectPairing: () -> Unit,
    onAcceptIncoming: () -> Unit,
    onRejectIncoming: () -> Unit,
    onPair: () -> Unit,
    onPairWith: (String) -> Unit,
    onExport: () -> Unit,
    onImport: (String) -> Unit,
    onUnpair: () -> Unit,
    onDismissPairingCode: () -> Unit,
    onClose: () -> Unit
) {
    BindSyncPlatformContext()
    val currentState = state.value

    // Set only while the platform is being asked, so the prompt appears next to the tap that caused it
    // rather than on every recomposition of a screen the user opened for something else.
    var permissionStep by remember { mutableStateOf<NearbyPermissionStep?>(null) }

    // The radio is stopped when the screen leaves: an advertisement left behind would keep the
    // device reachable (and the radio awake) with nothing on screen to pair with it.
    DisposableEffect(Unit) {
        onDispose { onStopDiscovering() }
    }

    // Already-held permissions never show the sheet: the explanation is only there to make the system
    // prompt make sense, and a user who granted it once should not have to dismiss it on every scan.
    val requestDiscovery: () -> Unit = {
        if (hasNearbyDevicePermissions()) {
            onDiscover()
        } else {
            permissionStep = NearbyPermissionStep.Explain
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SyncLoadedView(
            state = currentState,
            onSync = onSync,
            onDiscover = requestDiscovery,
            onStopDiscovering = onStopDiscovering,
            onSelectDevice = onSelectDevice,
            onPairDevice = onPairDevice,
            onPair = onPair,
            onPairWith = onPairWith,
            onExport = onExport,
            onImport = onImport,
            onUnpair = onUnpair,
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
                permissionStep = if (granted) null else NearbyPermissionStep.Denied
                if (granted) onDiscover()
            }

            null -> Unit
        }

        currentState.pairingPrompt?.let { prompt ->
            PairingDialog(
                prompt = prompt,
                onAccept = onAcceptPairing,
                onReject = onRejectPairing
            )
        }

        currentState.pairingCode?.let { code ->
            PairingCodeDialog(
                code = code,
                onDismiss = onDismissPairingCode
            )
        }

        currentState.incomingRequest?.let { request ->
            IncomingRequestDialog(
                request = request,
                onAccept = onAcceptIncoming,
                onReject = onRejectIncoming
            )
        }
    }
}
