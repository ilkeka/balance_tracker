package me.ilker.balance_tracker.sync

/**
 * Decides whether an incoming record replaces a local one.
 *
 * Ordering is by [SyncedTransaction.version], a Lamport counter rather than a wall-clock timestamp.
 * Wall clocks on two phones drift, and NTP corrections move them backwards, so a timestamp-ordered
 * merge lets a device with a fast clock win every conflict permanently and can reorder genuinely
 * ordered edits. A Lamport counter is monotonic per device and combined with the merge below it
 * gives a total order, so both devices converge to the same result regardless of sync order.
 *
 * Ties are broken by [SyncedTransaction.originId], which is the id of the device that *created* the
 * record and never changes. Using a stable per-record value rather than "whoever edited last" is
 * what keeps the outcome order-independent: both devices evaluate the identical comparison.
 */
internal object SyncedTransactionComparator {
    fun supersedes(candidate: SyncedTransaction, current: SyncedTransaction): Boolean =
        candidate.version > current.version ||
            (candidate.version == current.version && candidate.originId > current.originId)
}