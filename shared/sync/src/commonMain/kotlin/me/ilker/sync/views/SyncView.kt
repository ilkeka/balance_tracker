package me.ilker.sync.views

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.back
import me.ilker.balance_tracker.resources.sync_bluetooth_unavailable
import me.ilker.balance_tracker.resources.sync_cancel
import me.ilker.balance_tracker.resources.sync_connecting
import me.ilker.balance_tracker.resources.sync_declined
import me.ilker.balance_tracker.resources.sync_device_type_desktop
import me.ilker.balance_tracker.resources.sync_device_type_mobile
import me.ilker.balance_tracker.resources.sync_device_type_web
import me.ilker.balance_tracker.resources.sync_document_created
import me.ilker.balance_tracker.resources.sync_document_hint
import me.ilker.balance_tracker.resources.sync_error_generic
import me.ilker.balance_tracker.resources.sync_error_unavailable
import me.ilker.balance_tracker.resources.sync_error_unauthenticated
import me.ilker.balance_tracker.resources.sync_export_document
import me.ilker.balance_tracker.resources.sync_failed
import me.ilker.balance_tracker.resources.sync_import_code_placeholder
import me.ilker.balance_tracker.resources.sync_import_document
import me.ilker.balance_tracker.resources.sync_incoming_description
import me.ilker.balance_tracker.resources.sync_incoming_detail
import me.ilker.balance_tracker.resources.sync_incoming_title
import me.ilker.balance_tracker.resources.sync_incoming_verify
import me.ilker.balance_tracker.resources.sync_incompatible
import me.ilker.balance_tracker.resources.sync_merging
import me.ilker.balance_tracker.resources.sync_nearby_empty
import me.ilker.balance_tracker.resources.sync_nearby_scan
import me.ilker.balance_tracker.resources.sync_nearby_title
import me.ilker.balance_tracker.resources.sync_no_changes
import me.ilker.balance_tracker.resources.sync_pair_button
import me.ilker.balance_tracker.resources.sync_pair_code_hint
import me.ilker.balance_tracker.resources.sync_pair_code_placeholder
import me.ilker.balance_tracker.resources.sync_pair_code_title
import me.ilker.balance_tracker.resources.sync_pair_copy_code
import me.ilker.balance_tracker.resources.sync_pair_device
import me.ilker.balance_tracker.resources.sync_pair_hint
import me.ilker.balance_tracker.resources.sync_pair_prompt_message
import me.ilker.balance_tracker.resources.sync_pair_prompt_title
import me.ilker.balance_tracker.resources.sync_paired_with
import me.ilker.balance_tracker.resources.sync_scanning
import me.ilker.balance_tracker.resources.sync_start
import me.ilker.balance_tracker.resources.sync_step_find
import me.ilker.balance_tracker.resources.sync_step_open
import me.ilker.balance_tracker.resources.sync_success
import me.ilker.balance_tracker.resources.sync_success_detail
import me.ilker.balance_tracker.resources.sync_title
import me.ilker.balance_tracker.resources.sync_transferring
import me.ilker.balance_tracker.resources.sync_unpair
import me.ilker.balance_tracker.resources.sync_unpaired
import me.ilker.balance_tracker.resources.sync_unreadable_code
import me.ilker.balance_tracker.resources.sync_with_device
import me.ilker.sync.IncomingRequest
import me.ilker.sync.NearbyDevice
import me.ilker.sync.PairingPrompt
import me.ilker.sync.SyncFailure
import me.ilker.sync.SyncOutcome
import me.ilker.sync.SyncPhase
import me.ilker.sync.SyncState
import me.ilker.sync.clipboard.rememberCopyToClipboard
import me.ilker.sync.protocol.DeviceType
import org.jetbrains.compose.resources.stringResource

private val HeroShape = RoundedCornerShape(28.dp)
private val CardShape = RoundedCornerShape(24.dp)
private val RowShape = RoundedCornerShape(18.dp)
private val ActionShape = RoundedCornerShape(16.dp)
private val CardPadding = 20.dp
private val ScreenPadding = 16.dp
private val CardSpacing = 12.dp
private val ActionHeight = 56.dp
private val SmallActionHeight = 52.dp

private data class OutcomeVisuals(
    val icon: ImageVector,
    val title: String,
    val detail: String?,
    val containerColor: Color,
    val contentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SyncLoadedView(
    state: SyncState,
    onSync: () -> Unit,
    onDiscover: () -> Unit,
    onStopDiscovering: () -> Unit,
    onSelectDevice: (String) -> Unit,
    onPairDevice: (String) -> Unit,
    onPair: () -> Unit,
    onPairWith: (String) -> Unit,
    onExport: () -> Unit,
    onImport: (String) -> Unit,
    onUnpair: () -> Unit,
    onClose: () -> Unit
) {
    val busy = state.isSyncing || state.isPairing

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
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(CardSpacing)
        ) {
            item(key = "status") {
                StatusCard(state = state, onSync = onSync)
            }

            state.outcome?.let { outcome ->
                item(key = "outcome") {
                    OutcomeCard(outcome = outcome)
                }
            }

            when {
                state.isSyncing -> item(key = "busy") {
                    SyncProgressCard(phase = state.phase)
                }

                state.isPairing -> item(key = "busy") {
                    SyncProgressCard(phase = SyncPhase.Connecting)
                }

                !state.paired && !state.isPairingInProgress -> {
                    if (state.liveSyncSupported) {
                        item(key = "nearby") {
                            NearbySection(
                                state = state,
                                paired = false,
                                onDiscover = onDiscover,
                                onStopDiscovering = onStopDiscovering,
                                onSelectDevice = onSelectDevice,
                                onPairDevice = onPairDevice
                            )
                        }
                    } else {
                        item(key = "pairing") {
                            PairingSection(
                                onPair = onPair,
                                onPairWith = onPairWith
                            )
                        }
                    }
                }

                state.liveSyncSupported -> {
                    // A paired device syncs from the card above, which finds the stored peer
                    // itself. The picker is only needed when the secret was adopted out of band
                    // and there is no linked device to look for.
                    if (!state.isPairingInProgress && state.linkedDevice == null) {
                        item(key = "nearby") {
                            NearbySection(
                                state = state,
                                paired = true,
                                onDiscover = onDiscover,
                                onStopDiscovering = onStopDiscovering,
                                onSelectDevice = onSelectDevice,
                                onPairDevice = onPairDevice
                            )
                        }

                        val selected = state.nearbyDevices.firstOrNull {
                            it.id == state.selectedDeviceId
                        }

                        if (selected != null && !busy) {
                            item(key = "sync_now") {
                                SyncNowButton(
                                    deviceAlias = selected.alias,
                                    onClick = onSync
                                )
                            }
                        }
                    }
                }

                else -> item(key = "document") {
                    DocumentSection(
                        onExport = onExport,
                        onImport = onImport
                    )
                }
            }

            if (state.paired && !busy) {
                item(key = "unpair") {
                    UnpairButton(onUnpair = onUnpair)
                }
            }
        }
    }
}

/**
 * The one place that says whether this device is ready to sync.
 *
 * Paired devices get a tinted hero with the name and fingerprint of the peer, so the screen answers
 * "who am I connected to?" before the user looks for a button. Unpaired devices get the short version
 * of the Bluetooth flow instead of a bare action list.
 */
@Composable
private fun StatusCard(state: SyncState, onSync: () -> Unit) {
    val linked = state.linkedDevice
    val isPaired = state.paired
    val isCodeReady = !isPaired && state.isPairingInProgress

    val containerColor = if (isPaired) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (isPaired) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = HeroShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SyncIconBadge(
                    icon = when {
                        isPaired -> Icons.Rounded.Link
                        isCodeReady -> Icons.Rounded.ContentCopy
                        else -> Icons.Rounded.Bluetooth
                    },
                    size = 56.dp,
                    containerColor = if (isPaired) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (isPaired) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(
                            when {
                                isPaired -> Res.string.sync_paired_with
                                isCodeReady -> Res.string.sync_pair_device
                                else -> Res.string.sync_unpaired
                            }
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor.copy(alpha = 0.72f)
                    )

                    Text(
                        text = when {
                            isPaired -> linked?.alias ?: stringResource(Res.string.sync_paired_with)
                            isCodeReady -> stringResource(Res.string.sync_document_created)
                            else -> stringResource(Res.string.sync_pair_device)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            when {
                isPaired -> linked?.let { device ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetaPill(
                            icon = Icons.Rounded.Fingerprint,
                            text = device.fingerprint.uppercase(),
                            containerColor = contentColor.copy(alpha = 0.08f),
                            contentColor = contentColor
                        )
                    }
                }

                isCodeReady -> Text(
                    text = stringResource(Res.string.sync_pair_code_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )

                else -> {
                    Text(
                        text = stringResource(
                            if (state.liveSyncSupported) {
                                Res.string.sync_pair_hint
                            } else {
                                Res.string.sync_document_hint
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )

                    if (state.liveSyncSupported) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Step(number = 1, text = stringResource(Res.string.sync_step_open))
                            Step(number = 2, text = stringResource(Res.string.sync_step_find))
                        }
                    }
                }
            }

            // The stored peer is all a sync needs. Looking for it is the app's job, so the paired
            // card offers the action directly instead of asking the user to scan and pick again.
            if (isPaired && linked != null && !state.isSyncing && !state.isPairing) {
                Button(
                    onClick = onSync,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.sync_start))
                }
            }
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun SyncIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(size / 2)
            )
        }
    }
}

@Composable
private fun MetaPill(
    icon: ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

/**
 * The nearby-device picker.
 *
 * Unpaired rows carry their own Pair action so the flow is one tap instead of select-then-scroll.
 * Paired rows are radio options, because there the tap only chooses who to sync with and the action
 * stays in one place below the list.
 */
@Composable
private fun NearbySection(
    state: SyncState,
    paired: Boolean,
    onDiscover: () -> Unit,
    onStopDiscovering: () -> Unit,
    onSelectDevice: (String) -> Unit,
    onPairDevice: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.sync_nearby_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                if (state.nearbyDevices.isNotEmpty()) {
                    CountBadge(count = state.nearbyDevices.size)
                }
            }

            if (state.isDiscovering) {
                SearchingRow(onStopDiscovering = onStopDiscovering)
            } else if (state.nearbyDevices.isEmpty()) {
                FilledTonalButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SmallActionHeight),
                    onClick = onDiscover,
                    shape = ActionShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(stringResource(Res.string.sync_nearby_scan))
                }
            } else {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDiscover
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(stringResource(Res.string.sync_nearby_scan))
                }
            }

            if (state.nearbyDevices.isEmpty()) {
                EmptyNearby()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.nearbyDevices.forEach { device ->
                        NearbyDeviceRow(
                            device = device,
                            paired = paired,
                            selected = paired && state.selectedDeviceId == device.id,
                            pairingEnabled = !state.isPairing,
                            showFingerprint = paired,
                            onSelect = { onSelectDevice(device.id) },
                            onPair = { onPairDevice(device.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text = count.toString(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SearchingRow(onStopDiscovering: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RowShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )

            Text(
                text = stringResource(Res.string.sync_scanning),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            TextButton(onClick = onStopDiscovering) {
                Text(stringResource(Res.string.sync_cancel))
            }
        }
    }
}

@Composable
private fun EmptyNearby() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RowShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.BluetoothSearching,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(Res.string.sync_nearby_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NearbyDeviceRow(
    device: NearbyDevice,
    paired: Boolean,
    selected: Boolean,
    pairingEnabled: Boolean,
    showFingerprint: Boolean,
    onSelect: () -> Unit,
    onPair: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (paired) onSelect else ({}),
        enabled = paired,
        shape = RowShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SyncIconBadge(
                icon = device.type.icon(),
                size = 40.dp,
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = device.subtitle(showFingerprint = showFingerprint),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (paired) {
                Spacer(Modifier.width(8.dp))

                RadioButton(
                    selected = selected,
                    onClick = null
                )
            } else {
                Spacer(Modifier.width(12.dp))

                FilledTonalButton(
                    modifier = Modifier.height(40.dp),
                    onClick = onPair,
                    enabled = pairingEnabled,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 18.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.sync_pair_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncNowButton(deviceAlias: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(ActionHeight),
        onClick = onClick,
        shape = ActionShape
    ) {
        Icon(
            imageVector = Icons.Rounded.Sync,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = stringResource(Res.string.sync_with_device, deviceAlias),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun UnpairButton(onUnpair: () -> Unit) {
    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        onClick = onUnpair,
        shape = ActionShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Icon(
            imageVector = Icons.Rounded.LinkOff,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(stringResource(Res.string.sync_unpair))
    }
}

@Composable
private fun SyncProgressCard(phase: SyncPhase?) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )

                Text(
                    text = when (phase) {
                        SyncPhase.Connecting -> stringResource(Res.string.sync_connecting)
                        SyncPhase.Transferring -> stringResource(Res.string.sync_transferring)
                        SyncPhase.Merging -> stringResource(Res.string.sync_merging)
                        null -> stringResource(Res.string.sync_connecting)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * The Bluetooth pairing card shown while nothing is paired yet.
 *
 * Unpaired rows in the picker start the handshake themselves, so this card only has to explain what
 * the other device should be doing. The steps are short because the dialog that follows is the part
 * that actually needs attention.
 */
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SmallActionHeight),
                onClick = onPair,
                shape = ActionShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(stringResource(Res.string.sync_pair_device))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ManualCodeEntry(
                placeholder = stringResource(Res.string.sync_pair_code_placeholder),
                action = stringResource(Res.string.sync_pair_button),
                enabled = true,
                onSubmit = onPairWith
            )
        }
    }
}

/**
 * The approval prompt for a Bluetooth pairing.
 *
 * The code gets a block of its own because comparing six characters is the whole point of the dialog.
 * Rejecting is a plain cancel: a mismatch is not an error the app can act on.
 */
@Composable
internal fun PairingDialog(
    prompt: PairingPrompt,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(stringResource(Res.string.sync_pair_prompt_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.sync_pair_prompt_message, prompt.device.alias))

                VerificationCode(code = prompt.code)

                Text(
                    text = stringResource(Res.string.sync_incoming_verify),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text(stringResource(Res.string.sync_pair_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text(stringResource(Res.string.sync_cancel))
            }
        }
    )
}

/**
 * The approval prompt shown before a sync starts.
 *
 * Same code block as pairing: the fingerprint is what tells the user this is the device they paired
 * with and not someone else running the app in the same room.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun IncomingRequestDialog(
    request: IncomingRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Sync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(stringResource(Res.string.sync_incoming_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.sync_incoming_description, request.device.alias))
                Text(
                    text = stringResource(Res.string.sync_incoming_detail, request.transactionCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                VerificationCode(code = request.verifyCode)

                Text(
                    text = stringResource(Res.string.sync_incoming_verify),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun VerificationCode(code: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            text = code,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A one-time code for a device that cannot pair over Bluetooth.
 *
 * Shown as text rather than a QR image: the only device that can consume it is one without a radio,
 * and the user passes it along the way they would pass any other message.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun PairingCodeDialog(
    code: String,
    onDismiss: () -> Unit
) {
    val copyToClipboard = rememberCopyToClipboard()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SyncIconBadge(
                icon = Icons.Rounded.ContentCopy,
                size = 56.dp
            )

            Text(
                text = stringResource(Res.string.sync_pair_code_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(Res.string.sync_pair_code_hint),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                SelectionContainer {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        text = code,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SmallActionHeight),
                onClick = { copyToClipboard(code) },
                shape = ActionShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(8.dp))

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
    onImport: (String) -> Unit
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.sync_document_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SmallActionHeight),
                onClick = onExport,
                shape = ActionShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(stringResource(Res.string.sync_export_document))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ManualCodeEntry(
                placeholder = stringResource(Res.string.sync_import_code_placeholder),
                action = stringResource(Res.string.sync_import_document),
                enabled = true,
                onSubmit = onImport
            )
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
            title = stringResource(Res.string.sync_failed),
            detail = outcome.failure.message(),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = visuals.containerColor,
        contentColor = visuals.contentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = visuals.contentColor.copy(alpha = 0.12f),
                contentColor = visuals.contentColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = visuals.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = visuals.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
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

private fun DeviceType.icon(): ImageVector = when (this) {
    DeviceType.Mobile -> Icons.Rounded.Smartphone
    DeviceType.Desktop -> Icons.Rounded.Computer
    DeviceType.Web -> Icons.Rounded.Language
}

@Composable
private fun NearbyDevice.subtitle(showFingerprint: Boolean): String {
    val type = type.label()
    val model = model?.takeIf { it.isNotBlank() }

    return when {
        showFingerprint -> "$type · ${fingerprint.uppercase()}"
        model != null -> "$type · $model"
        else -> type
    }
}
