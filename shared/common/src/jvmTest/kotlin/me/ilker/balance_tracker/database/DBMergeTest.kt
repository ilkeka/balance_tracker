package me.ilker.balance_tracker.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import me.ilker.balance_tracker.Database
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sync.MergeStats
import me.ilker.balance_tracker.sync.SyncImportResult
import me.ilker.balance_tracker.sync.SyncSnapshot
import me.ilker.balance_tracker.sync.SyncedTransaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the real merge, including what a pure comparator test cannot reach: rowid stability,
 * soft-delete resurrection and Lamport advancement.
 */
class DBMergeTest {

    private fun inMemoryDb(): DB = DB(
        JdbcSqliteDriver(
            url = JdbcSqliteDriver.IN_MEMORY,
            schema = Database.Schema.synchronous()
        )
    )

    private fun remote(
        syncId: String,
        version: Long,
        originId: String,
        isDeleted: Boolean = false,
        amount: Double = 1.0
    ) = SyncedTransaction(
        syncId = syncId,
        amount = amount,
        dateTime = "01/01/2026",
        type = TransactionType.Expense,
        category = TransactionCategory.Predefined.Other,
        description = null,
        version = version,
        originId = originId,
        isDeleted = isDeleted
    )

    private fun snapshot(vararg transactions: SyncedTransaction) =
        SyncSnapshot(deviceId = "remote", transactions = transactions.toList())

    private suspend fun addSample(db: DB, dateTime: String = "01/01/2026"): Long = db.addTransaction(
        amount = 5.0,
        dateTime = dateTime,
        type = TransactionType.Expense,
        category = TransactionCategory.Predefined.Other,
        description = null
    )

    private suspend fun merge(
        db: DB,
        snapshot: SyncSnapshot,
        mode: SnapshotMode = SnapshotMode.Live
    ): MergeStats =
        assertIs<SyncImportResult.Merged>(db.importSnapshot(snapshot, mode)).stats

    @Test
    fun importingASnapshotInsertsUnknownRecords() = runTest {
        val db = inMemoryDb()

        val stats = merge(db, snapshot(remote(syncId = "a", version = 1, originId = "remote")))

        assertEquals(1, stats.inserted)
        assertEquals(1, db.getTransactions().executeAsList().size)
    }

    @Test
    fun importingTheSameSnapshotTwiceIsIdempotent() = runTest {
        val db = inMemoryDb()
        val firstSync = snapshot(remote(syncId = "a", version = 1, originId = "remote"))

        merge(db, firstSync)
        val second = merge(db, firstSync)

        assertEquals(0, second.applied)
        assertEquals(1, second.unchanged)
        assertEquals(1, db.getTransactions().executeAsList().size)
    }

    @Test
    fun aDeleteFromThePeerIsApplied() = runTest {
        val db = inMemoryDb()
        merge(db, snapshot(remote(syncId = "a", version = 1, originId = "remote")))

        val stats = merge(db, snapshot(remote(syncId = "a", version = 2, originId = "remote", isDeleted = true)))

        assertEquals(1, stats.updated)
        assertTrue(db.getTransactions().executeAsList().isEmpty())
        assertTrue(db.exportSnapshot().transactions.single().isDeleted)
    }

    /**
     * The bug that motivates tombstones: a peer that has not yet seen the delete pushes its stale
     * live copy back and the transaction reappears.
     */
    @Test
    fun aStaleLiveCopyCannotResurrectADeletedRecord() = runTest {
        val db = inMemoryDb()
        merge(db, snapshot(remote(syncId = "a", version = 1, originId = "remote")))
        merge(db, snapshot(remote(syncId = "a", version = 5, originId = "remote", isDeleted = true)))

        val stats = merge(db, snapshot(remote(syncId = "a", version = 1, originId = "remote")))

        assertEquals(0, stats.applied)
        assertTrue(db.getTransactions().executeAsList().isEmpty())
    }

    @Test
    fun mergingPreservesTheLocalRowId() = runTest {
        val db = inMemoryDb()
        merge(db, snapshot(remote(syncId = "a", version = 1, originId = "remote", amount = 1.0)))
        val id = db.getTransactions().executeAsList().single().id

        merge(db, snapshot(remote(syncId = "a", version = 2, originId = "remote", amount = 42.0)))

        val merged = db.getTransactionById(id = id).executeAsOneOrNull()
        assertEquals(id, merged?.id)
        assertEquals(42.0, merged?.amount)
    }

    /**
     * A local edit after accepting a high-version remote record must outrank that record, otherwise
     * the next merge silently undoes the user's change.
     */
    @Test
    fun localEditsOutrankImportedRecords() = runTest {
        val db = inMemoryDb()
        merge(db, snapshot(remote(syncId = "a", version = 500, originId = "remote")))

        addSample(db)

        assertTrue(db.exportSnapshot().transactions.any { it.version > 500 })
    }

    @Test
    fun importingOwnSnapshotIsRejected() = runTest {
        val db = inMemoryDb()
        addSample(db)

        assertIs<SyncImportResult.SameDevice>(
            db.importSnapshot(db.exportSnapshot(SnapshotMode.Live), SnapshotMode.Live)
        )
    }

    @Test
    fun softDeletedRecordsAreExcludedFromReadsButKeptForSync() = runTest {
        val db = inMemoryDb()
        val id = addSample(db)

        db.deleteTransaction(id = id)

        assertNull(db.getTransactionById(id = id).executeAsOneOrNull())
        assertTrue(db.getTransactions().executeAsList().isEmpty())
        assertTrue(db.exportSnapshot().transactions.single().isDeleted)
    }

    @Test
    fun editsAdvanceTheVersionSoLaterSyncsWin() = runTest {
        val db = inMemoryDb()
        val id = addSample(db)
        val initial = db.exportSnapshot().transactions.single()

        db.editTransaction(
            id = id,
            amount = 9.0,
            dateTime = "02/02/2026",
            type = TransactionType.Expense,
            category = TransactionCategory.Predefined.Grocery,
            description = "updated"
        )

        val edited = db.exportSnapshot().transactions.single()
        assertTrue(edited.version > initial.version)
        assertEquals(9.0, edited.amount)
    }

    @Test
    fun liveExportContainsOnlyThisDevicesTransactions() = runTest {
        val db = inMemoryDb()
        addSample(db)
        merge(db, snapshot(remote(syncId = "remote-1", version = 1, originId = "remote")))

        val live = db.exportSnapshot(SnapshotMode.Live)
        assertEquals(1, live.transactions.size)
        assertEquals(db.deviceId(), live.transactions.single().originId)

        val backup = db.exportSnapshot(SnapshotMode.Backup)
        assertEquals(2, backup.transactions.size)
    }

    @Test
    fun liveImportIgnoresRowsThePeerDoesNotOwn() = runTest {
        val db = inMemoryDb()
        val localId = db.deviceId()

        val stats = merge(
            db,
            snapshot(
                remote(syncId = "remote-1", version = 1, originId = "remote"),
                remote(syncId = "echo", version = 1, originId = localId),
                remote(syncId = "third", version = 1, originId = "third")
            )
        )

        assertEquals(1, stats.inserted)
        assertEquals(2, stats.ignored)
        assertEquals(1, db.getTransactions().executeAsList().size)
    }

    @Test
    fun aPeerCannotOverwriteALocallyOwnedRow() = runTest {
        val db = inMemoryDb()
        val id = addSample(db)
        val local = db.exportSnapshot(SnapshotMode.Live).transactions.single()

        val stats = merge(
            db,
            snapshot(
                remote(
                    syncId = local.syncId,
                    version = local.version + 10,
                    originId = local.originId,
                    amount = 99.0
                )
            )
        )

        assertEquals(0, stats.applied)
        assertEquals(1, stats.ignored)
        assertEquals(5.0, db.getTransactionById(id = id).executeAsOne().amount)
    }

    @Test
    fun foreignRowsCannotBeEditedOrDeleted() = runTest {
        val db = inMemoryDb()
        merge(db, snapshot(remote(syncId = "remote-1", version = 1, originId = "remote")))
        val id = db.getTransactions().executeAsList().single().id

        val edited = db.editTransaction(
            id = id,
            amount = 42.0,
            dateTime = "02/02/2026",
            type = TransactionType.Expense,
            category = TransactionCategory.Predefined.Other,
            description = null
        )
        assertEquals(0L, edited)
        assertEquals(1.0, db.getTransactionById(id = id).executeAsOne().amount)

        val deleted = db.deleteTransaction(id = id)
        assertEquals(0L, deleted)
        assertEquals(1, db.getTransactions().executeAsList().size)
    }

    @Test
    fun legacyRowsFromThePeerAreAttributedToThePeer() = runTest {
        val db = inMemoryDb()

        val stats = merge(db, snapshot(remote(syncId = "legacy", version = 1, originId = "")))

        assertEquals(1, stats.inserted)
        assertEquals("remote", db.exportSnapshot(SnapshotMode.Backup).transactions.single().originId)
    }

    @Test
    fun backupRestoreAdoptsRowsCreatedByTheBackupDevice() = runTest {
        val original = inMemoryDb()
        addSample(original)
        val backup = original.exportSnapshot(SnapshotMode.Backup)
        val originalDeviceId = original.deviceId()

        val restored = inMemoryDb()
        val restoredDeviceId = restored.deviceId()
        assertTrue(originalDeviceId != restoredDeviceId)

        val result = assertIs<SyncImportResult.Merged>(
            restored.importSnapshot(backup, SnapshotMode.Backup)
        )
        assertEquals(1, result.stats.inserted)

        val transaction = restored.getTransactions().executeAsList().single()
        assertEquals(restoredDeviceId, transaction.originId)
    }

    @Test
    fun backupRestoreKeepsPeerOwnedRowsReadOnly() = runTest {
        val original = inMemoryDb()
        addSample(original)
        merge(original, snapshot(remote(syncId = "remote-1", version = 1, originId = "remote")))
        val backup = original.exportSnapshot(SnapshotMode.Backup)

        val restored = inMemoryDb()
        val restoredDeviceId = restored.deviceId()
        restored.importSnapshot(backup, SnapshotMode.Backup)

        val rows = restored.getTransactions().executeAsList()
        assertEquals(2, rows.size)
        assertEquals(1, rows.count { it.originId == restoredDeviceId })
        assertEquals(1, rows.count { it.originId == "remote" })
    }
}