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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import me.ilker.sync.protocol.DescriptorCodec
import me.ilker.sync.protocol.DeviceDescriptor

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
 * All four live in one object because they share the adapter and have to be torn down together. The sync
 * screen owns exactly one instance for as long as it is on screen.
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

        adapter.bluetoothLeAdvertiser?.startAdvertising(
            advertiseSettings(),
            advertiseData(descriptor),
            advertiseCallback()
        )
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
            session.inbound.close()
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
                found.close(SyncDiscoveryException.Unavailable(SyncUnavailableReason.BluetoothUnusable))
            }

            /** Anything that is not one of our advertisements, or a repeat of one, is dropped here. */
            private fun publish(result: ScanResult) {
                val address = result.device.address ?: return
                if (!seen.add(address)) return
                val bytes = result.scanRecord?.manufacturerSpecificData?.get(DescriptorCodec.CompanyId)
                    ?: return
                DescriptorCodec.decode(bytes)?.let { found.trySend(DiscoveredDevice(address, it)) }
            }
        }

        scanCallback = callback
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

    @SuppressLint("MissingPermission")
    private fun serverCallback() = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            val remote = device ?: return
            if (newState != BluetoothProfile.STATE_CONNECTED) {
                served.remove(remote.address)?.inbound?.close(
                    SyncTransportException.Disconnected("the peer disconnected")
                )
                return
            }

            // One session per connection: the protocol is a sequence of messages, and interleaving
            // chunks from two peers into one stream would corrupt both assemblies.
            val session = ServedSession(remote)
            served[remote.address] = session

            val currentServer = server ?: return
            val characteristic = dataCharacteristic ?: return
            incoming.trySend(
                ServerBleTransport(server = currentServer, characteristic = characteristic, session = session)
            )
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
            deliverWrite(device, requestId, characteristic, value, responseNeeded)
        }

        @Suppress("unused")
        fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean
        ) {
            deliverWrite(device, requestId, characteristic, characteristic?.value, responseNeeded)
        }

        private fun deliverWrite(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            value: ByteArray?,
            responseNeeded: Boolean
        ) {
            val session = device?.address?.let(served::get)
            if (session == null || characteristic?.uuid != SyncService.Data) {
                if (responseNeeded) respond(device, requestId, BluetoothGatt.GATT_FAILURE)
                return
            }
            session.inbound.trySend(value ?: ByteArray(0))
            if (responseNeeded) respond(device, requestId, BluetoothGatt.GATT_SUCCESS)
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

    private fun advertiseData(descriptor: DeviceDescriptor) = AdvertiseData.Builder()
        .setIncludeDeviceName(false)
        .setIncludeTxPowerLevel(false)
        .addServiceUuid(ParcelUuid(SyncService.Uuid))
        .addManufacturerData(DescriptorCodec.CompanyId, DescriptorCodec.encode(descriptor))
        .build()

    private fun advertiseCallback() = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) = Unit
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) = Unit
    }

    private companion object {
        const val AdvertisingTimeoutMillis = 30_000
    }
}

/** Per-connection buffers for a session this device is serving. */
private class ServedSession(val device: BluetoothDevice) {
    val inbound = Channel<ByteArray>(Channel.BUFFERED)
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
    private val session: ServedSession
) : SyncTransport {
    private val writeMutex = Mutex()

    override val maxChunkSize: Int = ServerChunkSize

    override suspend fun open() = Unit

    override suspend fun write(chunk: ByteArray) {
        writeMutex.withLock {
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

    override suspend fun read(): ByteArray = session.inbound.receive()
        ?: throw SyncTransportException.Disconnected("the peer disconnected")

    override suspend fun close() {
        runCatching { server.cancelConnection(session.device) }
        session.inbound.close()
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

    /** One rendezvous per in-flight write, so a write completes only once the stack acknowledges it. */
    private val writeAcknowledged = Channel<Unit>(Channel.RENDEZVOUS)

    private var gatt: BluetoothGatt? = null

    override var maxChunkSize: Int = DefaultMtu - GattOverhead
        private set

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> gatt.discoverServices()
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
            val data = gatt.getService(SyncService.Uuid)?.getCharacteristic(SyncService.Data)
            if (data == null) {
                connected.completeExceptionally(
                    SyncTransportException.Failed("that device does not offer the sync service")
                )
                return
            }

            // Subscribing first, so notifications cannot arrive before there is somewhere to buffer
            // them; the write descriptor completing is also what finishes connecting.
            gatt.setCharacteristicNotification(data, true)
            gatt.writeDescriptor(
                data.getDescriptor(SyncService.ClientConfig) ?: return,
                NotificationsEnabled
            )
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                maxChunkSize = RequestedMtu - GattOverhead
                negotiatedMtu.complete(RequestedMtu)
            } else {
                @Suppress("DEPRECATION")
                run { gatt.requestMtu(RequestedMtu) }
            }
            connected.complete(Unit)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val usable = mtu.coerceIn(DefaultMtu, RequestedMtu)
            maxChunkSize = usable - GattOverhead
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
            writeAcknowledged.trySend(Unit)
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
            negotiatedMtu.await()
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
            current.writeCharacteristic(data, chunk, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            try {
                withTimeout(WriteTimeoutMillis) { writeAcknowledged.receive() }
            } catch (timeout: TimeoutCancellationException) {
                throw SyncTransportException.Failed("the peer did not acknowledge a write")
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
private const val WriteTimeoutMillis = 10_000L