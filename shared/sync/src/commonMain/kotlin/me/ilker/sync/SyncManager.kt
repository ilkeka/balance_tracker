package me.ilker.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sync.LinkedDevice
import me.ilker.balance_tracker.sync.SyncImportResult
import me.ilker.balance_tracker.sync.SyncSnapshot
import me.ilker.core.Manager
import me.ilker.sync.protocol.DeviceDescriptor
import me.ilker.sync.protocol.DeviceType
import me.ilker.sync.protocol.IncomingSyncRequest
import me.ilker.sync.protocol.PairingPayload
import me.ilker.sync.protocol.SyncMessage
import me.ilker.sync.protocol.SyncPayloadCodec
import me.ilker.sync.protocol.SyncProtocolException
import me.ilker.sync.protocol.SyncSessionResult
import me.ilker.sync.protocol.deviceFingerprint
import me.ilker.sync.protocol.runSyncSession
import me.ilker.sync.transport.DiscoveredDevice
import me.ilker.sync.transport.SyncDiscoveryException
import me.ilker.sync.transport.SyncLink
import me.ilker.sync.transport.SyncTransport
import me.ilker.sync.transport.SyncTransportException
import me.ilker.sync.transport.SyncUnavailableReason
import me.ilker.sync.transport.createSyncLink
import me.ilker.sync.transport.isLiveSyncSupported

class SyncManager(
    private val sdk: BalanceTrackerSDK
) : Manager<SyncState, SyncIntent, SyncSideEffect>() {
    private val currentState = MutableStateFlow(SyncState())

    /** The running scan, so it can be stopped when the user leaves the screen. */
    private var scanJob: Job? = null

    /**
     * Set while a peer waits on screen, so the session can be released when the user answers.
     *
     * A deferred rather than a boolean: the answer is read exactly once, by the coroutine blocked on
     * it, and a flag would let a second tap answer a session that has already moved on.
     */
    private var pendingApproval: CompletableDeferred<Boolean>? = null

    private var link: SyncLink? = null

    override val state: StateFlow<SyncState> = currentState.asStateFlow()
    override val sideEffect = Channel<SyncSideEffect>(capacity = Channel.BUFFERED)

    override fun sendIntent(intent: SyncIntent) {
        when (intent) {
            SyncIntent.Load -> load()
            SyncIntent.Pair -> pair()
            is SyncIntent.PairWith -> pairWith(intent.code)
            SyncIntent.Discover -> discover()
            SyncIntent.StopDiscovering -> stopDiscovering()
            is SyncIntent.SelectDevice -> selectDevice(intent.deviceId)
            SyncIntent.SyncNow -> syncNow()
            SyncIntent.AcceptIncoming -> answer(true)
            SyncIntent.RejectIncoming -> answer(false)
            SyncIntent.Export -> export()
            is SyncIntent.Import -> import(intent.code)
            SyncIntent.Unpair -> unpair()
            SyncIntent.OpenScanner -> currentState.update { it.copy(isScannerVisible = true) }
            SyncIntent.CloseScanner -> currentState.update { it.copy(isScannerVisible = false) }
            SyncIntent.DismissPairingCode -> currentState.update { it.copy(pairingCode = null, isPairingInProgress = false) }
            SyncIntent.DismissOutcome -> currentState.update { it.copy(outcome = null) }
        }
    }

    private fun load() {
        scope.launch {
            currentState.update {
                it.copy(
                    isLoading = false,
                    deviceId = sdk.deviceId(),
                    alias = sdk.deviceAlias(),
                    paired = sdk.getPairingSecret() != null,
                    linkedDevice = sdk.getLinkedDevice(),
                    liveSyncSupported = isLiveSyncSupported()
                )
            }
        }
    }

    /**
     * The secret is persisted the moment it is minted rather than after a successful sync, so a
     * connection that drops mid-transfer cannot leave the two devices holding different secrets.
     */
    private fun pair() {
        scope.launch {
            val code = PairingPayload.create()
            sdk.savePairingSecret(PairingPayload.decode(code), linkedDevice = null)
            currentState.update {
                it.copy(paired = false, isPairingInProgress = true, pairingCode = code)
            }
        }
    }

    private fun pairWith(code: String) {
        scope.launch {
            val secret = runCatching { PairingPayload.decode(code) }
                .getOrElse {
                    currentState.update {
                        it.copy(outcome = SyncOutcome.Failed(SyncFailure.UnreadableCode))
                    }
                    return@launch
                }

            sdk.savePairingSecret(secret, linkedDevice = null)
            currentState.update {
                it.copy(
                    paired = true,
                    isPairingInProgress = false,
                    isScannerVisible = false,
                    pairingCode = null
                )
            }

            // Pairing exists to enable a sync, so continue straight into one rather than making the
            // user ask for it a second time.
            syncNow()
        }
    }

    /**
     * Starts advertising, serving and scanning together.
     *
     * All three run at once because a device has to be reachable while it waits: whoever taps first
     * connects out, and whoever was already on this screen receives.
     */
    private fun discover() {
        val state = currentState.value
        if (state.isDiscovering) return
        if (!state.liveSyncSupported) {
            currentState.update { it.copy(outcome = SyncOutcome.Failed(SyncFailure.BluetoothUnusable)) }
            return
        }
        if (!state.paired) {
            currentState.update { it.copy(outcome = SyncOutcome.Failed(SyncFailure.Unauthenticated)) }
            return
        }

        scope.launch {
            val created = runCatching { startLink() }.getOrElse {
                fail(it)
                return@launch
            }
            link = created
            currentState.update { it.copy(isDiscovering = true, outcome = null) }

            listenForIncomingSessions(created)
            scanWindow(created)
        }
    }

    private fun stopDiscovering() {
        link?.stop()
        link = null
        // Cancelled rather than left to run out its clock: the radio is already stopped above, and a
        // still-waiting picker would keep a stale list on screen.
        scanJob?.cancel()
        scanJob = null
        currentState.update {
            it.copy(
                isDiscovering = false,
                nearbyDevices = emptyList(),
                selectedDeviceId = null
            )
        }
    }

    /**
     * Runs a full scan window, filling the picker as devices appear.
     *
     * Discovery does not stop early once something is found: the user chooses from the list, so
     * returning on the first hit would hide the device they actually meant.
     */
    private suspend fun scanWindow(current: SyncLink) {
        // Scoped to the window and closed when it ends. A channel owned by the manager instead would
        // outlive the scan, leaving this loop waiting on a picker nobody is filling.
        val found = Channel<DiscoveredDevice>(Channel.UNLIMITED)
        scanJob = scope.launch {
            try {
                runCatching {
                    current.discover(timeoutMillis = ScanWindowMillis) { device -> found.send(device) }
                }.onFailure { fail(it) }
            } finally {
                found.close()
                scanJob = null
                currentState.update { it.copy(isDiscovering = false) }
            }
        }

        for (device in found) {
            // A device that re-advertises replaces its own entry rather than showing up twice.
            val nearby = device.toNearbyDevice()
            currentState.update {
                it.copy(
                    nearbyDevices = it.nearbyDevices
                        .filterNot { existing -> existing.id == nearby.id } + nearby
                )
            }
        }
    }

    /** Accepts incoming sessions for as long as the screen is up, one at a time. */
    private suspend fun listenForIncomingSessions(current: SyncLink) {
        scope.launch {
            while (true) {
                val transport = runCatching { current.accept() }.getOrNull() ?: return@launch
                serveSession(transport)
            }
        }
    }

    /**
     * Runs the receiving half of a session.
     *
     * The peer's request is put on screen and nothing crosses the link until the user answers, so a
     * refusal leaves both devices untouched.
     */
    private suspend fun serveSession(transport: SyncTransport) {
        val secret = sdk.getPairingSecret()
        if (secret == null) {
            fail(SyncProtocolException.Unauthenticated)
            return
        }

        val snapshot = sdk.exportSyncSnapshot()
        val answer = CompletableDeferred<Boolean>()
        pendingApproval = answer
        busy(SyncPhase.Connecting)

        val result = runCatching {
            runSyncSession(
                transport = transport,
                local = requireLocalDescriptor(secret),
                secret = secret,
                snapshot = snapshot,
                request = null,
                onRequest = { request ->
                    currentState.update { it.copy(incomingRequest = request.toIncomingRequest()) }
                    answer.await()
                }
            )
        }

        pendingApproval = null
        currentState.update { it.copy(incomingRequest = null, isIncomingRequestAnswered = true) }

        result
            .onSuccess { outcome -> finishSession(outcome) }
            .onFailure { fail(it) }
    }

    private fun answer(accepted: Boolean) {
        pendingApproval?.complete(accepted)
        currentState.update {
            it.copy(
                incomingRequest = null,
                isIncomingRequestAnswered = true,
                outcome = if (accepted) null else SyncOutcome.Declined
            )
        }
    }

    private fun selectDevice(deviceId: String) {
        currentState.update { it.copy(selectedDeviceId = deviceId) }
    }

    private fun syncNow() {
        val state = currentState.value
        if (state.isSyncing) return
        if (!state.paired) {
            currentState.update { it.copy(outcome = SyncOutcome.Failed(SyncFailure.Unauthenticated)) }
            return
        }

        val device = state.selectedDeviceId?.let { selected ->
            state.nearbyDevices.firstOrNull { it.id == selected }
        } ?: state.nearbyDevices.firstOrNull()
        if (device == null) {
            currentState.update { it.copy(outcome = SyncOutcome.Failed(SyncFailure.NoDevicesNearby)) }
            return
        }

        scope.launch {
            busy(SyncPhase.Connecting)

            val secret = sdk.getPairingSecret()
            if (secret == null) {
                fail(SyncProtocolException.Unauthenticated)
                return@launch
            }

            // Syncing without having tapped the picker still has to work, so the link is created on
            // demand rather than requiring a discovery round trip first.
            val current = link ?: runCatching { startLink() }.getOrNull()?.also { link = it }
            if (current == null) {
                currentState.update { it.copy(isSyncing = false, phase = null) }
                return@launch
            }

            val connected = runCatching { current.connectTo(device.id) }.getOrElse {
                fail(it)
                return@launch
            }

            val snapshot = sdk.exportSyncSnapshot()
            val descriptor = requireLocalDescriptor(secret)
            currentState.update { it.copy(phase = SyncPhase.Transferring) }

            runCatching {
                runSyncSession(
                    transport = connected,
                    local = descriptor,
                    secret = secret,
                    snapshot = snapshot,
                    request = SyncMessage.Request(
                        device = descriptor,
                        transactionCount = snapshot.transactions.size
                    ),
                    // Initiating, so a request arriving here would mean the roles were reversed; treating
                    // it as a refusal keeps a mislabelled peer from pushing data.
                    onRequest = { false }
                )
            }.onSuccess { outcome -> finishSession(outcome) }.onFailure { fail(it) }
        }
    }

    /** Merges what the peer sent, or reports that there was nothing to do. */
    private suspend fun finishSession(outcome: SyncSessionResult) {
        when (outcome) {
            SyncSessionResult.Declined -> currentState.update {
                it.copy(isSyncing = false, phase = null, outcome = SyncOutcome.Declined)
            }

            is SyncSessionResult.Completed -> {
                rememberLinkedDevice(outcome.peer)
                outcome.peerSnapshot?.let { merge(it) }
                    ?: currentState.update {
                        it.copy(isSyncing = false, phase = null, outcome = SyncOutcome.UpToDate)
                    }
            }
        }
    }

    /**
     * Records who this installation is paired with, once that peer has proved it holds the secret.
     *
     * Written only after an authenticated session, so an unauthorised device that merely advertises
     * nearby cannot name itself in the sync screen. The alias follows the peer's own advertisement, so
     * renaming a device on the other end updates the label here on the next successful sync.
     */
    private suspend fun rememberLinkedDevice(peer: DeviceDescriptor) {
        if (peer.deviceId == sdk.deviceId()) return

        sdk.savePairingSecret(
            secret = requireSecret(),
            linkedDevice = LinkedDevice(
                deviceId = peer.deviceId,
                alias = peer.alias.ifBlank { LinkedDevice.DefaultAlias },
                fingerprint = peer.fingerprint
            )
        )
        currentState.update { it.copy(linkedDevice = sdk.getLinkedDevice()) }
    }

    /**
     * Desktop and web have no duplex link, so the same sealed payload is handed over out of band.
     * Because the payload and its signature are identical to the Bluetooth path, a code copied from
     * the desktop is interchangeable with a snapshot received over BLE.
     */
    private fun export() {
        scope.launch {
            busy(SyncPhase.Transferring)
            val outgoing = runCatching { SyncPayloadCodec.seal(requireSecret(), sdk.exportSyncSnapshot()) }
                .getOrElse {
                    fail(it)
                    return@launch
                }
            currentState.update { it.copy(isSyncing = false, phase = null, outgoingCode = outgoing) }
            sideEffect.send(SyncSideEffect.CopyToClipboard(outgoing))
        }
    }

    private fun import(code: String) {
        scope.launch {
            val opened = runCatching { SyncPayloadCodec.open(requireSecret(), code.trim()) }
                .getOrElse {
                    fail(it)
                    return@launch
                }
            merge(opened)
        }
    }

    private suspend fun merge(snapshot: SyncSnapshot) {
        currentState.update { it.copy(phase = SyncPhase.Merging) }
        val outcome = when (val result = sdk.importSyncSnapshot(snapshot)) {
            is SyncImportResult.Merged -> SyncOutcome.Merged(
                received = snapshot.transactions.size,
                applied = result.stats.applied
            )

            SyncImportResult.SameDevice -> SyncOutcome.UpToDate
        }
        currentState.update { it.copy(isSyncing = false, phase = null, outcome = outcome) }
    }

    private fun unpair() {
        scope.launch {
            stopDiscovering()
            sdk.clearPairingSecret()
            currentState.update {
                it.copy(
                    paired = false,
                    linkedDevice = null,
                    isPairingInProgress = false,
                    pairingCode = null,
                    outgoingCode = null,
                    outcome = null
                )
            }
        }
    }

    /**
     * Who this device presents itself as, both in the advertisement and in the handshake.
     *
     * [descriptor] has to be rebuilt per session rather than cached, because the fingerprint is derived
     * from the current secret and would be stale after re-pairing.
     */
    private suspend fun requireLocalDescriptor(secret: ByteArray): DeviceDescriptor = DeviceDescriptor(
        deviceId = sdk.deviceId(),
        alias = sdk.deviceAlias().ifEmpty { LinkedDevice.DefaultAlias },
        model = null,
        type = DeviceType.Mobile,
        fingerprint = deviceFingerprint(secret)
    )

    private suspend fun startLink(): SyncLink = createSyncLink().also { created ->
        created.start(requireLocalDescriptor(requireSecret()))
    }

    private suspend fun requireSecret(): ByteArray =
        sdk.getPairingSecret() ?: throw SyncProtocolException.Unauthenticated

    private fun busy(phase: SyncPhase) {
        currentState.update { it.copy(isSyncing = true, phase = phase, outcome = null) }
    }

    private fun fail(throwable: Throwable) {
        currentState.update {
            it.copy(
                isSyncing = false,
                phase = null,
                outcome = SyncOutcome.Failed(throwable.toSyncFailure())
            )
        }
    }

    private fun Throwable.toSyncFailure(): SyncFailure = when (this) {
        is SyncProtocolException.Unauthenticated -> SyncFailure.Unauthenticated
        is SyncProtocolException.IncompatibleVersion -> SyncFailure.Incompatible
        is SyncProtocolException.MessageTooLarge,
        is SyncProtocolException.MalformedChunk,
        is SyncProtocolException.MissingChunk -> SyncFailure.Transport

        is SyncDiscoveryException.Unavailable -> when (reason) {
            SyncUnavailableReason.BluetoothUnusable -> SyncFailure.BluetoothUnusable
            SyncUnavailableReason.NoDevicesNearby -> SyncFailure.NoDevicesNearby
        }

        is SyncTransportException -> SyncFailure.Transport
        is IllegalArgumentException -> SyncFailure.UnreadableCode
        else -> SyncFailure.Unknown
    }
}

private fun DiscoveredDevice.toNearbyDevice() = NearbyDevice(
    id = id,
    alias = descriptor.alias,
    model = descriptor.model,
    type = descriptor.type,
    fingerprint = descriptor.fingerprint
)

private fun IncomingSyncRequest.toIncomingRequest() = IncomingRequest(
    device = NearbyDevice(
        id = device.deviceId,
        alias = device.alias,
        model = device.model,
        type = device.type,
        fingerprint = device.fingerprint
    ),
    transactionCount = transactionCount
)

private const val ScanWindowMillis = 15_000L