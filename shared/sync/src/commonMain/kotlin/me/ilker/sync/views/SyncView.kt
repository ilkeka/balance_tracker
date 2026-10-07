package me.ilker.sync.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.back
import me.ilker.balance_tracker.resources.sync_bluetooth_unavailable
import me.ilker.balance_tracker.resources.sync_cancel
import me.ilker.balance_tracker.resources.sync_connecting
import me.ilker.balance_tracker.resources.sync_declined
import me.ilker.balance_tracker.resources.sync_device_type_desktop
import me.ilker.balance_tracker.resources.sync_device_type_mobile
import me.ilker.balance_tracker.resources.sync_device_type_web
import me.ilker.balance_tracker.resources.sync_document_hint
import me.ilker.balance_tracker.resources.sync_error_generic
import me.ilker.balance_tracker.resources.sync_error_unavailable
import me.ilker.balance_tracker.resources.sync_error_unauthenticated
import me.ilker.balance_tracker.resources.sync_export_document
import me.ilker.balance_tracker.resources.sync_failed
import me.ilker.balance_tracker.resources.sync_incoming_description
import me.ilker.balance_tracker.resources.sync_incoming_detail
import me.ilker.balance_tracker.resources.sync_incoming_title
import me.ilker.balance_tracker.resources.sync_incoming_verify
import me.ilker.balance_tracker.resources.sync_incompatible
import me.ilker.balance_tracker.resources.sync_merging
import me.ilker.balance_tracker.resources.sync_nearby_empty
import me.ilker.balance_tracker.resources.sync_nearby_scan
import me.ilker.balance_tracker.resources.sync_nearby_selected
import me.ilker.balance_tracker.resources.sync_nearby_title
import me.ilker.balance_tracker.resources.sync_no_changes
import me.ilker.balance_tracker.resources.sync_pair_copy_code
import me.ilker.balance_tracker.resources.sync_pair_device
import me.ilker.balance_tracker.resources.sync_pair_qr_description
import me.ilker.balance_tracker.resources.sync_pair_qr_title
import me.ilker.balance_tracker.resources.sync_pair_scan_hint
import me.ilker.balance_tracker.resources.sync_paired_with
import me.ilker.balance_tracker.resources.sync_scanning
import me.ilker.balance_tracker.resources.sync_start
import me.ilker.balance_tracker.resources.sync_success
import me.ilker.balance_tracker.resources.sync_success_detail
import me.ilker.balance_tracker.resources.sync_title
import me.ilker.balance_tracker.resources.sync_transferring
import me.ilker.balance_tracker.resources.sync_unpair
import me.ilker.balance_tracker.resources.sync_unpaired
import me.ilker.balance_tracker.resources.sync_unreadable_code
import me.ilker.sync.IncomingRequest
import me.ilker.sync.NearbyDevice
import me.ilker.sync.SyncFailure
import me.ilker.sync.SyncOutcome
import me.ilker.sync.SyncPhase
import me.ilker.sync.SyncState
import me.ilker.sync.protocol.DeviceType
import me.ilker.sync.qr.ManualPairingCodeEntry
import me.ilker.sync.qr.rememberCopyToClipboard
import me.ilker.sync.qr.rememberQrCodePainter
import org.jetbrains.compose.resources.stringResource

private val CardShape = RoundedCornerShape(24.dp)
private val CardPadding = 20.dp
private val ScreenPadding = 16.dp
private val CardSpacing = 12.dp
private val ActionHeight = 56.dp

private data class OutcomeVisuals(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val detail: String?,
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SyncLoadedView(
    state: SyncState,
    onSync: () -> Unit,
    onDiscover: () -> Unit,
    onStopDiscovering: () -> Unit,
    onSelectDevice: (String) -> Unit,
    onPair: () -> Unit,
    onPairWith: (String) -> Unit,
    onExport: () -> Unit,
    onImport: (String) -> Unit,
    onUnpair: () -> Unit,
    onScanRequested: () -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(top = 48.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(Res.string.back)
                    )
                }

                Spacer(Modifier.width(4.dp))

                Text(
                    text = stringResource(Res.string.sync_title),
                    fontSize = TextUnit(value = 24f, type = TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = ScreenPadding,
                end = ScreenPadding,
                top = 8.dp,
                bottom = CardPadding
            ),
            verticalArrangement = Arrangement.spacedBy(CardSpacing)
        ) {
            item(key = "device") {
                LinkedDeviceCard(state = state)
            }

            state.outcome?.let { outcome ->
                item(key = "outcome") {
                    OutcomeCard(outcome = outcome)
                }
            }

            when {
                state.isSyncing -> item(key = "busy") {
                    BusyIndicator(phase = state.phase)
                }

                !state.paired && !state.isPairingInProgress -> item(key = "pairing") {
                    PairingSection(
                        onPair = onPair,
                        onPairWith = onPairWith
                    )
                }

                state.liveSyncSupported -> {
                    item(key = "sync_now") {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ActionHeight),
                            enabled = state.nearbyDevices.isNotEmpty(),
                            onClick = onSync,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(stringResource(Res.string.sync_start))
                        }
                    }

                    item(key = "nearby") {
                        NearbySection(
                            state = state,
                            onDiscover = onDiscover,
                            onStopDiscovering = onStopDiscovering,
                            onSelectDevice = onSelectDevice
                        )
                    }
                }

                else -> item(key = "document") {
                    DocumentSection(
                        onExport = onExport,
                        onImport = onImport,
                        onScanRequested = onScanRequested
                    )
                }
            }

            if (state.paired) {
                item(key = "unpair") {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ActionHeight),
                        onClick = onUnpair,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LinkOff,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(stringResource(Res.string.sync_unpair))
                    }
                }
            }
        }
    }
}

/**
 * Shows the device this installation is paired with, or a hint that nothing is paired yet.
 */
@Composable
private fun LinkedDeviceCard(state: SyncState) {
    val linked = state.linkedDevice
    val isPaired = linked != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isPaired) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isPaired) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (isPaired) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isPaired) Res.string.sync_paired_with else Res.string.sync_unpaired
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = linked?.alias ?: state.deviceId.take(FingerprintLength),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * The nearby-device picker.
 *
 * Devices are listed rather than picked automatically: more than one phone can be in the room, and
 * silently choosing the first to answer would sync with the wrong ledger. The list stays open for the
 * whole scan window so a device that wakes up later is still offered.
 */
@Composable
private fun NearbySection(
    state: SyncState,
    onDiscover: () -> Unit,
    onStopDiscovering: () -> Unit,
    onSelectDevice: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(CardSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Devices,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(Res.string.sync_nearby_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.isDiscovering) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(
                            text = stringResource(Res.string.sync_scanning),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                TextButton(onClick = onDiscover) {
                    Text(stringResource(Res.string.sync_nearby_scan))
                }
            }

            if (state.nearbyDevices.isEmpty()) {
                Text(
                    text = stringResource(Res.string.sync_nearby_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            state.nearbyDevices.forEach { device ->
                NearbyDeviceRow(
                    device = device,
                    selected = state.selectedDeviceId == device.id,
                    onClick = { onSelectDevice(device.id) }
                )
            }

            if (state.isDiscovering) {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStopDiscovering
                ) {
                    Text(stringResource(Res.string.sync_cancel))
                }
            }
        }
    }
}

@Composable
private fun NearbyDeviceRow(
    device: NearbyDevice,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Devices,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = device.alias,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = device.type.label() + " · " + device.fingerprint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (selected) {
                Spacer(Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(Res.string.sync_nearby_selected),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * The approval prompt.
 *
 * Shown before anything crosses the link, and it prints the pairing fingerprint because "accept a sync
 * request" is meaningless without a way to tell it apart from a stranger running the same app. It is a
 * dialog rather than inline because the answer is the only thing that matters at this point, and the
 * session behind it is holding a connection open.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun IncomingRequestDialog(
    request: IncomingRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onReject,
        title = { Text(stringResource(Res.string.sync_incoming_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.sync_incoming_description, request.device.alias))
                Text(stringResource(Res.string.sync_incoming_detail, request.transactionCount))
                HorizontalDivider()
                Text(
                    text = stringResource(Res.string.sync_incoming_verify),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = request.verifyCode,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text(stringResource(Res.string.sync_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text(stringResource(Res.string.sync_cancel))
            }
        }
    )
}

@Composable
private fun BusyIndicator(phase: SyncPhase?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Text(
                text = when (phase) {
                    SyncPhase.Connecting -> stringResource(Res.string.sync_connecting)
                    SyncPhase.Transferring -> stringResource(Res.string.sync_transferring)
                    SyncPhase.Merging -> stringResource(Res.string.sync_merging)
                    null -> stringResource(Res.string.sync_connecting)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PairingSection(
    onPair: () -> Unit,
    onPairWith: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(CardSpacing)
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ActionHeight),
                onClick = onPair,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCode,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text(stringResource(Res.string.sync_pair_device))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ManualPairingCodeEntry(
                enabled = true,
                onScanned = onPairWith
            )
        }
    }
}

/**
 * The pairing code is only needed once, so the scanner is shown inline rather than in a separate
 * screen: it keeps "here is my code" and "enter theirs" on one surface, which is what pairing by
 * hand-off actually looks like.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun PairingCodeDialog(
    code: String,
    onDismiss: () -> Unit
) {
    val copyToClipboard = rememberCopyToClipboard()
    val painter = rememberQrCodePainter(code)

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(Res.string.sync_pair_qr_title), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(Res.string.sync_pair_qr_description),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            androidx.compose.foundation.Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                painter = painter,
                contentDescription = stringResource(Res.string.sync_pair_qr_title)
            )
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { copyToClipboard(code) }
            ) {
                Text(stringResource(Res.string.sync_pair_copy_code))
            }
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss
            ) {
                Text(stringResource(Res.string.sync_cancel))
            }
        }
    }
}

@Composable
private fun DocumentSection(
    onExport: () -> Unit,
    onImport: (String) -> Unit,
    onScanRequested: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(CardSpacing)
        ) {
            Text(
                text = stringResource(Res.string.sync_document_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ActionHeight),
                onClick = onExport,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FileUpload,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text(stringResource(Res.string.sync_export_document))
            }
            ManualPairingCodeEntry(
                enabled = true,
                onScanned = onImport
            )
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = onScanRequested,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCode,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text(stringResource(Res.string.sync_pair_scan_hint))
            }
        }
    }
}

@Composable
private fun OutcomeCard(outcome: SyncOutcome) {
    val visuals = when (outcome) {
        is SyncOutcome.Merged -> OutcomeVisuals(
            icon = Icons.Rounded.CheckCircle,
            title = stringResource(Res.string.sync_success),
            detail = stringResource(Res.string.sync_success_detail, outcome.received, outcome.applied),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )

        SyncOutcome.UpToDate -> OutcomeVisuals(
            icon = Icons.Rounded.CloudDone,
            title = stringResource(Res.string.sync_no_changes),
            detail = null,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )

        SyncOutcome.Declined -> OutcomeVisuals(
            icon = Icons.Rounded.Cancel,
            title = stringResource(Res.string.sync_declined),
            detail = null,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )

        is SyncOutcome.Failed -> OutcomeVisuals(
            icon = Icons.Rounded.Error,
            title = stringResource(Res.string.sync_failed) + ": " + outcome.failure.message(),
            detail = null,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = visuals.containerColor,
            contentColor = visuals.contentColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = visuals.icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = visuals.contentColor
            )

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = visuals.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = visuals.contentColor
                )

                visuals.detail?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = visuals.contentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun SyncFailure.message(): String = when (this) {
    SyncFailure.Unauthenticated -> stringResource(Res.string.sync_error_unauthenticated)
    SyncFailure.Incompatible -> stringResource(Res.string.sync_incompatible)
    SyncFailure.BluetoothUnusable -> stringResource(Res.string.sync_bluetooth_unavailable)
    SyncFailure.NoDevicesNearby -> stringResource(Res.string.sync_error_unavailable)
    SyncFailure.UnreadableCode -> stringResource(Res.string.sync_unreadable_code)
    SyncFailure.Transport,
    SyncFailure.Unknown -> stringResource(Res.string.sync_error_generic)
}

@Composable
private fun DeviceType.label(): String = when (this) {
    DeviceType.Mobile -> stringResource(Res.string.sync_device_type_mobile)
    DeviceType.Desktop -> stringResource(Res.string.sync_device_type_desktop)
    DeviceType.Web -> stringResource(Res.string.sync_device_type_web)
}

private const val FingerprintLength = 8