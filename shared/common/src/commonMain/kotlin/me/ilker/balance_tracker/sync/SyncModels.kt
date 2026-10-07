package me.ilker.balance_tracker.sync

import kotlinx.serialization.Serializable
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionType

/**
 * A transaction plus the metadata required to merge it across devices.
 *
 * [id] is deliberately absent: it is a local rowid and means nothing on another device. [syncId] is
 * the cross-device identity, minted once when the record is created and never changed afterwards.
 */
@Serializable
data class SyncedTransaction(
    val syncId: String,
    val amount: Double,
    val dateTime: String,
    val type: TransactionType,
    val category: TransactionCategory,
    val description: String?,
    val version: Long,
    val originId: String,
    val isDeleted: Boolean
)

/**
 * The complete syncable state of one device.
 *
 * A snapshot is exchanged in full on every sync rather than as a delta. Transaction counts for a
 * personal ledger are small, and a full exchange needs no per-peer cursor to be persisted, which
 * removes a whole class of "what did I last send them" bookkeeping bugs.
 */
@Serializable
data class SyncSnapshot(
    val deviceId: String,
    val transactions: List<SyncedTransaction>
)

data class MergeStats(
    val inserted: Int,
    val updated: Int,
    val unchanged: Int
) {
    val applied: Int get() = inserted + updated
}

sealed interface SyncImportResult {
    data class Merged(val stats: MergeStats) : SyncImportResult

    /** The snapshot came from this very device, so there was nothing to merge. */
    data object SameDevice : SyncImportResult
}