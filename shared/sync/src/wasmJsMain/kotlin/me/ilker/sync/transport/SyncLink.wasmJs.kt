package me.ilker.sync.transport

internal actual fun isLiveSyncSupported(): Boolean = false

private const val Reason = "the browser has no nearby-device link; use the sync code instead"

internal actual fun createSyncLink(): SyncLink = UnsupportedSyncLink(Reason)
