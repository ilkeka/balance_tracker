package me.ilker.sync

import kotlinx.serialization.Serializable
import me.ilker.balance_tracker.sync.LinkedDevice
import me.ilker.core.Intent
import me.ilker.core.SideEffect
import me.ilker.sync.protocol.DeviceType

enum class SyncPhase {
    Connecting,
    Transferring,
    Merging
}

enum class SyncFailure {
    /** The peer does not hold our pairing secret, or the payload was altered in transit. */
    Unauthenticated,

    /** The peer speaks a different protocol version. */
    Incompatible,

    /** The radio is off, unpermissioned, or missing on this device. */
    BluetoothUnusable,

    /** The radio works but nothing matching this app advertised nearby. */
    NoDevicesNearby,

    /** A pasted pairing code that is not shaped like one, or whose embedded digest does not match. */
    UnreadableCode,

    /** The link dropped or never completed the exchange. */
    Transport,

    Unknown
}

sealed interface SyncOutcome {
    data class Merged(val received: Int, val applied: Int) : SyncOutcome
    data object UpToDate : SyncOutcome

    /** The peer declined the request, so nothing moved in either direction. */
    data object Declined : SyncOutcome

    data class Failed(val failure: SyncFailure) : SyncOutcome
}

/**
 * A nearby device the user can pick, as advertised.
 *
 * [id] is the platform handle discovery produced and is never displayed; the rest is what the peer
 * chose to publish about itself.
 */
@Serializable
data class NearbyDevice(
    val id: String,
    val alias: String,
    val model: String?,
    val type: DeviceType,
    val fingerprint: String
)

/**
 * A device waiting for this one to confirm a Bluetooth pairing.
 *
 * [code] is the six characters both users compare. Unlike the code shown by the old QR flow, it is
 * not a secret: it only names the session so two people in the same room can tell they are pairing
 * with each other.
 */
data class PairingPrompt(
    val device: NearbyDevice,
    val code: String
)

/** A device asking to sync, held on screen until the user answers. */
data class IncomingRequest(
    val device: NearbyDevice,
    val transactionCount: Int
) {
    /**
     * The public half of the pairing secret, shown so the user can check it against the other phone
     * before approving. Two devices that paired out of band show the same six characters, which is
     * what makes the prompt meaningful rather than a blind accept.
     */
    val verifyCode: String get() = device.fingerprint
}

data class SyncState(
    val isLoading: Boolean = true,
    val deviceId: String = "",
    val alias: String = "",
    val paired: Boolean = false,
    val linkedDevice: LinkedDevice? = null,
    val liveSyncSupported: Boolean = false,
    val isSyncing: Boolean = false,
    val phase: SyncPhase? = null,
    /** Set on targets without a radio while a one-time code is being shown for a document sync. */
    val pairingCode: String? = null,
    val isPairingInProgress: Boolean = false,
    /** Non-null while the two devices are comparing the code that will link them over Bluetooth. */
    val pairingPrompt: PairingPrompt? = null,
    /** True between tapping a nearby device and the pairing handshake finishing. */
    val isPairing: Boolean = false,
    val isDiscovering: Boolean = false,
    val nearbyDevices: List<NearbyDevice> = emptyList(),
    val selectedDeviceId: String? = null,
    /** Non-null while another device is waiting for this one to accept or decline. */
    val incomingRequest: IncomingRequest? = null,
    val isIncomingRequestAnswered: Boolean = false,
    val outgoingCode: String? = null,
    val outcome: SyncOutcome? = null
) : me.ilker.core.State

sealed interface SyncIntent : Intent {
    data object Load : SyncIntent

    /**
     * Marks the sync screen as no longer visible.
     *
     * An active transfer keeps the link alive so it can finish in the background, while an idle
     * screen stops advertising immediately.
     */
    data object ScreenLeft : SyncIntent

    /**
     * Mints a fresh one-time secret and shows it as text.
     *
     * Only offered where there is no radio to pair over, because a device with Bluetooth pairs by
     * finding the other one instead of copying a code.
     */
    data object Pair : SyncIntent

    /** Adopts a secret created on another device, either pasted here or typed in. */
    data class PairWith(val code: String) : SyncIntent

    /** Connects to a nearby device and asks both users to confirm the same code. */
    data class PairDevice(val deviceId: String) : SyncIntent

    data object AcceptPairing : SyncIntent
    data object RejectPairing : SyncIntent

    /** Starts advertising and scanning for nearby devices. */
    data object Discover : SyncIntent

    data object StopDiscovering : SyncIntent

    /** Marks the device the user tapped in the nearby list. */
    data class SelectDevice(val deviceId: String) : SyncIntent

    /** Connects to the selected device and asks it to sync. */
    data object SyncNow : SyncIntent

    /** Asks the user whether to accept a request that arrived from another device. */
    data object AcceptIncoming : SyncIntent

    data object RejectIncoming : SyncIntent

    data object Export : SyncIntent
    data class Import(val code: String) : SyncIntent
    data object Unpair : SyncIntent
    data object DismissPairingCode : SyncIntent
    data object DismissOutcome : SyncIntent
}

sealed interface SyncSideEffect : SideEffect {
    data class CopyToClipboard(val value: String) : SyncSideEffect
}
