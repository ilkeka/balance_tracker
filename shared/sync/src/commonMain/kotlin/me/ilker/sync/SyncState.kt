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

    /** A pairing code that is not shaped like one, or whose embedded digest does not match. */
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
    val pairingCode: String? = null,
    val isPairingInProgress: Boolean = false,
    val isDiscovering: Boolean = false,
    val nearbyDevices: List<NearbyDevice> = emptyList(),
    val selectedDeviceId: String? = null,
    /** Non-null while another device is waiting for this one to accept or decline. */
    val incomingRequest: IncomingRequest? = null,
    val isIncomingRequestAnswered: Boolean = false,
    val outgoingCode: String? = null,
    val isScannerVisible: Boolean = false,
    val outcome: SyncOutcome? = null
) : me.ilker.core.State

sealed interface SyncIntent : Intent {
    data object Load : SyncIntent

    /** Mints a fresh one-time secret and shows it as a QR code for the other device to scan. */
    data object Pair : SyncIntent

    /** Adopts a secret read off the other device, either scanned or pasted. */
    data class PairWith(val code: String) : SyncIntent

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
    data object OpenScanner : SyncIntent
    data object CloseScanner : SyncIntent
    data object DismissPairingCode : SyncIntent
    data object DismissOutcome : SyncIntent
}

sealed interface SyncSideEffect : SideEffect {
    data class CopyToClipboard(val value: String) : SyncSideEffect

    /** Asks the platform layer for the runtime Bluetooth permissions. */
    data object RequestBluetoothPermission : SyncSideEffect
}