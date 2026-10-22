package me.ilker.balance_tracker.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import me.ilker.balance_tracker.Database
import me.ilker.balance_tracker.Pairing
import me.ilker.balance_tracker.Transactions
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sync.LinkedDevice
import me.ilker.balance_tracker.sync.MergeStats
import me.ilker.balance_tracker.sync.SyncImportResult
import me.ilker.balance_tracker.sync.SyncSnapshot
import me.ilker.balance_tracker.sync.SyncedTransaction
import me.ilker.balance_tracker.sync.SyncedTransactionComparator
import me.ilker.balance_tracker.sync.generateSyncId

/**
 * What a snapshot is for.
 *
 * [Live] is the device-to-device exchange: each phone contributes only the rows it created.
 * [Backup] is the manual export/import document: it contains the whole local ledger, including
 * rows received from the peer.
 */
internal enum class SnapshotMode {
    Live,
    Backup
}

internal class DB(driver: SqlDriver) {
    private val database = Database(
        driver = driver,
        TransactionsAdapter = Transactions.Adapter(
            typeAdapter = EnumColumnAdapter(),
            categoryAdapter = object : ColumnAdapter<TransactionCategory, String> {
                override fun decode(databaseValue: String): TransactionCategory = TransactionCategory
                    .Predefined
                    .entries
                    .find { it.value == databaseValue }
                    ?: TransactionCategory.Custom(databaseValue)

                override fun encode(value: TransactionCategory): String = value.value

            }
        )
    )
    private val transactionQueries = database.transactionQueries
    private val deviceQueries = database.deviceQueries
    private val pairingQueries = database.pairingQueries
    private var cachedDeviceId: String? = null

    /**
     * Stable id for this installation, persisted on first use so the tiebreak in
     * [SyncedTransactionComparator] refers to the same device across restarts.
     */
    internal suspend fun deviceId(): String = cachedDeviceId
        ?: database.transactionWithResult { ensureDeviceId() }

    /** Name this device publishes to nearby peers, empty until the user picks one. */
    internal suspend fun deviceAlias(): String = deviceQueries.getDevice().executeAsOneOrNull()?.alias.orEmpty()

    internal suspend fun saveDeviceAlias(alias: String) {
        // Seeding first: the alias lives on the singleton row, so an update before the first deviceId
        // read would match nothing and be silently lost.
        database.transactionWithResult<Unit> {
            ensureDeviceId()
            deviceQueries.setAlias(alias = alias)
        }
    }

    internal suspend fun pairing(): Pairing? = pairingQueries.getPairing().executeAsOneOrNull()

    internal suspend fun savePairing(secret: String, linkedDevice: LinkedDevice?) {
        pairingQueries.upsertPairing(
            secret = secret,
            peerDeviceId = linkedDevice?.deviceId,
            peerAlias = linkedDevice?.alias.orEmpty(),
            peerFingerprint = linkedDevice?.fingerprint.orEmpty()
        )
    }

    internal suspend fun clearPairing() {
        pairingQueries.clearPairing()
    }

    internal fun getTransactionById(id: Long) = transactionQueries
        .getTransaction(id = id) { id, amount, dateTime, type, category, description, originId ->
            TransactionDomainModel(
                id = id,
                amount = amount,
                dateTime = dateTime,
                type = type,
                category = category,
                description = description,
                originId = originId
            )
        }

    internal fun getTransactions() = transactionQueries
        .getTransactions { id, amount, dateTime, type, category, description, originId ->
            TransactionDomainModel(
                id = id,
                amount = amount,
                dateTime = dateTime,
                type = type,
                category = category,
                description = description,
                originId = originId
            )
        }

    internal suspend fun addTransaction(
        amount: Double,
        dateTime: String,
        type: TransactionType,
        category: TransactionCategory,
        description: String?
    ): Long = database.transactionWithResult {
        val originId = ensureDeviceId()
        transactionQueries.insertTransaction(
            amount = amount,
            dateTime = dateTime,
            type = type,
            category = category,
            description = description,
            syncId = generateSyncId(),
            version = nextVersion(),
            originId = originId,
            isDeleted = 0
        )
    }

    internal suspend fun editTransaction(
        id: Long,
        amount: Double,
        dateTime: String,
        type: TransactionType,
        category: TransactionCategory,
        description: String?
    ): Long = database.transactionWithResult {
        // Only the device that created a row may change it. The WHERE clause makes that a data rule,
        // not just a hidden button in the UI.
        transactionQueries.editTransaction(
            amount = amount,
            dateTime = dateTime,
            type = type,
            category = category,
            description = description,
            version = nextVersion(),
            originId = ensureDeviceId(),
            id = id
        )
    }

    /**
     * Soft delete. The row stays with a bumped version so the deletion can win a merge on the other
     * device; a hard DELETE would let a stale copy from a not-yet-synced device resurrect it.
     */
    internal suspend fun deleteTransaction(id: Long): Long = database.transactionWithResult {
        transactionQueries.softDeleteTransaction(
            id = id,
            version = nextVersion(),
            originId = ensureDeviceId()
        )
    }

    internal suspend fun exportSnapshot(mode: SnapshotMode = SnapshotMode.Backup): SyncSnapshot =
        database.transactionWithResult {
            val localDeviceId = ensureDeviceId()
            val transactions = transactionQueries.getAllSyncedTransactions { _, amount, dateTime, type, category, description, syncId, version, originId, isDeleted ->
                SyncedTransaction(
                    syncId = syncId,
                    amount = amount,
                    dateTime = dateTime,
                    type = type,
                    category = category,
                    description = description,
                    version = version,
                    originId = originId,
                    isDeleted = isDeleted != 0L
                )
            }.executeAsList()
                .filter { mode == SnapshotMode.Backup || it.originId == localDeviceId }

            SyncSnapshot(deviceId = localDeviceId, transactions = transactions)
        }

    internal suspend fun importSnapshot(
        remote: SyncSnapshot,
        mode: SnapshotMode = SnapshotMode.Backup
    ): SyncImportResult = database.transactionWithResult {
        val localDeviceId = ensureDeviceId()
        if (mode == SnapshotMode.Live && remote.deviceId == localDeviceId) {
            return@transactionWithResult SyncImportResult.SameDevice
        }

        val peerOnly = mode == SnapshotMode.Live
        val adoptBackupOwner = mode == SnapshotMode.Backup && remote.deviceId != localDeviceId

        var inserted = 0
        var updated = 0
        var unchanged = 0
        var ignored = 0
        var highestRemoteVersion = 0L

        remote.transactions.forEach { incoming ->
            // Rows from before origin tracking have no owner. In a snapshot they can only have been
            // created on the device that produced it, so attribute them to that device.
            val incomingOrigin = incoming.originId.ifBlank { remote.deviceId }
            val candidate = if (incomingOrigin == incoming.originId) {
                incoming
            } else {
                incoming.copy(originId = incomingOrigin)
            }

            val local = transactionQueries.getTransactionBySyncId(syncId = candidate.syncId)
                .executeAsOneOrNull()
                ?.toSyncedTransaction()

            if (peerOnly) {
                val peerOwnsRow = candidate.originId == remote.deviceId
                val localOwnedByPeer = local?.originId == remote.deviceId
                if (!peerOwnsRow || (local != null && !localOwnedByPeer)) {
                    // The sender is not the author: this is either one of our rows echoed back or
                    // a row from another device. It must not change local state.
                    ignored++
                    return@forEach
                }
            }

            val stored = if (adoptBackupOwner && candidate.originId == remote.deviceId) {
                // A backup restored on a new install keeps its rows editable by adopting them.
                candidate.copy(originId = localDeviceId)
            } else {
                candidate
            }

            val changesRow = when {
                local == null -> true
                adoptBackupOwner && stored.originId != local.originId -> true
                else -> SyncedTransactionComparator.supersedes(candidate = stored, current = local)
            }

            when {
                local == null -> {
                    transactionQueries.insertTransaction(
                        amount = stored.amount,
                        dateTime = stored.dateTime,
                        type = stored.type,
                        category = stored.category,
                        description = stored.description,
                        syncId = stored.syncId,
                        version = stored.version,
                        originId = stored.originId,
                        isDeleted = stored.isDeleted.asFlag()
                    )
                    inserted++
                }

                changesRow -> {
                    transactionQueries.updateSyncedTransaction(
                        amount = stored.amount,
                        dateTime = stored.dateTime,
                        type = stored.type,
                        category = stored.category,
                        description = stored.description,
                        version = stored.version,
                        originId = stored.originId,
                        isDeleted = stored.isDeleted.asFlag(),
                        syncId = stored.syncId
                    )
                    updated++
                }

                else -> unchanged++
            }

            if (stored.version > highestRemoteVersion) highestRemoteVersion = stored.version
        }

        // Keep our counter ahead of everything we have ever observed, otherwise the next local
        // edit could be assigned a version that loses to a record we just accepted.
        if (highestRemoteVersion > currentLamport()) {
            deviceQueries.setLamport(lamport = highestRemoteVersion)
        }

        SyncImportResult.Merged(
            MergeStats(
                inserted = inserted,
                updated = updated,
                unchanged = unchanged,
                ignored = ignored
            )
        )
    }

    private suspend fun currentLamport(): Long = deviceQueries.getDevice().executeAsOneOrNull()?.lamport ?: 0L

    /**
     * Reads the stored device id, seeding it on first use. Runs inside the caller's transaction so
     * the insert cannot race a concurrent first write.
     */
    private suspend fun ensureDeviceId(): String {
        cachedDeviceId?.let { return it }
        val resolved = deviceQueries.getDevice().executeAsOneOrNull()?.deviceId
            ?: generateSyncId().also { generated ->
                deviceQueries.insertDevice(deviceId = generated)
            }
        // Rows created before origin tracking belong to this installation. Filling them here keeps
        // them in the live snapshot without a destructive schema migration.
        transactionQueries.assignOriginIdToUnsetRows(originId = resolved)
        cachedDeviceId = resolved
        return resolved
    }

    /**
     * Allocates the next version for a local write.
     *
     * [ensureDeviceId] runs first because the counter lives in the `Device` row: without it the
     * seeding insert has not happened yet and the `setLamport` update would match no rows, leaving
     * the counter stuck at 0.
     */
    private suspend fun nextVersion(): Long {
        ensureDeviceId()
        val next = currentLamport() + 1
        deviceQueries.setLamport(lamport = next)
        return next
    }

    private fun Transactions.toSyncedTransaction() = SyncedTransaction(
        syncId = syncId,
        amount = amount,
        dateTime = dateTime,
        type = type,
        category = category,
        description = description,
        version = version,
        originId = originId,
        isDeleted = isDeleted != 0L
    )

    private fun Boolean.asFlag(): Long = if (this) 1L else 0L
}