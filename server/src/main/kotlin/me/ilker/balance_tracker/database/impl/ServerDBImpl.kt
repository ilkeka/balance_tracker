package me.ilker.balance_tracker.database.impl

import app.cash.sqldelight.EnumColumnAdapter
import me.ilker.balance_tracker.ServerDatabase
import me.ilker.balance_tracker.Transactions
import me.ilker.balance_tracker.database.DatabaseDriverFactory
import me.ilker.balance_tracker.database.ServerDB
import kotlin.Throws

internal class ServerDBImpl(
    databaseDriverFactory: DatabaseDriverFactory
): ServerDB {
    private val database = ServerDatabase(
        driver = databaseDriverFactory.createDriver(),
        TransactionsAdapter = Transactions.Adapter(
            typeAdapter = EnumColumnAdapter(),
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

    override suspend fun getUserByMail(
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
