package me.ilker.balance_tracker.sync

import kotlinx.serialization.Serializable

/**
 * The device this installation is paired with.
 *
 * Stored so the sync screen can name it, and so a session can tell a known peer from a stranger that
 * happens to be running the app nearby. [fingerprint] is the public half of the pairing secret: two
 * devices that paired with each other show the same value, which is what lets both users confirm that
 * an approval prompt refers to the device in their hand.
 */
@Serializable
data class LinkedDevice(
    val deviceId: String,
    val alias: String,
    val fingerprint: String
) {
    companion object {
        /** Shown for the peer until it has been named, and for this device before it is. */
        const val DefaultAlias: String = "My device"
    }
}