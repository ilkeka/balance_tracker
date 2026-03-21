package me.ilker.balance_tracker.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import me.ilker.balance_tracker.Database
import me.ilker.balance_tracker.Transactions
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.balance_tracker.sdk.TransactionType

internal class DB(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = Database(
        driver = databaseDriverFactory.createDriver(),
        TransactionsAdapter = Transactions.Adapter(
            typeAdapter =  EnumColumnAdapter(),
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
    private val dbQuery = database.transactionQueries

    internal fun getTransactionById(id: Long) = dbQuery
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

    internal fun getTransactions() = dbQuery
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
    ) = dbQuery.insertTransaction(
        amount = amount,
        dateTime = dateTime,
        type = type,
        category = category,
        description = description
    )

    internal suspend fun deleteTransaction(
        id: Long
    ) = dbQuery.deleteTransaction(
        id = id
    )
}
