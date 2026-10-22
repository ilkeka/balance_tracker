package me.ilker.sync.transport

// CoreBluetooth is not implemented yet. iOS therefore uses the same out-of-band sync code as the
// desktop and web builds rather than pretending a live link exists.
internal actual fun isLiveSyncSupported(): Boolean = false

private const val Reason = "iOS has no live sync link yet; use the sync code instead"

internal actual fun createSyncLink(): SyncLink = UnsupportedSyncLink(Reason)
