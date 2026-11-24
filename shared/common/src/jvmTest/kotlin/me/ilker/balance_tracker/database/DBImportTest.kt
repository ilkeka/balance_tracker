package me.ilker.balance_tracker.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import me.ilker.balance_tracker.Database
import me.ilker.balance_tracker.sdk.NewTransaction
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals

class DBImportTest {

    private fun inMemoryDb(): DB = DB(
        JdbcSqliteDriver(
            url = JdbcSqliteDriver.IN_MEMORY,
            schema = Database.Schema.synchronous()
        )
    )

    private fun row(
        amount: Double = 5.0,
        dateTime: String = "01/01/2026",
        type: TransactionType = TransactionType.Expense,
        category: TransactionCategory = TransactionCategory.Predefined.Other,
        description: String? = null
    ) = NewTransaction(
        amount = amount,
        dateTime = dateTime,
        type = type,
        category = category,
        description = description
    )

    @Test
    fun importedRowsBecomeVisibleTransactions() = runTest {
        val db = inMemoryDb()

        val result = db.importTransactions(listOf(row(), row(amount = 12.5)))

        assertEquals(2, result.imported)
        assertEquals(0, result.skipped)
        assertEquals(2, db.getTransactions().executeAsList().size)
    }

    @Test
    fun importingTheSameRowTwiceSkipsTheSecondCopy() = runTest {
        val db = inMemoryDb()

        db.importTransactions(listOf(row(description = "Coffee")))
        val second = db.importTransactions(listOf(row(description = "Coffee")))

        assertEquals(0, second.imported)
        assertEquals(1, second.skipped)
        assertEquals(1, db.getTransactions().executeAsList().size)
    }

    @Test
    fun aRowThatOnlyDiffersByDescriptionIsImported() = runTest {
        val db = inMemoryDb()

        db.importTransactions(listOf(row(description = "Coffee")))
        val second = db.importTransactions(listOf(row(description = "Lunch")))

        assertEquals(1, second.imported)
        assertEquals(2, db.getTransactions().executeAsList().size)
    }

    @Test
    fun nullAndEmptyDescriptionsAreTheSameContent() = runTest {
        val db = inMemoryDb()

        db.importTransactions(listOf(row(description = null)))
        val second = db.importTransactions(listOf(row(description = "")))

        assertEquals(0, second.imported)
        assertEquals(1, second.skipped)
        assertEquals(1, db.getTransactions().executeAsList().size)
    }

    /**
     * The regression this guards: transactions created by the add screen store an empty description,
     * while a CSV row without one parses to null. Treating those as different duplicated every row
     * the user had entered manually as soon as they imported their own export back.
     */
    @Test
    fun aTransactionAddedWithTheUsersEmptyDescriptionIsSkippedOnImport() = runTest {
        val db = inMemoryDb()
        db.addTransaction(
            amount = 5.0,
            dateTime = "01/01/2026",
            type = TransactionType.Expense,
            category = TransactionCategory.Predefined.Other,
            description = ""
        )

        val result = db.importTransactions(listOf(row(description = null)))

        assertEquals(0, result.imported)
        assertEquals(1, result.skipped)
        assertEquals(1, db.getTransactions().executeAsList().size)
    }

    @Test
    fun surroundingWhitespaceDoesNotHideADuplicate() = runTest {
        val db = inMemoryDb()
        db.addTransaction(
            amount = 5.0,
            dateTime = "01/01/2026",
            type = TransactionType.Expense,
            category = TransactionCategory.Predefined.Other,
            description = "  Coffee  "
        )

        val result = db.importTransactions(listOf(row(description = "Coffee")))

        assertEquals(0, result.imported)
        assertEquals(1, result.skipped)
    }
}
