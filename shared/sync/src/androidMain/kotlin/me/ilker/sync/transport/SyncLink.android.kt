package me.ilker.sync.transport

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import me.ilker.sync.protocol.DescriptorCodec
import me.ilker.sync.protocol.DeviceDescriptor
import java.util.UUID

internal actual fun isLiveSyncSupported(): Boolean = true

@SuppressLint("MissingPermission")
internal actual fun createSyncLink(): SyncLink {
    val context = AndroidContextHolder.context
        ?: throw SyncDiscoveryException.Unavailable(SyncUnavailableReason.BluetoothUnusable)

    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        ?: throw SyncDiscoveryException.Unavailable(SyncUnavailableReason.BluetoothUnusable)
    val adapter = manager.adapter
        ?: throw SyncDiscoveryException.Unavailable(SyncUnavailableReason.BluetoothUnusable)

    if (!context.hasPermission(Manifest.permission.BLUETOOTH_CONNECT) || !adapter.isEnabled) {
        throw SyncDiscoveryException.Unavailable(SyncUnavailableReason.BluetoothUnusable)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !context.hasPermission(Manifest.permission.BLUETOOTH_SCAN)
    ) {
        throw SyncDiscoveryException.Unavailable(SyncUnavailableReason.BluetoothUnusable)
    }

    return AndroidBleSyncLink(context = context, adapter = adapter, manager = manager)
}

/**
 * Bluetooth LE nearby-device link: advertising, scanning, connecting out, and serving connections from
 * peers.
 *
 * All four live in one object because they share the adapter and have to be torn down together. The
 * sync manager owns one instance and keeps it alive while the screen or an active session needs it.
 */
private class AndroidBleSyncLink(
    private val context: Context,
    private val adapter: BluetoothAdapter,
    private val manager: BluetoothManager
) : SyncLink {
    private var advertiseCallback: AdvertiseCallback? = null
    private var scanCallback: ScanCallback? = null
    private var server: BluetoothGattServer? = null
    private var descriptor: DeviceDescriptor? = null

    /** The characteristic notifications are sent from, held so it keeps its service association. */
    private var dataCharacteristic: BluetoothGattCharacteristic? = null

    /** Incoming sessions keyed by peer address, so one connection maps to one set of buffers. */
    private val served = mutableMapOf<String, ServedSession>()

    /**
     * Sessions waiting to be collected by whoever is receiving.
     *
     * Buffered rather than rendezvous: a connection that arrives before anyone asks to receive would
     * otherwise be dropped, and the peer would see that as the app having closed.
     */
    private val incoming = Channel<SyncTransport>(Channel.BUFFERED)

    @SuppressLint("MissingPermission")
    override fun start(descriptor: DeviceDescriptor) {
        this.descriptor = descriptor

        if (server == null) {
            val opened = openGattServer() ?: return
            val service = buildService()
            opened.addService(service)
            server = opened
            dataCharacteristic = service.getCharacteristic(SyncService.Data)
        }

        adapter.bluetoothLeAdvertiser?.let { advertiser ->
            // Stopped before restarting rather than left in place: the same link is reused across scan
            // windows, and a second registration would fail with "already started" while the previous
            // callback kept the advertisement alive after the sheet was closed.
            advertiseCallback?.let { previous -> runCatching { advertiser.stopAdvertising(previous) } }

            val callback = advertiseCallback()
            advertiseCallback = callback
            // Legacy advertising and scan response are 31 bytes each. The service UUID goes in the
            // advertisement so the scanner's service filter matches, and the descriptor goes in the
            // scan response: together they overflow a single packet by a wide margin.
            runCatching {
                advertiser.startAdvertising(
                    advertiseSettings(),
                    advertiseData(),
                    advertiseScanResponse(descriptor),
                    callback
                )
            }.onFailure { error ->
                Log.e(LogTag, "Could not request BLE advertising", error)
            }
        }
    }

    /**
     * Opens the GATT server, or returns null when this platform cannot.
     *
     * API 36 moved `openGattServer` from the adapter to the manager, and the compile-time stubs only
     * expose the new home, so older releases are reached reflectively rather than by a call that would
     * not link.
     */
    @SuppressLint("MissingPermission")
    private fun openGattServer(): BluetoothGattServer? {
        if (Build.VERSION.SDK_INT >= GattServerMovedToManagerApi) {
            return manager.openGattServer(context, serverCallback())
        }
        return runCatching {
            BluetoothAdapter::class.java
                .getMethod("openGattServer", Context::class.java, BluetoothGattServerCallback::class.java)
                .invoke(adapter, context, serverCallback()) as BluetoothGattServer
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    override fun stop() {
        advertiseCallback?.let { callback ->
            runCatching { adapter.bluetoothLeAdvertiser?.stopAdvertising(callback) }
        }
        advertiseCallback = null

        scanCallback?.let { callback ->
            runCatching { adapter.bluetoothLeScanner?.stopScan(callback) }
        }
        scanCallback = null

        served.values.forEach { session ->
            runCatching { server?.cancelConnection(session.device) }
            session.inbound?.close()
            session.inbound = null
        }
        served.clear()
        dataCharacteristic = null

        runCatching { server?.close() }
        server = null

        incoming.close()
    }

    @SuppressLint("MissingPermission")
    override suspend fun connectTo(deviceId: String): SyncTransport {
        val remote = runCatching { adapter.getRemoteDevice(deviceId) }.getOrNull()
            ?: throw SyncTransportException.Unavailable("that device is no longer reachable")

        return ClientBleTransport(context = context, remote = remote).also { it.connect() }
    }

    override suspend fun accept(): SyncTransport =
        incoming.receive() ?: throw SyncTransportException.Disconnected("the link was closed")

    @SuppressLint("MissingPermission")
    override suspend fun discover(
        timeoutMillis: Long,
        onFound: suspend (DiscoveredDevice) -> Unit
    ) {
        val scanner = adapter.bluetoothLeScanner
            ?: throw SyncDiscoveryException.Unavailable(SyncUnavailableReason.BluetoothUnusable)

        val seen = mutableSetOf<String>()
        val found = Channel<DiscoveredDevice>(Channel.UNLIMITED)

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) = publish(result)

            override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach(::publish)

            override fun onScanFailed(errorCode: Int) {
                Log.e(LogTag, "BLE scan failed with error code $errorCode")
                found.close(SyncDiscoveryException.Unavailable(SyncUnavailableReason.BluetoothUnusable))
            }

            /**
             * Anything that is not one of our advertisements, or a repeat of one, is dropped here.
             *
             * The address is marked as seen only once the manufacturer data decoded, because the
             * first report for an advertiser can be the advertisement packet alone: the descriptor
             * arrives in the scan response, and marking the address on that first callback would
             * discard the very packet that makes the device usable.
             */
            private fun publish(result: ScanResult) {
                val address = result.device.address ?: return
                Log.d(LogTag, "Scan result $address")
                val bytes = result.scanRecord?.manufacturerSpecificData?.get(DescriptorCodec.CompanyId)
                    ?: return
                val descriptor = DescriptorCodec.decode(bytes) ?: return
                if (!seen.add(address)) return
                Log.d(LogTag, "Found ${descriptor.alias} (${descriptor.type}) at $address")
                found.trySend(DiscoveredDevice(address, descriptor))
            }
        }

        scanCallback = callback
        // The service UUID is in the advertisement packet (the descriptor goes in the scan response),
        // so the controller can match peers without waking the app for every beacon in range.
        scanner.startScan(
            listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(SyncService.Uuid)).build()),
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                // Report every advertisement, so a peer that renames itself still shows correctly.
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build(),
            callback
        )

        try {
            // The whole window is scanned even once devices are found: the user picks from the list, so
            // stopping at the first hit would hide the device they actually meant.
            withTimeout(timeoutMillis) { for (device in found) onFound(device) }
        } catch (timeout: TimeoutCancellationException) {
            // A finished scan window is the normal outcome, not a failure.
        } finally {
            scanCallback?.let { runCatching { adapter.bluetoothLeScanner?.stopScan(it) } }
            scanCallback = null
            found.close()
        }
    }

    private fun buildService(): BluetoothGattService = BluetoothGattService(
        SyncService.Uuid,
        BluetoothGattService.SERVICE_TYPE_PRIMARY
    ).apply {
        addCharacteristic(
            BluetoothGattCharacteristic(
                SyncService.Data,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
            ).apply {
                addDescriptor(
                    BluetoothGattDescriptor(
                        SyncService.ClientConfig,
                        BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
                    )
                )
            }
        )
        // Readable rather than a descriptor: a descriptor read can only report a handle's contents, and
        // the peer needs the whole 27-byte payload, which is what a read request can carry.
        addCharacteristic(
            BluetoothGattCharacteristic(
                SyncService.Descriptor,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
        )
    }

    /**
     * Starts a fresh transport for a peer that has just enabled notifications.
     *
     * The subscription, rather than the connection, is what marks a session. A peer that closes
     * right after one session and reconnects quickly can keep the same server-side connection
     * before the stack reports the change; the peer still subscribes again, so starting the
     * transport here means that reconnect is served instead of writing into a channel the previous
     * session already closed.
     */
    private fun startSession(session: ServedSession) {
        // One transport per connection at a time. A second enable while a session is running is the
        // same client reconfiguring, not a new peer.
        if (session.inbound != null) return

        val currentServer = server ?: return
        val characteristic = dataCharacteristic ?: return

        val inbound = Channel<ByteArray>(Channel.BUFFERED)
        session.inbound = inbound
        session.preparedWrites.clear()
        Log.d(LogTag, "Serving a session from ${session.device.address}")
        val queued = incoming.trySend(
            ServerBleTransport(
                server = currentServer,
                characteristic = characteristic,
                session = session,
                inbound = inbound
            )
        )
        if (queued.isFailure) {
            if (session.inbound === inbound) session.inbound = null
            inbound.close()
        }
    }

    @SuppressLint("MissingPermission")
    private fun serverCallback() = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            val remote = device ?: return
            if (newState != BluetoothProfile.STATE_CONNECTED) {
                served.remove(remote.address)?.let { session ->
                    session.inbound?.close(SyncTransportException.Disconnected("the peer disconnected"))
                    session.inbound = null
                }
                return
            }

            // One session per connection: the protocol is a sequence of messages, and interleaving
            // chunks from two peers into one stream would corrupt both assemblies.
            //
            // The transport itself is created when the peer subscribes, not here. A peer that closes
            // right after a session and reconnects can land back on the same server-side connection
            // before the stack reports the disconnect; keying the session to the subscription means
            // that reconnect still starts a fresh transport instead of writing into a closed channel.
            served[remote.address] = ServedSession(remote)
        }

        /**
         * API 36 reshaped this callback to carry the written bytes and their offset.
         *
         * The five-argument overload below is not an override on this SDK, but it has the same JVM
         * signature as the method the framework calls on releases before 36, so declaring it is what
         * keeps the server working on those.
         */
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            deliverWrite(
                device = device,
                requestId = requestId,
                characteristic = characteristic,
                preparedWrite = preparedWrite,
                offset = offset,
                value = value,
                responseNeeded = responseNeeded
            )
        }

        @Suppress("unused")
        fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean
        ) {
            deliverWrite(
                device = device,
                requestId = requestId,
                characteristic = characteristic,
                preparedWrite = preparedWrite,
                offset = 0,
                value = characteristic?.value,
                responseNeeded = responseNeeded
            )
        }

        private fun deliverWrite(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            offset: Int,
            value: ByteArray?,
            responseNeeded: Boolean
        ) {
            val session = device?.address?.let(served::get)
            if (session == null || characteristic?.uuid != SyncService.Data) {
                if (responseNeeded) respond(device, requestId, BluetoothGatt.GATT_FAILURE)
                return
            }

            if (preparedWrite) {
                // A write longer than the negotiated MTU arrives as prepare/execute. Buffering it
                // until onExecuteWrite keeps a single session message from being split into several
                // malformed chunks.
                session.preparedWrites
                    .getOrPut(characteristic.uuid) { mutableMapOf() }[offset] = value ?: ByteArray(0)
            } else {
                session.inbound?.trySend(value ?: ByteArray(0))
            }
            if (responseNeeded) respond(device, requestId, BluetoothGatt.GATT_SUCCESS)
        }

        override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
            val session = device?.address?.let(served::get)
            if (session == null) {
                respond(device, requestId, BluetoothGatt.GATT_FAILURE)
                return
            }

            if (execute) {
                session.preparedWrites.remove(SyncService.Data)?.let { chunks ->
                    val assembled = assemblePreparedWrite(chunks)
                    if (assembled.isNotEmpty()) session.inbound?.trySend(assembled)
                }
            } else {
                session.preparedWrites.clear()
            }
            respond(device, requestId, BluetoothGatt.GATT_SUCCESS)
        }

        /**
         * Answers the client's notification subscription.
         *
         * The default [BluetoothGattServerCallback] implementation does not respond, which leaves
         * the peer's `writeDescriptor` pending until its connection timeout. That used to look like
         * a peer that connected and then never sent a single session message.
         */
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            val isOurDescriptor = descriptor?.uuid == SyncService.ClientConfig &&
                descriptor.characteristic?.uuid == SyncService.Data
            val remote = device
            // Stacking a subscription on a connection whose disconnect has not been reported yet
            // can leave the session map empty. The descriptor is ours, so recreating the session
            // here is safe and keeps the peer from being rejected for a bookkeeping gap.
            val session = if (isOurDescriptor && remote != null) {
                served.getOrPut(remote.address) { ServedSession(remote) }
            } else {
                null
            }

            if (session != null && !preparedWrite) {
                session.notificationsEnabled = value?.contentEquals(NotificationsEnabled) == true
                if (session.notificationsEnabled) startSession(session)
            }
            Log.d(
                LogTag,
                "Notification subscription from ${device?.address}: valid=${session != null}, " +
                    "enabled=${session?.notificationsEnabled}"
            )
            if (responseNeeded) {
                respond(
                    device = device,
                    requestId = requestId,
                    status = if (session != null) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_FAILURE
                )
            }
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor?
        ) {
            val valid = descriptor?.uuid == SyncService.ClientConfig &&
                descriptor.characteristic?.uuid == SyncService.Data
            respond(
                device = device,
                requestId = requestId,
                status = if (valid) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_FAILURE,
                value = if (valid) NotificationsEnabled else null
            )
        }

        private fun assemblePreparedWrite(chunks: Map<Int, ByteArray>): ByteArray {
            val sorted = chunks.toSortedMap()
            val size = sorted.entries.maxOfOrNull { (offset, bytes) -> offset + bytes.size } ?: 0
            return ByteArray(size).also { assembled ->
                sorted.forEach { (offset, bytes) -> bytes.copyInto(assembled, offset) }
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val current = descriptor
            if (characteristic?.uuid != SyncService.Descriptor || current == null) {
                respond(device, requestId, BluetoothGatt.GATT_FAILURE)
                return
            }
            val payload = DescriptorCodec.encode(current)
            if (offset >= payload.size) {
                respond(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET)
                return
            }
            respond(device, requestId, BluetoothGatt.GATT_SUCCESS, payload.copyOfRange(offset, payload.size))
        }
    }

    @SuppressLint("MissingPermission")
    private fun respond(
        device: BluetoothDevice?,
        requestId: Int,
        status: Int,
        value: ByteArray? = null
    ) {
        val current = server ?: return
        val remote = device ?: return
        runCatching { current.sendResponse(remote, requestId, status, 0, value) }
    }

    private fun advertiseSettings() = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        .setConnectable(true)
        // Capped rather than left running: the sync screen is not a foreground service, so an endless
        // advertisement would keep the radio busy after the app is backgrounded.
        .setTimeout(AdvertisingTimeoutMillis)
        .build()

    /** Small enough for one legacy advertisement packet; 128-bit service UUIDs take 18 bytes. */
    private fun advertiseData() = AdvertiseData.Builder()
        .setIncludeDeviceName(false)
        .setIncludeTxPowerLevel(false)
        .addServiceUuid(ParcelUuid(SyncService.Uuid))
        .build()

    /** Fits the scan response exactly: 2 bytes of AD header, 2 of company id, 27 of descriptor. */
    private fun advertiseScanResponse(descriptor: DeviceDescriptor) = AdvertiseData.Builder()
        .setIncludeDeviceName(false)
        .setIncludeTxPowerLevel(false)
        .addManufacturerData(DescriptorCodec.CompanyId, DescriptorCodec.encode(descriptor))
        .build()

    private fun advertiseCallback() = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            Log.e(LogTag, "BLE advertising failed with error code $errorCode")
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(LogTag, "BLE advertising started")
        }
    }

    private companion object {
        const val LogTag = "BalanceTrackerSync"

        /**
         * Long enough for both people to open the screen and pick each other, short enough that a
         * forgotten screen cannot leave the radio on indefinitely. The sync manager stops the link
         * when the screen is left and no session is running, so this is the backstop rather than the
         * usual lifetime.
         *
         * Android's builder rejects anything above 180_000 with an exception. The exception used to
         * be swallowed, which left the radio completely silent while the UI said it was looking.
         */
        const val AdvertisingTimeoutMillis = 180_000
    }
}

/** Per-connection buffers for a session this device is serving. */
private class ServedSession(val device: BluetoothDevice) {
    /**
     * Chunks of the session currently being served, or null between sessions.
     *
     * Replaced for every subscription rather than fixed at connection time: a quick reconnect can
     * reuse the connection, and the previous session's channel is already closed.
     */
    @Volatile
    var inbound: Channel<ByteArray>? = null

    /** Chunks of a write that is too long for one ATT packet, keyed by their offset. */
    val preparedWrites = mutableMapOf<UUID, MutableMap<Int, ByteArray>>()

    /** Whether the peer has written the notification-enable value to the CCCD. */
    @Volatile
    var notificationsEnabled: Boolean = false
}

/**
 * A transport to a device that connected to us.
 *
 * Notifications are unacknowledged, so writes are serialised and paced: the Android stack queues them
 * and drops the overflow, which would lose chunks without anything surfacing here.
 */
@SuppressLint("MissingPermission")
private class ServerBleTransport(
    private val server: BluetoothGattServer,
    /** The characteristic registered with the server, which notifications have to be sent on. */
    private val characteristic: BluetoothGattCharacteristic,
    private val session: ServedSession,
    private val inbound: Channel<ByteArray>
) : SyncTransport {
    private val writeMutex = Mutex()

    override val maxChunkSize: Int = ServerChunkSize

    override suspend fun open() = Unit

    override suspend fun write(chunk: ByteArray) {
        writeMutex.withLock {
            if (!session.notificationsEnabled) {
                throw SyncTransportException.Failed("the peer did not subscribe to notifications")
            }

            val sent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                server.notifyCharacteristicChanged(session.device, characteristic, false, chunk)
            } else {
                @Suppress("DEPRECATION")
                run {
                    characteristic.value = chunk
                    server.notifyCharacteristicChanged(session.device, characteristic, false)
                }
            }

            if (sent != BluetoothStatusCodes.SUCCESS) {
                throw SyncTransportException.Failed("the peer stopped accepting data")
            }
            // Longer than one connection interval, so the peer's controller drains each notification
            // before the next one is queued.
            delay(NotificationIntervalMillis)
        }
    }

    override suspend fun read(): ByteArray = inbound.receive()
        ?: throw SyncTransportException.Disconnected("the peer disconnected")

    override suspend fun close() {
        // Cleared before closing so a peer that reconnects immediately can start the next session;
        // the identity check keeps a late close from detaching a newer transport.
        if (session.inbound === inbound) session.inbound = null
        runCatching { server.cancelConnection(session.device) }
        inbound.close()
    }

    private companion object {
        const val NotificationIntervalMillis = 30L
    }
}

/**
 * A transport to a device we connected to.
 *
 * The connecting side negotiates the MTU, so it can carry larger chunks than the serving side, which has
 * to assume the default until the peer says otherwise.
 */
@SuppressLint("MissingPermission")
private class ClientBleTransport(
    private val context: Context,
    private val remote: BluetoothDevice
) : SyncTransport {
    private val writeMutex = Mutex()
    private val inbound = Channel<ByteArray>(Channel.BUFFERED)
    private val connected = CompletableDeferred<Unit>()
    private val negotiatedMtu = CompletableDeferred<Int>()

    /** The in-flight write, so a write completes only once the stack reports its status. */
    @Volatile
    private var pendingWrite: CompletableDeferred<Int>? = null

    private var gatt: BluetoothGatt? = null

    override var maxChunkSize: Int = DefaultMtu - GattOverhead
        private set

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        connected.completeExceptionally(
                            SyncTransportException.Failed("the connection failed with status $status")
                        )
                    } else if (!gatt.discoverServices()) {
                        connected.completeExceptionally(
                            SyncTransportException.Failed("the sync service could not be discovered")
                        )
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (!connected.isCompleted) {
                        connected.completeExceptionally(
                            SyncTransportException.Failed("the connection dropped while connecting")
                        )
                    }
                    inbound.close(SyncTransportException.Disconnected("the peer disconnected"))
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connected.completeExceptionally(
                    SyncTransportException.Failed("service discovery failed with status $status")
                )
                return
            }

            val data = gatt.getService(SyncService.Uuid)?.getCharacteristic(SyncService.Data)
            if (data == null) {
                connected.completeExceptionally(
                    SyncTransportException.Failed("that device does not offer the sync service")
                )
                return
            }

            // Subscribing first, so notifications cannot arrive before there is somewhere to buffer
            // them; the write descriptor completing is also what finishes connecting.
            if (!gatt.setCharacteristicNotification(data, true)) {
                connected.completeExceptionally(
                    SyncTransportException.Failed("notifications could not be enabled")
                )
                return
            }

            val descriptor = data.getDescriptor(SyncService.ClientConfig)
            if (descriptor == null) {
                connected.completeExceptionally(
                    SyncTransportException.Failed("that device does not offer the sync descriptor")
                )
                return
            }

            val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, NotificationsEnabled) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                run {
                    descriptor.value = NotificationsEnabled
                    gatt.writeDescriptor(descriptor)
                }
            }
            if (!started) {
                connected.completeExceptionally(
                    SyncTransportException.Failed("notifications could not be subscribed")
                )
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connected.completeExceptionally(
                    SyncTransportException.Failed("notification subscription failed with status $status")
                )
                return
            }

            connected.complete(Unit)
            Log.d("BalanceTrackerSync", "Notification subscription complete, requesting MTU")

            // Ask for a larger MTU before the session starts. If the request is refused, the
            // guaranteed default still works: only the chunk size has to stay at its minimum.
            if (!gatt.requestMtu(RequestedMtu)) {
                maxChunkSize = DefaultMtu - GattOverhead
                negotiatedMtu.complete(DefaultMtu)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val usable = if (status == BluetoothGatt.GATT_SUCCESS) {
                mtu.coerceIn(DefaultMtu, RequestedMtu)
            } else {
                DefaultMtu
            }
            maxChunkSize = usable - GattOverhead
            Log.d("BalanceTrackerSync", "MTU negotiated: $usable, chunk=${maxChunkSize}")
            negotiatedMtu.complete(usable)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            characteristic.value?.let { inbound.trySend(it) }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            pendingWrite?.complete(status)
        }
    }

    suspend fun connect() {
        open()
    }

    /**
     * Connects, discovers the service and subscribes, so callers can treat the returned transport as
     * ready to use.
     *
     * Idempotent: [SyncLink.connectTo] opens it to report reachability, and the session opens it again
     * before exchanging anything.
     */
    override suspend fun open() {
        // connectTo has usually already opened it; opening twice would drop the first connection on
        // the floor, and the session opens the transport again before exchanging anything.
        if (gatt != null) return

        gatt = remote.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)

        runCatching {
            withTimeout(ConnectionTimeoutMillis) { connected.await() }
            // The MTU is an optimisation, not a requirement. If the stack never reports it, the
            // default-sized chunks are still valid.
            withTimeoutOrNull(MtuNegotiationTimeoutMillis) { negotiatedMtu.await() }
        }.onFailure {
            close()
            throw if (it is SyncTransportException) {
                it
            } else {
                SyncTransportException.Failed(it.message ?: "could not connect to that device")
            }
        }
    }

    /**
     * Writes are serialised with only one in flight: BLE gives no ordering or completion guarantee
     * across concurrent characteristic writes, and interleaved chunks would be silently dropped by the
     * framing layer.
     */
    override suspend fun write(chunk: ByteArray) {
        val current = gatt ?: throw SyncTransportException.Failed("transport is not open")
        val data = current.getService(SyncService.Uuid)?.getCharacteristic(SyncService.Data)
            ?: throw SyncTransportException.Failed("transport is not open")

        writeMutex.withLock {
            val result = CompletableDeferred<Int>()
            pendingWrite = result
            try {
                val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    current.writeCharacteristic(
                        data,
                        chunk,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    ) == BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    run {
                        data.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        data.value = chunk
                        current.writeCharacteristic(data)
                    }
                }

                if (!started) {
                    throw SyncTransportException.Failed("the peer rejected a write")
                }

                val status = try {
                    withTimeout(WriteTimeoutMillis) { result.await() }
                } catch (timeout: TimeoutCancellationException) {
                    throw SyncTransportException.Failed("the peer did not acknowledge a write")
                }
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    throw SyncTransportException.Failed("the peer rejected a write with status $status")
                }
            } finally {
                pendingWrite = null
            }
        }
    }

    override suspend fun read(): ByteArray = inbound.receive()
        ?: throw SyncTransportException.Disconnected("the peer disconnected")

    override suspend fun close() {
        val current = gatt
        gatt = null
        runCatching {
            current?.disconnect()
            current?.close()
        }
        inbound.close()
    }
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/** CCCD value that turns notifications on. */
private val NotificationsEnabled = byteArrayOf(1, 0)

/** API level that moved `BluetoothGattServer` creation from the adapter to the manager. */
private const val GattServerMovedToManagerApi = 36

private const val ConnectionTimeoutMillis = 15_000L
private const val MtuNegotiationTimeoutMillis = 2_000L
private const val WriteTimeoutMillis = 10_000L