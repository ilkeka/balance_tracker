package me.ilker.sync.transport

import me.ilker.sync.protocol.DeviceDescriptor

/**
 * A nearby device running this app, as read from its BLE advertisement.
 *
 * [address] is the platform handle needed to connect. It is deliberately kept out of anything the
 * user sees: MAC addresses are user-identifying and Android already rotates them per connection.
 */
internal data class DiscoveredDevice(
    val address: String,
    val descriptor: DeviceDescriptor
) {
    /** Stable key for list diffing; the address is unique among scan results. */
    val id: String get() = address
}

/** Why a nearby-device link could not be used, so the UI can say something specific. */
internal enum class SyncUnavailableReason {
    /** No usable radio, permission missing, or the adapter is switched off. */
    BluetoothUnusable,

    /** The radio works but nothing matching this app advertised within the scan window. */
    NoDevicesNearby
}

internal sealed class SyncDiscoveryException(message: String) : Exception(message) {
    data class Unavailable(val reason: SyncUnavailableReason) : SyncDiscoveryException(reason.name) {
        private fun readResolve(): Any = this
    }
}

/**
 * A bidirectional link to a nearby device that carries framed chunks.
 *
 * Transports know nothing about messages, merging or authentication, which is why the same session
 * drives every platform's implementation.
 */
internal interface SyncTransport {
    /**
     * Largest chunk this link can carry. BLE links report the negotiated MTU minus the characteristic
     * header, which is why chunking is a transport concern rather than a fixed value.
     */
    val maxChunkSize: Int

    /** Establishes the link, suspending until bytes can flow. */
    suspend fun open()

    suspend fun write(chunk: ByteArray)

    /** Suspends until the next chunk arrives from the peer. */
    suspend fun read(): ByteArray

    suspend fun close()
}

/**
 * Everything needed to find and be found by a nearby device.
 *
 * Advertising and serving are long-lived and shared by every attempt, while a [SyncTransport] is one
 * connection, so the two are separate types. A device both announces and accepts because a sync is
 * symmetric: whoever presses the button connects out, and whoever was already on the screen receives.
 */
internal interface SyncLink {
    /**
     * Starts advertising [descriptor] and serving incoming connections.
     *
     * Idempotent, so re-entering the screen does not stack advertisements.
     */
    fun start(descriptor: DeviceDescriptor)

    /** Stops advertising, serving and scanning. Safe to call when nothing is running. */
    fun stop()

    /**
     * Connects to the device the user picked, suspending until bytes can flow.
     *
     * [deviceId] is the opaque handle discovery produced; implementations turn it into whatever their
     * radio needs, so nothing above the transport has to know it is a Bluetooth address.
     *
     * @throws SyncTransportException.Unavailable when that device is no longer reachable.
     */
    suspend fun connectTo(deviceId: String): SyncTransport

    /**
     * Waits for a peer to connect to us and returns the transport for that session.
     *
     * Never resolves on its own: if nobody connects, the caller's timeout decides, so a receive
     * attempt cannot hang a coroutine for the life of the app.
     */
    suspend fun accept(): SyncTransport

    /**
     * Scans for [timeoutMillis], emitting devices as they are found so the picker fills in
     * progressively. Repeats for one address are suppressed, keeping the most recent descriptor.
     *
     * Does not return early when devices are found: the user chooses from the list, so stopping at
     * the first hit would hide the device they actually meant.
     */
    suspend fun discover(
        timeoutMillis: Long,
        onFound: suspend (DiscoveredDevice) -> Unit
    )
}

internal sealed class SyncTransportException(message: String) : Exception(message) {
    data class Unavailable(val reason: String) : SyncTransportException(reason) {
        private fun readResolve(): Any = this
    }

    data class Failed(val reason: String) : SyncTransportException(reason) {
        private fun readResolve(): Any = this
    }

    /** The peer went away mid-session, so the exchange did not complete. */
    data class Disconnected(val reason: String) : SyncTransportException(reason) {
        private fun readResolve(): Any = this
    }

    /** The user did not answer the approval prompt in time, so the session was dropped. */
    data object ApprovalTimedOut : SyncTransportException("the incoming request was not answered") {
        private fun readResolve(): Any = ApprovalTimedOut
    }
}

/** Whether this platform can announce itself and exchange data with a nearby device. */
internal expect fun isLiveSyncSupported(): Boolean

/**
 * The nearby-device link for this platform.
 *
 * @throws SyncDiscoveryException.Unavailable when the radio cannot be used at all, so the caller can
 *   report the reason instead of enumerating the causes itself.
 */
internal expect fun createSyncLink(): SyncLink

/**
 * Stand-in for platforms with no nearby-device link.
 *
 * Reporting [SyncUnavailableReason.BluetoothUnusable] rather than silently doing nothing means the
 * desktop, web and iOS builds all fail the same way, so the caller only handles one path.
 */
internal class UnsupportedSyncLink(private val reason: String) : SyncLink {
    override fun start(descriptor: DeviceDescriptor) = Unit

    override fun stop() = Unit

    override suspend fun connectTo(deviceId: String): SyncTransport =
        throw SyncTransportException.Unavailable(reason)

    override suspend fun accept(): SyncTransport = throw SyncTransportException.Unavailable(reason)

    override suspend fun discover(
        timeoutMillis: Long,
        onFound: suspend (DiscoveredDevice) -> Unit
    ): Unit = throw SyncDiscoveryException.Unavailable(SyncUnavailableReason.BluetoothUnusable)
}
