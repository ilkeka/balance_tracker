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

    private suspend fun merge(db: DB, snapshot: SyncSnapshot): MergeStats =
        assertIs<SyncImportResult.Merged>(db.importSnapshot(snapshot)).stats

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
        val id = addSample(db)
        val syncId = db.exportSnapshot().transactions.single().syncId

        merge(db, snapshot(remote(syncId = syncId, version = 99, originId = "remote", amount = 42.0)))

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

        assertIs<SyncImportResult.SameDevice>(db.importSnapshot(db.exportSnapshot()))
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
}