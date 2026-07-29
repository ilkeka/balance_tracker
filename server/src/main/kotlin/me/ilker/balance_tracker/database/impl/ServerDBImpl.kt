package me.ilker.balance_tracker.database.impl

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.ilker.balance_tracker.ServerDatabase
import me.ilker.balance_tracker.Transactions
import me.ilker.balance_tracker.database.ServerDB
import me.ilker.balance_tracker.sdk.TransactionCategory

internal class ServerDBImpl : ServerDB {
    private val driver = JdbcSqliteDriver(
        url = "jdbc:sqlite:Database.db",
        schema = ServerDatabase.Schema.synchronous()
    )

    private val database = ServerDatabase(
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
        ),
    )

    override suspend fun createUser(
        id: String,
        email: String,
        password: String,
        createdAt: String
    ) = database
        .userQueries
        .insertUser(
            id = id,
            email = email,
            password = password,
            createdAt = createdAt
        )

    override suspend fun getUser(
        id: String
    ) = database
        .userQueries
        .getUser(id = id)
        .executeAsOneOrNull()

    override suspend fun getUserByEmail(
        email: String
    ) = database
        .userQueries
        .getUserByEmail(email = email)
        .executeAsOneOrNull()

    @Throws(Exception::class)
    override suspend fun getUsers() = database
        .userQueries
        .getUsers()
        .executeAsList()
}
