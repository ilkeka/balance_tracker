package me.ilker.balance_tracker.database

import me.ilker.balance_tracker.Database
import me.ilker.transaction.transactions.TransactionDomainModel

internal class DB(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = Database(databaseDriverFactory.createDriver())
    private val dbQuery = database.databaseQueries

    internal fun getTransactions() = dbQuery
        .getTransactions { id, amount, dateTime ->
            TransactionDomainModel(
                id = id,
                amount = amount,
                dateTime = dateTime
            )
        }

    internal fun addTransaction(
        amount: Double,
        dateTime: String
    ) = dbQuery.insertTransaction(
        amount = amount,
        dateTime = dateTime
    )
}
