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
        .getTransaction(id = id) { id, amount, dateTime, type, category, description ->
            TransactionDomainModel(
                id = id,
                amount = amount,
                dateTime = dateTime,
                type = type,
                category = category,
                description = description
            )
        }

    internal fun getTransactions() = transactionQueries
        .getTransactions { id, amount, dateTime, type, category, description ->
            TransactionDomainModel(
                id = id,
                amount = amount,
                dateTime = dateTime,
                type = type,
                category = category,
                description = description
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
        transactionQueries.editTransaction(
            amount = amount,
            dateTime = dateTime,
            type = type,
            category = category,
            description = description,
            version = nextVersion(),
            id = id
        )
    }

    /**
     * Soft delete. The row stays with a bumped version so the deletion can win a merge on the other
     * device; a hard DELETE would let a stale copy from a not-yet-synced device resurrect it.
     */
    internal suspend fun deleteTransaction(id: Long): Long = database.transactionWithResult {
        transactionQueries.softDeleteTransaction(id = id, version = nextVersion())
    }

    internal suspend fun exportSnapshot(): SyncSnapshot = database.transactionWithResult {
        SyncSnapshot(
            deviceId = ensureDeviceId(),
            transactions = transactionQueries.getAllSyncedTransactions { _, amount, dateTime, type, category, description, syncId, version, originId, isDeleted ->
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
        )
    }

    internal suspend fun importSnapshot(remote: SyncSnapshot): SyncImportResult =
        database.transactionWithResult {
            if (remote.deviceId == ensureDeviceId()) return@transactionWithResult SyncImportResult.SameDevice

            var inserted = 0
            var updated = 0
            var unchanged = 0
            var highestRemoteVersion = 0L

            remote.transactions.forEach { incoming ->
                val local = transactionQueries.getTransactionBySyncId(syncId = incoming.syncId)
                    .executeAsOneOrNull()
                    ?.toSyncedTransaction()

                when {
                    local == null -> {
                        transactionQueries.insertTransaction(
                            amount = incoming.amount,
                            dateTime = incoming.dateTime,
                            type = incoming.type,
                            category = incoming.category,
                            description = incoming.description,
                            syncId = incoming.syncId,
                            version = incoming.version,
                            originId = incoming.originId,
                            isDeleted = incoming.isDeleted.asFlag()
                        )
                        inserted++
                    }

                    SyncedTransactionComparator.supersedes(candidate = incoming, current = local) -> {
                        transactionQueries.updateSyncedTransaction(
                            amount = incoming.amount,
                            dateTime = incoming.dateTime,
                            type = incoming.type,
                            category = incoming.category,
                            description = incoming.description,
                            version = incoming.version,
                            originId = incoming.originId,
                            isDeleted = incoming.isDeleted.asFlag(),
                            syncId = incoming.syncId
                        )
                        updated++
                    }

                    else -> unchanged++
                }

                if (incoming.version > highestRemoteVersion) highestRemoteVersion = incoming.version
            }

            // Keep our counter ahead of everything we have ever observed, otherwise the next local
            // edit could be assigned a version that loses to a record we just accepted.
            if (highestRemoteVersion > currentLamport()) {
                deviceQueries.setLamport(lamport = highestRemoteVersion)
            }

            SyncImportResult.Merged(
                MergeStats(inserted = inserted, updated = updated, unchanged = unchanged)
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