package me.ilker.balance_tracker.sync

import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncedTransactionComparatorTest {

    private fun record(
        syncId: String = "sync",
        version: Long,
        originId: String,
        isDeleted: Boolean = false
    ) = SyncedTransaction(
        syncId = syncId,
        amount = 1.0,
        dateTime = "01/01/2026",
        type = TransactionType.Expense,
        category = TransactionCategory.Predefined.Other,
        description = null,
        version = version,
        originId = originId,
        isDeleted = isDeleted
    )

    @Test
    fun higherVersionWinsRegardlessOfOrigin() {
        val local = record(version = 7, originId = "zzzz")
        val remote = record(version = 8, originId = "aaaa")

        assertTrue(SyncedTransactionComparator.supersedes(candidate = remote, current = local))
    }

    @Test
    fun higherOriginBreaksVersionTie() {
        val local = record(version = 7, originId = "aaaa")
        val remote = record(version = 7, originId = "bbbb")

        assertTrue(SyncedTransactionComparator.supersedes(candidate = remote, current = local))
        assertFalse(SyncedTransactionComparator.supersedes(candidate = local, current = remote))
    }

    @Test
    fun identicalVersionAndOriginIsNotSuperseded() {
        val local = record(version = 7, originId = "aaaa")
        val remote = record(version = 7, originId = "aaaa")

        assertFalse(SyncedTransactionComparator.supersedes(candidate = remote, current = local))
    }

    @Test
    fun aDeleteBeatsAnOlderLiveRecord() {
        val local = record(version = 3, originId = "aaaa", isDeleted = false)
        val remote = record(version = 4, originId = "aaaa", isDeleted = true)

        assertTrue(SyncedTransactionComparator.supersedes(candidate = remote, current = local))
    }

    @Test
    fun anOlderDeleteLosesToANewerEdit() {
        val local = record(version = 9, originId = "aaaa", isDeleted = false)
        val remote = record(version = 4, originId = "bbbb", isDeleted = true)

        assertFalse(SyncedTransactionComparator.supersedes(candidate = remote, current = local))
    }

    /**
     * Convergence depends on the comparison being antisymmetric: for any two records exactly one of
     * A supersedes B, B supersedes A, or neither holds, and it is the same conclusion on both
     * devices. Without this the two devices can each keep its own copy and never agree.
     */
    @Test
    fun comparisonIsAntisymmetric() {
        val versions = listOf(1L, 2L, 3L)
        val origins = listOf("aaaa", "bbbb", "cccc")

        for (aVersion in versions) for (bVersion in versions) for (aOrigin in origins) for (bOrigin in origins) {
            val a = record(version = aVersion, originId = aOrigin)
            val b = record(version = bVersion, originId = bOrigin)

            val aSupersedesB = SyncedTransactionComparator.supersedes(candidate = a, current = b)
            val bSupersedesA = SyncedTransactionComparator.supersedes(candidate = b, current = a)

            assertFalse(
                aSupersedesB && bSupersedesA,
                "both won for a=($aVersion,$aOrigin) b=($bVersion,$bOrigin)"
            )
        }
    }

    @Test
    fun comparisonIsTransitive() {
        val versions = listOf(1L, 2L, 3L)
        val origins = listOf("aaaa", "bbbb", "cccc")
        val records = versions.flatMap { version ->
            origins.map { origin -> record(version = version, originId = origin) }
        }

        for (a in records) for (b in records) for (c in records) {
            if (SyncedTransactionComparator.supersedes(a, b) && SyncedTransactionComparator.supersedes(b, c)) {
                assertTrue(
                    SyncedTransactionComparator.supersedes(a, c),
                    "transitivity broken for ${a.version}/${a.originId} ${b.version}/${b.originId} ${c.version}/${c.originId}"
                )
            }
        }
    }
}