package me.ilker.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sync.LinkedDevice
import me.ilker.balance_tracker.sync.SyncImportResult
import me.ilker.balance_tracker.sync.SyncSnapshot
import me.ilker.core.Manager
import me.ilker.sync.platform.deviceDisplayName
import me.ilker.sync.platform.deviceModel
import me.ilker.sync.platform.hasNearbyDevicePermissions
import me.ilker.sync.protocol.DescriptorCodec
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
import me.ilker.sync.protocol.unpairProof
import me.ilker.sync.protocol.verifiesUnpair
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

    /**
     * Whether the sync screen is currently on screen.
     *
     * The manager outlives the navigation entry so a transfer can continue while the user visits
     * another screen. Visibility is tracked separately from that lifetime: an idle link still stops
     * on the way out, while an active session is allowed to finish.
     */
    private val screenVisible = MutableStateFlow(false)

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

    /**
     * Serialises every scan, including the silent ones that validate or revoke a link.
     *
     * A platform link has one scanner, so overlapping windows would replace each other's callback and
     * leave one caller waiting forever for results that will never arrive.
     */
    private val scanMutex = Mutex()

    override val state: StateFlow<SyncState> = currentState.asStateFlow()
    override val sideEffect = Channel<SyncSideEffect>(capacity = Channel.BUFFERED)

    override fun sendIntent(intent: SyncIntent) {
        when (intent) {
            SyncIntent.Load -> load()
            SyncIntent.ScreenLeft -> screenLeft()
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
        screenVisible.value = true
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

            // A transfer that started before the user left is still using the link. Reopening or
            // validating it here would race the session, so only an idle screen sets one up.
            if (!screenVisible.value || currentState.value.isSyncing || currentState.value.isPairing) {
                return@launch
            }

            // Start advertising as soon as the screen opens, so the other phone can find this one
            // without the user doing anything here. Paired devices advertise too: that is what lets
            // the peer reach them to sync, and to report that it has just unpaired. Only when the
            // permissions are already held: a prompt before the user asked for anything would be a
            // worse first impression than waiting for the "look for nearby devices" tap.
            if (isLiveSyncSupported() && hasNearbyDevicePermissions()) {
                if (ensureLink() && paired) reconcileLinkedPeerOnce()
            }
        }
    }

    /**
     * Handles the navigation entry going away.
     *
     * A running transfer keeps the radio: that is what lets the user visit another screen without
     * interrupting it. Everything else is torn down here, and prompts are declined because nobody is
     * left to answer them. Session paths keep the link until their own completion notices that the
     * screen is gone.
     */
    private fun screenLeft() {
        screenVisible.value = false

        // Also covers a session that has connected but has not raised its prompt yet: without a
        // screen nobody can answer it, and leaving the deferred waiting would hold the peer open.
        val waitingForUser =
            currentState.value.pairingPrompt != null ||
                currentState.value.incomingRequest != null ||
                pendingPairingApproval != null ||
                pendingApproval != null
        if (waitingForUser) {
            pendingPairingApproval?.complete(false)
            pendingApproval?.complete(false)
            pendingPairingApproval = null
            pendingApproval = null
            currentState.update {
                it.copy(
                    pairingPrompt = null,
                    incomingRequest = null,
                    isIncomingRequestAnswered = true
                )
            }
        }

        stopScanning()

        if (currentState.value.isSyncing || currentState.value.isPairing) {
            // The picker is screen-local: keeping stale rows would let the user start a session with
            // a device that may no longer be reachable when they return.
            currentState.update { it.copy(nearbyDevices = emptyList(), selectedDeviceId = null) }
        } else {
            stopLink()
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

                    PairingResult.Declined -> {
                        currentState.update {
                            it.copy(outcome = SyncOutcome.Declined)
                        }
                        stopLinkIfScreenLeft()
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
     * link outlives an individual scan window and is dropped when the screen is left or a session
     * that continued off-screen finishes.
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
        stopLink()
    }

    /**
     * Drops the radio link and the picker rows that depended on it.
     *
     * Only safe when no session is using the link. The completion paths call this after a transfer
     * ends and the user has already left the screen.
     */
    private fun stopLink() {
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
     * Turns the radio off after a session ends if its screen is no longer visible.
     *
     * A session that is still running is left alone: this can be reached from pairing cleanup while
     * the first sync has already been queued.
     */
    private fun stopLinkIfScreenLeft() {
        if (screenVisible.value || currentState.value.isSyncing) return
        stopLink()
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
                scanMutex.withLock {
                    current.discover(timeoutMillis = ScanWindowMillis) { device -> found.send(device) }
                }
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
            // Seeing the peer advertise the unpaired placeholder means the secret was removed on the
            // other end: the stored link can never authenticate again, so drop it here as well.
            val linked = currentState.value.linkedDevice
            if (currentState.value.paired && linked != null && device.descriptor.revokesLink(linked)) {
                forgetPairing()
            }

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

    /**
     * A short, silent scan on entry that catches a peer which unpaired while this screen was closed.
     *
     * Nothing is reported when the peer is absent: a device that is simply not nearby has not revoked
     * anything, and clearing the link just because it stayed silent would be destructive.
     */
    private suspend fun reconcileLinkedPeerOnce() {
        val linked = currentState.value.linkedDevice ?: return
        val current = link ?: return
        val revoked = scanForPeer(current, PeerValidationScanMillis) { descriptor ->
            descriptor.revokesLink(linked)
        } ?: return

        if (currentState.value.paired) {
            forgetPairing()
        }
    }

    /**
     * Scans until [matches] accepts a descriptor or the window closes.
     *
     * The scan is cancelled as soon as something matches instead of running out the whole window,
     * because this is a background check and the caller has a session to start.
     */
    private suspend fun scanForPeer(
        current: SyncLink,
        timeoutMillis: Long,
        matches: (DeviceDescriptor) -> Boolean
    ): DiscoveredDevice? = coroutineScope {
        val found = CompletableDeferred<DiscoveredDevice?>()
        val job = launch {
            try {
                scanMutex.withLock {
                    current.discover(timeoutMillis = timeoutMillis) { device ->
                        if (matches(device.descriptor)) found.complete(device)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // Validation and revocation are best effort: a failed scan must not replace the
                // screen's state with a transport error.
            } finally {
                found.complete(null)
            }
        }

        val result = found.await()
        job.cancel()
        result
    }

    /**
     * Finds the stored peer for a sync started without a picker row.
     *
     * The address from the pairing session cannot be reused because BLE addresses rotate. The
     * fingerprint in the advertisement is derived from the shared secret, so it is the stable way
     * to recognise the device the user already paired with.
     */
    private suspend fun findLinkedPeer(current: SyncLink): NearbyDevice? {
        val linked = sdk.getLinkedDevice() ?: return null
        return scanForPeer(current, PairedSyncScanMillis) { descriptor ->
            descriptor.isLinkedPeer(linked)
        }?.toNearbyDevice()
    }

    /** Accepts incoming sessions for as long as the link is up, one at a time. */
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
        // A connection can arrive just as the user leaves. Without a screen there is no one to
        // approve a pairing code or an incoming request, so close it rather than raise a prompt the
        // user cannot see.
        if (!screenVisible.value) {
            runCatching { transport.close() }
            return
        }

        val first = transport.readMessageText()
        val pairing = runCatching<PairingMessage?> { PairingMessageCodec.read(first) }.getOrElse { error ->
            if (error is SyncProtocolException.IncompatibleVersion) throw error
            null
        }

        if (pairing is PairingMessage.Hello) {
            servePairing(transport, pairing)
            return
        }

        if (pairing is PairingMessage.Unpair) {
            serveUnpair(transport, pairing)
            return
        }

        val hello = SyncMessageCodec.read(first) as? SyncMessage.Hello
            ?: throw SyncProtocolException.Unauthenticated
        serveSession(transport, hello)
    }

    /**
     * Applies a remote unpair, after checking that the sender really held the secret being revoked.
     *
     * The proof is what keeps a stranger from clearing the link just by finding the advertisement.
     * An already-unpaired receiver has nothing to clear and simply lets the sender time out.
     */
    private suspend fun serveUnpair(transport: SyncTransport, message: PairingMessage.Unpair) {
        val secret = sdk.getPairingSecret()
        val linked = sdk.getLinkedDevice()
        val fromLinkedDevice = linked == null || linked.deviceId == message.device.deviceId
        val authorized = secret != null &&
            fromLinkedDevice &&
            verifiesUnpair(secret = secret, message = message, receiverDeviceId = sdk.deviceId())

        try {
            if (!authorized) return

            runCatching {
                transport.open()
                FramedLink(transport).sendRaw(PairingMessageCodec.write(PairingMessage.Accept()))
            }
            forgetPairing()
        } finally {
            transport.close()
        }
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
        stopLinkIfScreenLeft()
    }

    /**
     * Puts the session code on screen and waits for the user to compare it with the other device.
     *
     * Called on both sides of a pairing. The code is only meaningful if both people look at both
     * screens, which is why the answer is a deliberate tap rather than a timeout.
     */
    private suspend fun askPairingApproval(peer: DeviceDescriptor, code: String): Boolean {
        // Nobody is on the screen to compare the code, so decline instead of raising an invisible
        // prompt that would hold the peer until it times out.
        if (!screenVisible.value || currentState.value.isSyncing) return false

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
            // Say why this session cannot continue. Closing silently would look like a dropped link,
            // and the initiator would keep showing the peer as paired instead of clearing it.
            runCatching {
                transport.open()
                FramedLink(transport).sendRaw(
                    SyncMessageCodec.write(SyncMessage.Unpaired(device = localDescriptor(secret = null)))
                )
            }
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
                    // The screen can disappear between the connection arriving and the request
                    // reaching this point. Declining keeps the peer from waiting on a prompt that
                    // no one can see.
                    if (!screenVisible.value) {
                        false
                    } else {
                        currentState.update { it.copy(incomingRequest = request.toIncomingRequest()) }
                        answer.await()
                    }
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

        // A picker row already describes a reachable peer. When there is none, the stored pairing
        // is enough to find it: the user should not have to scan and select a device they already
        // paired with.
        val nearby = state.selectedDeviceId?.let { selected ->
            state.nearbyDevices.firstOrNull { it.id == selected }
        } ?: state.nearbyDevices.firstOrNull()

        scope.launch {
            stopScanning()
            // Marked busy before opening the link so leaving the screen in that gap does not tear
            // down the link this session is about to use.
            busy(SyncPhase.Connecting)
            if (!ensureLink()) return@launch
            val current = link ?: run {
                fail(SyncTransportException.Disconnected("the link closed before the sync started"))
                return@launch
            }

            val device = nearby ?: findLinkedPeer(current)
            if (device == null) {
                fail(SyncDiscoveryException.Unavailable(SyncUnavailableReason.NoDevicesNearby))
                return@launch
            }

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
            }.onSuccess { outcome -> finishSession(outcome) }.onFailure { error ->
                val linked = currentState.value.linkedDevice
                if (error is SyncProtocolException.PeerUnpaired &&
                    linked != null &&
                    device.fingerprint == linked.fingerprint
                ) {
                    // The device we tried to sync with is the one stored as linked, and it has just
                    // said it no longer holds the secret. Clearing here avoids showing a dead link.
                    forgetPairing()
                } else {
                    fail(error)
                }
            }
        }
    }

    /** Merges what the peer sent, or reports that there was nothing to do. */
    private suspend fun finishSession(outcome: SyncSessionResult) {
        try {
            when (outcome) {
                SyncSessionResult.Declined -> currentState.update {
                    it.copy(isSyncing = false, phase = null, outcome = SyncOutcome.Declined)
                }

                is SyncSessionResult.Completed -> {
                    rememberLinkedDevice(outcome.peer)
                    // The picker stops growing, but the link keeps advertising and serving. The peer may
                    // come back for another sync, or to report that it has unpaired. Visibility is what
                    // turns the radio off once the screen is gone.
                    stopScanning()
                    outcome.peerSnapshot?.let { merge(it) }
                        ?: currentState.update {
                            it.copy(isSyncing = false, phase = null, outcome = SyncOutcome.UpToDate)
                        }
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            // Storing the peer or merging its rows can fail after the exchange itself completed.
            // Report it like any other session failure so the busy state cannot outlive the transfer.
            fail(throwable)
        } finally {
            // If the user left during the transfer, this is the point where the radio is no longer
            // needed. A visible screen keeps advertising for the next session, as before.
            stopLinkIfScreenLeft()
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
        val result = sdk.importSyncSnapshot(snapshot)
        val outcome = when (result) {
            // Each device contributes only its own rows, so "received" is the peer's ledger, not the
            // number of changes. Rows the peer sent but does not own are not counted as received.
            is SyncImportResult.Merged -> if (result.stats.applied == 0) {
                SyncOutcome.UpToDate
            } else {
                SyncOutcome.Merged(
                    received = snapshot.transactions.size - result.stats.ignored,
                    applied = result.stats.applied
                )
            }

            SyncImportResult.SameDevice -> SyncOutcome.UpToDate
        }
        currentState.update { it.copy(isSyncing = false, phase = null, outcome = outcome) }
    }

    private fun unpair() {
        scope.launch {
            // Captured before the secret is cleared: the old secret is what lets the peer verify that
            // this is a real revocation from the device it paired with, not a stranger's spoof.
            val secret = sdk.getPairingSecret()
            val linked = sdk.getLinkedDevice()

            stopScanning()
            forgetPairing()
            currentState.update { it.copy(nearbyDevices = emptyList(), selectedDeviceId = null) }

            if (!isLiveSyncSupported() || !hasNearbyDevicePermissions()) return@launch
            if (!ensureLink()) return@launch

            // Best effort. If the peer is not nearby the local link is still gone, and the peer will
            // notice the next time it scans or tries to sync.
            if (secret != null && linked != null) {
                notifyPeerOfUnpair(secret = secret, linked = linked)
            }
        }
    }

    /** Drops the pairing locally and advertises the unpaired placeholder on the existing link. */
    private suspend fun forgetPairing() {
        // A prompt that was waiting on the old link has nothing left to authorize. Releasing it as a
        // refusal keeps a pairing or sync session from sitting on screen after the link is gone.
        pendingPairingApproval?.complete(false)
        pendingApproval?.complete(false)
        pendingPairingApproval = null
        pendingApproval = null

        sdk.clearPairingSecret()
        currentState.update {
            it.copy(
                paired = false,
                linkedDevice = null,
                isSyncing = false,
                phase = null,
                isPairingInProgress = false,
                pairingCode = null,
                pairingPrompt = null,
                isPairing = false,
                incomingRequest = null,
                isIncomingRequestAnswered = true,
                outgoingCode = null,
                outcome = null
            )
        }

        // The advertisement still carries the old fingerprint, which no longer names a secret this
        // device holds. Restarting it with the unpaired placeholder keeps the picker honest.
        runCatching { link?.start(localDescriptor(secret = null)) }
    }

    /**
     * Tells the peer that this device has removed their shared secret.
     *
     * The peer is found by its advertised fingerprint rather than by a stored address: BLE addresses
     * rotate, while the fingerprint is exactly the public half of the secret being revoked.
     */
    private suspend fun notifyPeerOfUnpair(secret: ByteArray, linked: LinkedDevice) {
        val current = link ?: return
        if (link !== current) return

        val target = scanForPeer(current, UnpairNotifyScanMillis) { descriptor ->
            descriptor.isLinkedPeer(linked)
        } ?: return
        // The screen may have been left during the scan, which stops the link this target came from.
        if (link !== current) return

        val transport = runCatching { current.connectTo(target.id) }.getOrNull() ?: return
        try {
            transport.open()
            val framed = FramedLink(transport)
            val local = localDescriptor(secret = null)
            framed.sendRaw(
                PairingMessageCodec.write(
                    PairingMessage.Unpair(
                        device = local,
                        proof = unpairProof(
                            secret = secret,
                            senderDeviceId = local.deviceId,
                            receiverDeviceId = linked.deviceId
                        )
                    )
                )
            )
            // The ack is useful but not required: the write above already reached the peer's server.
            withTimeoutOrNull(UnpairAckTimeoutMillis) { framed.readRaw() }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            // Best effort, as above.
        } finally {
            runCatching { transport.close() }
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
        stopLinkIfScreenLeft()
    }

    private fun Throwable.toSyncFailure(): SyncFailure = when (this) {
        is SyncProtocolException.Unauthenticated,
        is SyncProtocolException.PeerUnpaired -> SyncFailure.Unauthenticated

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

/**
 * Whether an advertised descriptor is the device stored as linked.
 *
 * Advertisements only carry the first four bytes of the id, so the prefix is the stable part to
 * compare; no full id ever reaches the scan result.
 */
private fun DeviceDescriptor.sharesDeviceIdWith(linked: LinkedDevice): Boolean =
    deviceId.isNotEmpty() &&
        deviceId.length <= DescriptorCodec.DeviceIdHexLength &&
        linked.deviceId.startsWith(deviceId, ignoreCase = true)

/** The linked peer, still holding the same secret: the only device an unpair notice may target. */
private fun DeviceDescriptor.isLinkedPeer(linked: LinkedDevice): Boolean =
    sharesDeviceIdWith(linked) && fingerprint.equals(linked.fingerprint, ignoreCase = true)

/** The linked peer after it has replaced its fingerprint with the unpaired placeholder. */
private fun DeviceDescriptor.revokesLink(linked: LinkedDevice): Boolean =
    sharesDeviceIdWith(linked) &&
        linked.fingerprint != UnpairedFingerprint &&
        fingerprint == UnpairedFingerprint

private const val ScanWindowMillis = 15_000L

/** Settling time between closing the pairing link and opening the first sync link on the same peer. */
private const val PairingToSyncDelayMillis = 250L

/** Short enough not to hold the picker up, long enough to see a peer that is already advertising. */
private const val PeerValidationScanMillis = 2_000L

/** A paired peer is already advertising; this is long enough to catch it without feeling stuck. */
private const val PairedSyncScanMillis = 10_000L

/** A revocation is useful as soon as it is sent, so this window is deliberately brief. */
private const val UnpairNotifyScanMillis = 3_000L

private const val UnpairAckTimeoutMillis = 1_000L

/** Published while no secret exists. Only a label: pairing replaces it with the real fingerprint. */
private const val UnpairedFingerprint = "000000"
