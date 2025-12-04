package me.ilker.balance_tracker.database

import app.cash.sqldelight.EnumColumnAdapter
import me.ilker.balance_tracker.Database
import me.ilker.balancetracker.Transactions
import me.ilker.transaction.transactions.TransactionDomainModel
import me.ilker.transaction.transactions.TransactionType

internal class DB(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = Database(
        driver = databaseDriverFactory.createDriver(),
        TransactionsAdapter = Transactions.Adapter(
            typeAdapter =  EnumColumnAdapter(),
        ),
    )
    private val dbQuery = database.databaseQueries

    internal fun getTransactions() = dbQuery
        .getTransactions { id, amount, dateTime, type ->
            TransactionDomainModel(
                id = id,
                amount = amount,
                dateTime = dateTime,
                type = type
            )
        }

    internal fun addTransaction(
        amount: Double,
        dateTime: String,
        type: TransactionType
    ) = dbQuery.insertTransaction(
        amount = amount,
        dateTime = dateTime,
        type = type
    )
}
