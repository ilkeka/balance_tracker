package me.ilker.sync.transport

import java.util.UUID

/**
 * Fixed 128-bit identifiers derived from the app id, so only builds of this app can find each other.
 *
 * [Data] is the one characteristic a session uses, and it is asymmetric by role rather than by name:
 * the connecting device writes to it and the serving device notifies on it. A single characteristic is
 * enough because a GATT link already has a direction per end.
 */
internal object SyncService {
    val Uuid: UUID = UUID.fromString("6f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8")

    /** The session pipe: written by the client, notified by the server. */
    val Data: UUID = UUID.fromString("6f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f9")

    /** Readable so a peer can confirm who it reached before committing to a session. */
    val Descriptor: UUID = UUID.fromString("6f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7fa")

    /** The standard "notify" client-characteristic-config descriptor. */
    val ClientConfig: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}

internal const val DefaultMtu = 23
internal const val RequestedMtu = 247
internal const val GattOverhead = 3

/**
 * Chunk size the serving side can rely on.
 *
 * Only the connecting side can negotiate an MTU, and a notification longer than the negotiated MTU is
 * dropped rather than fragmented, so the server stays at the guaranteed default. Being slower than
 * necessary costs little next to silently losing chunks.
 */
internal const val ServerChunkSize = DefaultMtu - GattOverhead
