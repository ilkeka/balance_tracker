package me.ilker.sync.protocol

import kotlinx.serialization.Serializable

/**
 * How a peer presents itself. Purely for display, as in the LocalSend protocol: nothing about the
 * exchange depends on it.
 */
enum class DeviceType {
    Mobile,
    Desktop,
    Web;

    /**
     * Single-byte discriminant for the advertisement, which has one byte to spend and cannot afford
     * the string. Written explicitly rather than derived from the enum name so that reordering or
     * renaming a value cannot silently change what older builds read.
     */
    val wireCode: Int
        get() = when (this) {
            Mobile -> 1
            Desktop -> 2
            Web -> 3
        }

    /** Stable lowercase wire value, so a future target does not break an older build's parsing. */
    val wireValue: String
        get() = when (this) {
            Mobile -> "mobile"
            Desktop -> "desktop"
            Web -> "web"
        }

    companion object {
        fun fromWire(value: String): DeviceType = when (value) {
            "mobile" -> Mobile
            "desktop" -> Desktop
            "web" -> Web
            // Unknown types fall back rather than failing the handshake: an older build must still be
            // able to sync with a newer one that added a platform.
            else -> Desktop
        }

        fun fromWireCode(code: Int): DeviceType = when (code) {
            1 -> Mobile
            2 -> Desktop
            3 -> Web
            // A code from a newer build is shown as a desktop rather than dropped: a wrong label on the
            // picker is much cheaper than a device the user cannot sync with at all.
            else -> Desktop
        }
    }
}

/**
 * Who a device is, carried in the BLE advertisement and re-checked over the connection.
 *
 * [fingerprint] is the stable public half of the pairing secret, published in clear so the UI can
 * show "Nice Orange · A1B2C3" and let the user confirm they picked the right device before approving
 * anything. The secret itself never leaves the devices that paired.
 */
@Serializable
internal data class DeviceDescriptor(
    val deviceId: String,
    val alias: String,
    val model: String?,
    val type: DeviceType,
    val fingerprint: String,
    val protocolVersion: Int = SyncPayloadCodec.Version
)

/**
 * Encodes a descriptor into the fixed-width advertisement payload.
 *
 * BLE manufacturer data is capped at 27 bytes on older controllers, so this is not a free-form blob.
 * The layout is positional rather than JSON for that reason, and [DescriptorCodec.MaxSize] is
 * asserted in tests so a longer alias cannot silently produce an advertisement that gets dropped.
 */
internal object DescriptorCodec {
    /** Company id assigned to this app, only used to namespace the advertisement. */
    const val CompanyId: Int = 0x4254

    const val MaxAliasLength: Int = 8
    const val MaxModelLength: Int = 6
    const val MaxSize: Int = 27

    /**
     * Layout, big-endian:
     * `[version:1][type:1][deviceId:8][fingerprint:3][model:6][alias:8]` = 27 bytes.
     * [deviceId] is 4 bytes and [fingerprint] is 3, so the text is written out in full at 8 and 6
     * characters respectively.
     *
     * The device id is truncated rather than hashed so the full id stays in the handshake where there
     * is room for it; the truncated copy is only used to tell two advertisers apart in the picker.
     */
    fun encode(descriptor: DeviceDescriptor): ByteArray {
        val payload = ByteArray(MaxSize)
        payload[0] = descriptor.protocolVersion.toByte()
        payload[1] = descriptor.type.wireCode.toByte()

        hexToByteArray(descriptor.deviceId.take(DeviceIdHexLength).lowercase())
            .copyInto(payload, destinationOffset = DeviceIdOffset)
        // Fingerprints are published uppercase so they are readable in the approval prompt, so the
        // hex decoder needs them back in its own case before this can run off a real device.
        hexToByteArray(descriptor.fingerprint.take(FingerprintHexLength).lowercase())
            .copyInto(payload, destinationOffset = FingerprintOffset)

        descriptor.model.orEmpty().take(MaxModelLength).encodeToByteArray()
            .copyInto(payload, destinationOffset = ModelOffset)
        descriptor.alias.take(MaxAliasLength).encodeToByteArray()
            .copyInto(payload, destinationOffset = AliasOffset)

        return payload
    }

    /**
     * Reads an advertisement payload, or returns null when it is not one of ours or is truncated.
     *
     * Returns null rather than throwing because this runs on whatever the radio picked up, and an
     * unrelated advertiser must be ignored instead of reported as an error.
     */
    fun decode(payload: ByteArray): DeviceDescriptor? {
        if (payload.size < MaxSize) return null
        if (payload[0].toInt() and 0xFF != SyncPayloadCodec.Version) return null

        val deviceId = payload.copyOfRange(DeviceIdOffset, DeviceIdOffset + DeviceIdBytes)
            .toHex().padEnd(DeviceIdHexLength, '0')
        val fingerprint = payload.copyOfRange(FingerprintOffset, FingerprintOffset + FingerprintBytes)
            .toHex().uppercase()

        return DeviceDescriptor(
            deviceId = deviceId,
            alias = payload.copyOfRange(AliasOffset, AliasOffset + MaxAliasLength)
                .decodeToString().trimEnd('\u0000', ' '),
            model = payload.copyOfRange(ModelOffset, ModelOffset + MaxModelLength)
                .decodeToString().trimEnd('\u0000', ' ')
                .takeIf { it.isNotBlank() },
            type = DeviceType.fromWireCode(payload[1].toInt()),
            fingerprint = fingerprint
        )
    }

    private const val DeviceIdBytes = 4
    private const val FingerprintBytes = 3
    private const val DeviceIdOffset = 2
    private const val FingerprintOffset = DeviceIdOffset + DeviceIdBytes
    private const val ModelOffset = FingerprintOffset + FingerprintBytes
    private const val AliasOffset = ModelOffset + MaxModelLength
    private const val DeviceIdHexLength = DeviceIdBytes * 2
    private const val FingerprintHexLength = FingerprintBytes * 2
}

/** Public half of the pairing secret: enough to recognise a device, not enough to authenticate as it. */
internal fun deviceFingerprint(secret: ByteArray): String = secret.copyOf(3).toHex().uppercase()
