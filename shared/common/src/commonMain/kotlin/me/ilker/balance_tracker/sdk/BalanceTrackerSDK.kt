package me.ilker.balance_tracker.sdk

import kotlinx.coroutines.flow.Flow
import me.ilker.balance_tracker.sync.LinkedDevice
import me.ilker.balance_tracker.sync.SyncImportResult
import me.ilker.balance_tracker.sync.SyncSnapshot

interface BalanceTrackerSDK {
    val transactions: Flow<List<TransactionDomainModel>>

    /** Stable id for this installation, used as the merge tiebreak and to identify a peer. */
    suspend fun deviceId(): String

    suspend fun getTransactionById(id: Long): TransactionDomainModel?

    @Throws(Exception::class)
    suspend fun getTransactions(): List<TransactionDomainModel>

    @Throws(Exception::class)
    suspend fun addTransaction(
        amount: Double,
        dateTime: String,
        type: TransactionType,
        category: TransactionCategory,
        description: String?
    ): Long

    @Throws(Exception::class)
    suspend fun editTransaction(
        id: Long,
        amount: Double,
        dateTime: String,
        type: TransactionType,
        category: TransactionCategory,
        description: String?
    ): Long

    suspend fun deleteTransaction(
        id: Long
    ): Long

    /**
     * The full syncable state of this device. Exchange is always full, in both directions, so there
     * is no per-peer cursor to persist.
     */
    suspend fun exportSyncSnapshot(): SyncSnapshot

    /**
     * Merges a snapshot received from a paired device. Records are combined per `syncId` rather
     * than replacing local state, so transactions the peer had never seen are kept.
     */
    @Throws(Exception::class)
    suspend fun importSyncSnapshot(snapshot: SyncSnapshot): SyncImportResult

    /** Stored shared secret for the paired device, or null when nothing is paired yet. */
    suspend fun getPairingSecret(): ByteArray?

    /**
     * The paired device as last seen, or null when nothing is paired yet. [fingerprint] lets both
     * users confirm an approval prompt refers to the device in their hand.
     */
    suspend fun getLinkedDevice(): LinkedDevice?

    suspend fun savePairingSecret(secret: ByteArray, linkedDevice: LinkedDevice?)

    suspend fun clearPairingSecret()

    /** Name this device publishes to nearby peers; empty until the user picks one. */
    suspend fun deviceAlias(): String

    suspend fun saveDeviceAlias(alias: String)
}