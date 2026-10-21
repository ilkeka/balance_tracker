package me.ilker.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
import me.ilker.sync.platform.deviceDisplayName
import me.ilker.sync.platform.deviceModel
import me.ilker.sync.platform.hasNearbyDevicePermissions
import me.ilker.sync.protocol.DeviceDescriptor
import me.ilker.sync.protocol.DeviceType
import me.ilker.sync.protocol.FramedLink
import me.ilker.sync.protocol.IncomingSyncRequest
import me.ilker.sync.protocol.PairingMessage
import me.ilker.sync.protocol.PairingMessageCodec
import me.ilker.sync.protocol.PairingPayload
import me.ilker.sync.protocol.PairingResult
import me.ilker.sync.protocol.SyncMessage
import me.ilker.sync.protocol.SyncMessageCodec
import me.ilker.sync.protocol.SyncPayloadCodec
import me.ilker.sync.protocol.SyncProtocolException
import me.ilker.sync.protocol.SyncSessionResult
import me.ilker.sync.protocol.deviceFingerprint
import me.ilker.sync.protocol.randomNonce
import me.ilker.sync.protocol.readMessageText
import me.ilker.sync.protocol.runPairingSession
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
    private val currentState = MutableStateFlow(SyncState(liveSyncSupported = isLiveSyncSupported()))

    /** The running scan, so it can be stopped when the user leaves the screen. */
    private var scanJob: Job? = null

    /**
     * Set while a peer waits on screen, so the session can be released when the user answers.
     *
     * A deferred rather than a boolean: the answer is read exactly once, by the coroutine blocked on
     * it, and a flag would let a second tap answer a session that has already moved on.
     */
    private var pendingApproval: CompletableDeferred<Boolean>? = null

    /** The same mechanism for the numeric comparison shown while pairing over Bluetooth. */
    private var pendingPairingApproval: CompletableDeferred<Boolean>? = null

    private var link: SyncLink? = null

    override val state: StateFlow<SyncState> = currentState.asStateFlow()
    override val sideEffect = Channel<SyncSideEffect>(capacity = Channel.BUFFERED)

    override fun sendIntent(intent: SyncIntent) {
        when (intent) {
            SyncIntent.Load -> load()
            SyncIntent.Pair -> pair()
            is SyncIntent.PairWith -> pairWith(intent.code)
            is SyncIntent.PairDevice -> pairDevice(intent.deviceId)
            SyncIntent.AcceptPairing -> answerPairing(true)
            SyncIntent.RejectPairing -> answerPairing(false)
            SyncIntent.Discover -> discover()
            SyncIntent.StopDiscovering -> stopDiscovering()
            is SyncIntent.SelectDevice -> selectDevice(intent.deviceId)
            SyncIntent.SyncNow -> syncNow()
            SyncIntent.AcceptIncoming -> answer(true)
            SyncIntent.RejectIncoming -> answer(false)
            SyncIntent.Export -> export()
            is SyncIntent.Import -> import(intent.code)
            SyncIntent.Unpair -> unpair()
            SyncIntent.DismissPairingCode -> {
                // The code is only useful while it is on screen. The secret itself stays saved, so a
                // later sync can still use it; the sheet is just the way to hand it over.
                currentState.update { it.copy(pairingCode = null, isPairingInProgress = false) }
            }
            SyncIntent.DismissOutcome -> currentState.update { it.copy(outcome = null) }
        }
    }

    private fun load() {
        scope.launch {
            val paired = sdk.getPairingSecret() != null
            currentState.update {
                it.copy(
                    isLoading = false,
                    deviceId = sdk.deviceId(),
                    alias = sdk.deviceAlias().ifBlank { deviceDisplayName() },
                    paired = paired,
                    linkedDevice = sdk.getLinkedDevice(),
                    liveSyncSupported = isLiveSyncSupported()
                )
            }

            // Start advertising as soon as the screen opens, so the other phone can find this one
            // without the user doing anything here. Only when the permissions are already held: a
            // prompt before the user asked for anything would be a worse first impression than
            // waiting for the "look for nearby devices" tap.
            if (!paired && isLiveSyncSupported() && hasNearbyDevicePermissions()) {
                ensureLink()
            }
        }
    }

    /**
     * Mints a secret and shows it as text for a device that has no radio to pair over.
     *
     * Desktop and web builds use the same out-of-band secret as the document exchange, so this is the
     * only way they can join a pairing. Phones find each other over Bluetooth instead.
     */
    private fun pair() {
        scope.launch {
            val code = PairingPayload.create()
            sdk.savePairingSecret(PairingPayload.decode(code), linkedDevice = null)
            currentState.update {
                it.copy(
                    paired = false,
                    isPairingInProgress = true,
                    pairingCode = code
                )
            }
        }
    }

    /** Adopts a secret created on another device, pasted or typed from the code it showed. */
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
                    pairingCode = null,
                    outcome = null
                )
            }
        }
    }

    /**
     * Connects to the device the user picked and pairs over Bluetooth.
     *
     * The connection carries the whole handshake, including the new secret, so nothing has to travel
     * through a camera or a copied code. Both users see the same six-character code and accept on
     * their own device before the secret is created.
     */
    private fun pairDevice(deviceId: String) {
        val state = currentState.value
        if (state.isPairing || state.pairingPrompt != null || state.isSyncing || state.paired) return
        val device = state.nearbyDevices.firstOrNull { it.id == deviceId } ?: return

        scope.launch {
            // Scanning while connecting slows the handshake on some stacks, and the user has already
            // picked the peer, so the window has served its purpose.
            stopScanning()
            currentState.update { it.copy(isPairing = true, outcome = null) }
            if (!ensureLink()) {
                currentState.update { it.copy(isPairing = false) }
                return@launch
            }
            val current = link ?: return@launch

            try {
                val transport = current.connectTo(device.id)
                val result = runPairingSession(
                    transport = transport,
                    local = PairingMessage.Hello(
                        device = localDescriptor(sdk.getPairingSecret()),
                        nonce = randomNonce()
                    ),
                    peerHello = null,
                    onPeer = ::askPairingApproval,
                    onPaired = ::rememberPairing
                )

                currentState.update { it.copy(isPairing = false) }
                when (result) {
                    is PairingResult.Paired -> {
                        // The stack has just torn down the pairing connection. Letting it settle for a
                        // moment keeps the immediate first sync from racing the disconnect on stacks
                        // that report the next connect before the old one is fully gone.
                        delay(PairingToSyncDelayMillis)
                        // Pairing exists to enable a sync, and the peer acknowledged storing the secret
                        // before this returned, so the first sync can follow immediately.
                        selectDevice(device.id)
                        syncNow()
                    }

                    PairingResult.Declined -> currentState.update {
                        it.copy(outcome = SyncOutcome.Declined)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                currentState.update { it.copy(isPairing = false, pairingPrompt = null) }
                fail(throwable)
            }
        }
    }

    /**
     * Opens the link and starts advertising, or reuses the one already running.
     *
     * Advertising and serving are shared by every attempt, while a session is one connection, so the
     * link outlives an individual scan window and is only dropped when the screen is left.
     *
     * @return false when the link could not be opened, after reporting the failure as an outcome.
     */
    private suspend fun ensureLink(): Boolean {
        val wasOpen = link != null
        val current = try {
            startLink()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            fail(throwable)
            return false
        }
        if (!wasOpen) listenForIncomingSessions(current)
        return true
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

        scope.launch {
            if (!ensureLink()) return@launch
            val created = link ?: return@launch
            currentState.update { it.copy(isDiscovering = true, outcome = null) }

            scanWindow(created)
        }
    }

    private fun stopDiscovering() {
        link?.stop()
        link = null
        stopScanning()
        currentState.update {
            it.copy(
                nearbyDevices = emptyList(),
                selectedDeviceId = null
            )
        }
    }

    /**
     * Ends the scan window without touching the link.
     *
     * The link keeps advertising and serving, because a chosen peer still has to connect back on the
     * session that follows; only the picker stops growing.
     */
    private fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
        currentState.update { it.copy(isDiscovering = false) }
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
                current.discover(timeoutMillis = ScanWindowMillis) { device -> found.send(device) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                fail(throwable)
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
                try {
                    serveIncoming(transport)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    fail(throwable)
                }
            }
        }
    }

    /**
     * Decides what kind of session just arrived by reading its first message.
     *
     * The initiator always speaks first, which is what makes this possible: a pairing hello and a
     * sync hello have different shapes, so no extra envelope or second characteristic is needed.
     */
    private suspend fun serveIncoming(transport: SyncTransport) {
        val first = transport.readMessageText()
        val pairing = runCatching<PairingMessage?> { PairingMessageCodec.read(first) }.getOrElse { error ->
            if (error is SyncProtocolException.IncompatibleVersion) throw error
            null
        }

        if (pairing is PairingMessage.Hello) {
            servePairing(transport, pairing)
            return
        }

        val hello = SyncMessageCodec.read(first) as? SyncMessage.Hello
            ?: throw SyncProtocolException.Unauthenticated
        serveSession(transport, hello)
    }

    /** Runs the receiving half of a pairing handshake. */
    private suspend fun servePairing(transport: SyncTransport, peerHello: PairingMessage.Hello) {
        // An already-paired device says no instead of silently replacing the link a sync depends on.
        // Rejecting before the prompt also keeps a stranger from making the phone ring while its
        // owner is in the middle of something else.
        val busy = currentState.value.isPairing || currentState.value.pairingPrompt != null
        if (sdk.getPairingSecret() != null || busy) {
            runCatching {
                transport.open()
                FramedLink(transport).sendRaw(PairingMessageCodec.write(PairingMessage.Reject()))
            }
            transport.close()
            return
        }

        currentState.update { it.copy(isPairing = true) }
        val result = runCatching {
            runPairingSession(
                transport = transport,
                local = PairingMessage.Hello(
                    device = localDescriptor(sdk.getPairingSecret()),
                    nonce = randomNonce()
                ),
                peerHello = peerHello,
                onPeer = ::askPairingApproval,
                onPaired = ::rememberPairing
            )
        }

        currentState.update { it.copy(isPairing = false, pairingPrompt = null) }
        result
            .onSuccess { outcome ->
                if (outcome is PairingResult.Declined) {
                    currentState.update { it.copy(outcome = SyncOutcome.Declined) }
                }
            }
            .onFailure { fail(it) }
    }

    /**
     * Puts the session code on screen and waits for the user to compare it with the other device.
     *
     * Called on both sides of a pairing. The code is only meaningful if both people look at both
     * screens, which is why the answer is a deliberate tap rather than a timeout.
     */
    private suspend fun askPairingApproval(peer: DeviceDescriptor, code: String): Boolean {
        if (currentState.value.isSyncing) return false

        val answer = CompletableDeferred<Boolean>()
        pendingPairingApproval = answer
        currentState.update {
            it.copy(pairingPrompt = PairingPrompt(device = peer.toNearbyDevice(), code = code))
        }

        return try {
            answer.await()
        } finally {
            pendingPairingApproval = null
            currentState.update { it.copy(pairingPrompt = null) }
        }
    }

    private fun answerPairing(accepted: Boolean) {
        pendingPairingApproval?.complete(accepted)
        currentState.update { it.copy(pairingPrompt = null) }
    }

    /** Stores the secret both sides now hold, and swaps the advertisement for the real fingerprint. */
    private suspend fun rememberPairing(peer: DeviceDescriptor, secret: ByteArray) {
        sdk.savePairingSecret(
            secret = secret,
            linkedDevice = LinkedDevice(
                deviceId = peer.deviceId,
                alias = peer.alias.ifBlank { LinkedDevice.DefaultAlias },
                fingerprint = deviceFingerprint(secret)
            )
        )
        currentState.update {
            it.copy(
                paired = true,
                linkedDevice = sdk.getLinkedDevice(),
                pairingPrompt = null,
                isPairing = false,
                outcome = null
            )
        }

        // Until now the advertisement carried the unpaired placeholder. Restarting it with the new
        // fingerprint lets the peer recognise this device on the scan that follows.
        runCatching { link?.start(localDescriptor(secret)) }
    }

    /**
     * Runs the receiving half of a session.
     *
     * The peer's request is put on screen and nothing crosses the link until the user answers, so a
     * refusal leaves both devices untouched.
     */
    private suspend fun serveSession(transport: SyncTransport, peerHello: SyncMessage.Hello) {
        val secret = sdk.getPairingSecret()
        if (secret == null) {
            transport.close()
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
                local = localDescriptor(secret),
                secret = secret,
                snapshot = snapshot,
                request = null,
                onRequest = { request ->
                    currentState.update { it.copy(incomingRequest = request.toIncomingRequest()) }
                    answer.await()
                },
                peerHello = peerHello
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
        if (state.isSyncing || state.isPairing) return
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
            stopScanning()
            if (!ensureLink()) return@launch
            val current = link ?: return@launch

            busy(SyncPhase.Connecting)

            val secret = sdk.getPairingSecret()
            if (secret == null) {
                fail(SyncProtocolException.Unauthenticated)
                return@launch
            }

            val connected = runCatching { current.connectTo(device.id) }.getOrElse {
                fail(it)
                return@launch
            }

            val snapshot = sdk.exportSyncSnapshot()
            val descriptor = localDescriptor(secret)
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
                // Nothing else is expected over this link now, and advertising is started again on
                // demand, so the radio is turned off rather than left on until the screen closes.
                stopDiscovering()
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
                    pairingPrompt = null,
                    isPairing = false,
                    outgoingCode = null,
                    outcome = null
                )
            }
            // The advertisement still carries the old fingerprint, which no longer names a secret
            // this device holds. Restarting it with the unpaired placeholder keeps the picker honest.
            if (isLiveSyncSupported() && hasNearbyDevicePermissions()) {
                ensureLink()
            }
        }
    }

    /**
     * Who this device presents itself as, both in the advertisement and in the handshake.
     *
     * [descriptor] has to be rebuilt per session rather than cached, because the fingerprint is derived
     * from the current secret and would be stale after re-pairing. While nothing is paired there is no
     * secret to derive it from, so a fixed placeholder is published: it is only a label in the picker,
     * and the pairing handshake replaces it with the real fingerprint.
     */
    private suspend fun localDescriptor(secret: ByteArray?): DeviceDescriptor = DeviceDescriptor(
        deviceId = sdk.deviceId(),
        alias = sdk.deviceAlias().ifBlank { deviceDisplayName() },
        model = deviceModel(),
        type = DeviceType.Mobile,
        fingerprint = secret?.let(::deviceFingerprint) ?: UnpairedFingerprint
    )

    /**
     * Opens the link if it is not open yet and (re)starts advertising.
     *
     * The existing link is reused rather than replaced: the GATT server and its accept loop belong to
     * it, and starting a second one would leave the first advertising until its own timeout.
     */
    private suspend fun startLink(): SyncLink {
        val current = link ?: createSyncLink()
        current.start(localDescriptor(sdk.getPairingSecret()))
        link = current
        return current
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

private fun DeviceDescriptor.toNearbyDevice() = NearbyDevice(
    id = deviceId,
    alias = alias,
    model = model,
    type = type,
    fingerprint = fingerprint
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

/** Settling time between closing the pairing link and opening the first sync link on the same peer. */
private const val PairingToSyncDelayMillis = 250L

/** Published while no secret exists. Only a label: pairing replaces it with the real fingerprint. */
private const val UnpairedFingerprint = "000000"
